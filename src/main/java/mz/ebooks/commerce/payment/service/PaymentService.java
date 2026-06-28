package mz.ebooks.commerce.payment.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mz.ebooks.commerce.address.repository.AddressRepository;
import mz.ebooks.commerce.messaging.CommerceEventPublisher;
import mz.ebooks.commerce.order.entity.Order;
import mz.ebooks.commerce.order.repository.OrderRepository;
import mz.ebooks.commerce.payment.dto.InitiatePaymentRequest;
import mz.ebooks.commerce.payment.dto.PaymentDto;
import mz.ebooks.commerce.payment.dto.PaymentResponse;
import mz.ebooks.commerce.payment.entity.Payment;
import mz.ebooks.commerce.payment.provider.EmolaProvider;
import mz.ebooks.commerce.payment.provider.MpesaProvider;
import mz.ebooks.commerce.payment.provider.PaypalProvider;
import mz.ebooks.commerce.payment.provider.StripeProvider;
import mz.ebooks.commerce.payment.repository.PaymentRepository;
import mz.ebooks.commerce.subscription.service.SubscriptionService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final AddressRepository addressRepository;
    private final MpesaProvider mpesaProvider;
    private final EmolaProvider emolaProvider;
    private final StripeProvider stripeProvider;
    private final PaypalProvider paypalProvider;
    private final CommerceEventPublisher eventPublisher;
    private final SubscriptionService subscriptionService;

    @Transactional
    public PaymentResponse initiatePayment(InitiatePaymentRequest req) {
        String currency = req.getCurrency() != null ? req.getCurrency() : "MZN";

        Payment payment = Payment.builder()
                .orderId(req.getOrderId())
                .subscriptionId(req.getSubscriptionId())
                .userId(req.getUserId())
                .method(req.getMethod())
                .status("PENDING")
                .amount(req.getAmount())
                .currency(currency)
                .phoneNumber(req.getPhoneNumber())
                .build();

        payment = paymentRepository.save(payment);

        return switch (req.getMethod().toUpperCase()) {
            case "MPESA" -> handleMpesa(payment, req);
            case "EMOLA" -> handleEmola(payment, req);
            case "VISA", "MASTERCARD" -> handleStripe(payment, req, currency);
            case "PAYPAL" -> handlePaypal(payment, req);
            default -> throw new IllegalArgumentException("Unsupported payment method: " + req.getMethod());
        };
    }

    private PaymentResponse handleMpesa(Payment payment, InitiatePaymentRequest req) {
        MpesaProvider.MpesaPaymentResult result = mpesaProvider.initiateC2B(
                req.getPhoneNumber(), req.getAmount(), payment.getId().toString());

        payment.setMpesaConversationId(result.conversationId());
        payment.setMpesaThirdPartyRef(payment.getId().toString());
        if (!result.success()) {
            payment.setStatus("FAILED");
            payment.setFailureReason(result.responseDesc());
        }
        paymentRepository.save(payment);

        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .status(payment.getStatus())
                .method("MPESA")
                .instructions("Digite o PIN M-Pesa no seu telemóvel para confirmar o pagamento de " +
                        req.getAmount() + " MZN")
                .build();
    }

    private PaymentResponse handleEmola(Payment payment, InitiatePaymentRequest req) {
        EmolaProvider.EmolaPaymentResult result = emolaProvider.initiatePayment(
                req.getPhoneNumber(), req.getAmount(), payment.getId().toString());

        payment.setEmolaReference(result.reference());
        if (!result.success()) {
            payment.setStatus("FAILED");
            payment.setFailureReason(result.errorMessage());
        }
        paymentRepository.save(payment);

        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .status(payment.getStatus())
                .method("EMOLA")
                .instructions("Confirme o pagamento de " + req.getAmount() + " MZN no E-mola")
                .build();
    }

    private PaymentResponse handleStripe(Payment payment, InitiatePaymentRequest req, String currency) {
        Map<String, String> metadata = new HashMap<>();
        if (req.getOrderId() != null) metadata.put("orderId", req.getOrderId().toString());
        if (req.getSubscriptionId() != null) metadata.put("subscriptionId", req.getSubscriptionId().toString());
        metadata.put("userId", req.getUserId());

        StripeProvider.PaymentIntentResult result = stripeProvider.createPaymentIntent(
                req.getAmount(), currency, metadata);

        if (result.success()) {
            payment.setStripePaymentIntentId(result.paymentIntentId());
            payment.setExternalId(result.paymentIntentId());
        } else {
            payment.setStatus("FAILED");
            payment.setFailureReason(result.errorMessage());
        }
        paymentRepository.save(payment);

        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .status(payment.getStatus())
                .method(req.getMethod())
                .clientSecret(result.clientSecret())
                .build();
    }

    private PaymentResponse handlePaypal(Payment payment, InitiatePaymentRequest req) {
        PaypalProvider.PaypalOrderResult result = paypalProvider.createOrder(
                req.getAmount(), "USD", req.getReturnUrl(), req.getCancelUrl());

        if (result.success()) {
            payment.setPaypalOrderId(result.orderId());
            payment.setExternalId(result.orderId());
        } else {
            payment.setStatus("FAILED");
            payment.setFailureReason(result.errorMessage());
        }
        paymentRepository.save(payment);

        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .status(payment.getStatus())
                .method("PAYPAL")
                .redirectUrl(result.approvalUrl())
                .build();
    }

    @Transactional
    public void handleStripeWebhook(String payload, String sig) {
        com.stripe.model.Event event = stripeProvider.handleWebhookEvent(payload, sig);
        log.info("Stripe webhook event: {}", event.getType());

        if ("payment_intent.succeeded".equals(event.getType())) {
            com.stripe.model.StripeObject stripeObject = event.getDataObjectDeserializer()
                    .getObject().orElse(null);
            if (stripeObject instanceof com.stripe.model.PaymentIntent paymentIntent) {
                paymentRepository.findByStripePaymentIntentId(paymentIntent.getId())
                        .ifPresent(this::completePayment);
            }
        } else if ("payment_intent.payment_failed".equals(event.getType())) {
            com.stripe.model.StripeObject stripeObject = event.getDataObjectDeserializer()
                    .getObject().orElse(null);
            if (stripeObject instanceof com.stripe.model.PaymentIntent paymentIntent) {
                paymentRepository.findByStripePaymentIntentId(paymentIntent.getId())
                        .ifPresent(p -> failPayment(p, paymentIntent.getLastPaymentError() != null
                                ? paymentIntent.getLastPaymentError().getMessage() : "Payment failed"));
            }
        }
    }

    @Transactional
    public void handleMpesaCallback(Map<String, Object> body) {
        log.info("M-Pesa callback received: {}", body);
        try {
            Object resultCodeObj = body.get("ResultCode");
            int resultCode = resultCodeObj instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(resultCodeObj));
            String conversationId = (String) body.get("ConversationID");

            if (conversationId == null) {
                log.warn("M-Pesa callback missing ConversationID");
                return;
            }

            paymentRepository.findByMpesaConversationId(conversationId).ifPresent(payment -> {
                if (resultCode == 0) {
                    completePayment(payment);
                } else {
                    String reason = (String) body.getOrDefault("ResultDesc", "M-Pesa payment failed");
                    failPayment(payment, reason);
                }
            });
        } catch (Exception e) {
            log.error("Error processing M-Pesa callback: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public void handleEmolaCallback(Map<String, Object> body) {
        log.info("E-mola callback received: {}", body);
        try {
            String reference = (String) body.get("reference");
            String status = (String) body.get("status");

            if (reference == null) {
                log.warn("E-mola callback missing reference");
                return;
            }

            paymentRepository.findByEmolaReference(reference).ifPresent(payment -> {
                if ("COMPLETED".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status)) {
                    completePayment(payment);
                } else {
                    String reason = (String) body.getOrDefault("message", "E-mola payment failed");
                    failPayment(payment, reason);
                }
            });
        } catch (Exception e) {
            log.error("Error processing E-mola callback: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public PaymentResponse handlePaypalCapture(String paypalOrderId) {
        PaypalProvider.PaypalCaptureResult result = paypalProvider.captureOrder(paypalOrderId);

        Payment payment = paymentRepository.findByPaypalOrderId(paypalOrderId)
                .orElseThrow(() -> new RuntimeException("Payment not found for PayPal order: " + paypalOrderId));

        if (result.success()) {
            completePayment(payment);
        } else {
            failPayment(payment, result.errorMessage());
        }

        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .status(payment.getStatus())
                .method("PAYPAL")
                .build();
    }

    @Transactional
    public void completePayment(Payment payment) {
        payment.setStatus("COMPLETED");
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // Update order if linked — then publish commerce.order.paid with full payload
        if (payment.getOrderId() != null) {
            orderRepository.findById(payment.getOrderId()).ifPresent(order -> {
                order.setStatus("PAID");
                orderRepository.save(order);
                publishOrderPaidEvent(order, payment);
            });
        }

        // Activate subscription if linked
        if (payment.getSubscriptionId() != null) {
            subscriptionService.activateSubscription(payment.getSubscriptionId(), payment.getId());
        }

        Map<String, Object> event = new HashMap<>();
        event.put("paymentId", payment.getId().toString());
        event.put("userId", payment.getUserId());
        event.put("amount", payment.getAmount());
        event.put("currency", payment.getCurrency());
        event.put("method", payment.getMethod());
        if (payment.getOrderId() != null) event.put("orderId", payment.getOrderId().toString());
        if (payment.getSubscriptionId() != null) event.put("subscriptionId", payment.getSubscriptionId().toString());
        eventPublisher.publishPaymentCompleted(event);

        log.info("Payment {} completed for user {}", payment.getId(), payment.getUserId());
    }

    private void publishOrderPaidEvent(Order order, Payment payment) {
        Map<String, Object> deliveryAddress = null;
        if (order.getAddressId() != null) {
            deliveryAddress = addressRepository.findById(order.getAddressId())
                    .map(addr -> {
                        Map<String, Object> a = new HashMap<>();
                        a.put("recipientName", addr.getName());
                        String address = addr.getStreet() + (addr.getNumber() != null ? ", " + addr.getNumber() : "");
                        a.put("address", address);
                        a.put("district", addr.getDistrict());
                        a.put("city", addr.getCity());
                        a.put("province", addr.getProvince());
                        a.put("country", addr.getCountry());
                        a.put("postalCode", addr.getPostalCode());
                        return a;
                    }).orElse(null);
        }

        List<Map<String, Object>> items = order.getItems().stream()
                .map(item -> {
                    Map<String, Object> i = new HashMap<>();
                    i.put("orderItemId", item.getId().toString());
                    i.put("bookId", item.getBookId().toString());
                    i.put("bookTitle", item.getBookTitle());
                    i.put("type", item.getBookType());
                    i.put("bookType", item.getBookType());
                    i.put("bookCover", item.getBookCover());
                    i.put("quantity", item.getQuantity());
                    i.put("price", item.getUnitPrice());
                    i.put("unitPrice", item.getUnitPrice());
                    i.put("totalPrice", item.getTotalPrice());
                    i.put("currency", order.getCurrency());
                    return i;
                }).collect(java.util.stream.Collectors.toList());

        Map<String, Object> event = new HashMap<>();
        event.put("orderId", order.getId().toString());
        event.put("orderNumber", order.getOrderNumber());
        event.put("userId", order.getUserId());
        event.put("total", order.getTotal());
        event.put("subtotal", order.getSubtotal());
        event.put("deliveryFee", order.getDeliveryFee());
        event.put("currency", order.getCurrency());
        event.put("paymentMethod", payment.getMethod());
        event.put("paymentId", payment.getId().toString());
        event.put("items", items);
        if (deliveryAddress != null) event.put("deliveryAddress", deliveryAddress);
        eventPublisher.publishOrderPaid(event);
    }

    @Transactional
    public void failPayment(Payment payment, String reason) {
        payment.setStatus("FAILED");
        payment.setFailureReason(reason);
        paymentRepository.save(payment);

        Map<String, Object> event = new HashMap<>();
        event.put("paymentId", payment.getId().toString());
        event.put("userId", payment.getUserId());
        event.put("reason", reason);
        if (payment.getOrderId() != null) event.put("orderId", payment.getOrderId().toString());
        eventPublisher.publishPaymentFailed(event);

        log.warn("Payment {} failed for user {}: {}", payment.getId(), payment.getUserId(), reason);
    }

    @Transactional
    public PaymentDto refundPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));

        if (!"COMPLETED".equals(payment.getStatus())) {
            throw new IllegalStateException("Only completed payments can be refunded");
        }

        // Route to correct provider
        switch (payment.getMethod().toUpperCase()) {
            case "VISA", "MASTERCARD" -> {
                if (payment.getStripePaymentIntentId() != null) {
                    StripeProvider.RefundResult result = stripeProvider.refund(
                            payment.getStripePaymentIntentId(), null);
                    if (!result.success()) {
                        throw new RuntimeException("Stripe refund failed: " + result.errorMessage());
                    }
                }
            }
            default -> log.warn("Refund not automatically processed for method: {}", payment.getMethod());
        }

        payment.setStatus("REFUNDED");
        payment = paymentRepository.save(payment);

        Map<String, Object> event = new HashMap<>();
        event.put("paymentId", paymentId.toString());
        event.put("userId", payment.getUserId());
        event.put("amount", payment.getAmount());
        if (payment.getOrderId() != null) event.put("orderId", payment.getOrderId().toString());
        eventPublisher.publishPaymentRefunded(event);

        log.info("Payment {} refunded", paymentId);
        return toDto(payment);
    }

    public PaymentDto getPayment(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .map(this::toDto)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
    }

    private PaymentDto toDto(Payment p) {
        return PaymentDto.builder()
                .id(p.getId())
                .orderId(p.getOrderId())
                .subscriptionId(p.getSubscriptionId())
                .userId(p.getUserId())
                .method(p.getMethod())
                .status(p.getStatus())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .phoneNumber(p.getPhoneNumber())
                .failureReason(p.getFailureReason())
                .paidAt(p.getPaidAt())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
