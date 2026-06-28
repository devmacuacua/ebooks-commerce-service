package mz.ebooks.commerce.order.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mz.ebooks.commerce.address.dto.AddressDto;
import mz.ebooks.commerce.address.entity.Address;
import mz.ebooks.commerce.address.repository.AddressRepository;
import mz.ebooks.commerce.messaging.CommerceEventPublisher;
import mz.ebooks.commerce.order.dto.CheckoutItemRequest;
import mz.ebooks.commerce.order.dto.CheckoutResponse;
import mz.ebooks.commerce.order.dto.CreateOrderRequest;
import mz.ebooks.commerce.order.dto.OrderDto;
import mz.ebooks.commerce.order.dto.OrderItemDto;
import mz.ebooks.commerce.order.dto.OrderSummaryDto;
import mz.ebooks.commerce.order.entity.Order;
import mz.ebooks.commerce.order.entity.OrderItem;
import mz.ebooks.commerce.order.repository.OrderRepository;
import mz.ebooks.commerce.payment.dto.InitiatePaymentRequest;
import mz.ebooks.commerce.payment.dto.PaymentResponse;
import mz.ebooks.commerce.payment.repository.PaymentRepository;
import mz.ebooks.commerce.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final AddressRepository addressRepository;
    private final CommerceEventPublisher eventPublisher;
    private final PaymentService paymentService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Transactional
    public CheckoutResponse checkout(String userId, CreateOrderRequest req) {
        List<CheckoutItemRequest> reqItems = req.getItems();

        if (reqItems == null || reqItems.isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }

        String currency = req.getCurrency() != null ? req.getCurrency() : "MZN";

        BigDecimal subtotal = reqItems.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean hasPhysical = reqItems.stream()
                .anyMatch(item -> "PHYSICAL".equalsIgnoreCase(item.getBookType())
                        || "BOTH".equalsIgnoreCase(item.getBookType()));

        BigDecimal deliveryFee = hasPhysical ? new BigDecimal("150.00") : BigDecimal.ZERO;
        BigDecimal total = subtotal.add(deliveryFee);

        String orderNumber = generateOrderNumber();

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .userId(userId)
                .addressId(req.getAddressId())
                .status("PENDING")
                .subtotal(subtotal)
                .deliveryFee(deliveryFee)
                .total(total)
                .currency(currency)
                .notes(req.getNotes())
                .build();

        List<OrderItem> orderItems = reqItems.stream()
                .map(item -> OrderItem.builder()
                        .bookId(item.getBookId())
                        .bookTitle(item.getBookTitle())
                        .bookType(item.getBookType())
                        .bookCover(item.getBookCover())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getPrice())
                        .totalPrice(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                        .build())
                .toList();

        order = orderRepository.save(order);
        order.getItems().addAll(orderItems);
        order = orderRepository.save(order);

        Map<String, Object> createdEvent = new HashMap<>();
        createdEvent.put("orderId", order.getId().toString());
        createdEvent.put("orderNumber", order.getOrderNumber());
        createdEvent.put("userId", userId);
        createdEvent.put("total", order.getTotal());
        createdEvent.put("currency", order.getCurrency());
        eventPublisher.publishOrderCreated(createdEvent);

        log.info("Order {} created for user {}", orderNumber, userId);

        InitiatePaymentRequest paymentReq = InitiatePaymentRequest.builder()
                .userId(userId)
                .orderId(order.getId())
                .method(req.getPaymentMethod())
                .amount(total)
                .currency(currency)
                .phoneNumber(req.getPhoneNumber())
                .returnUrl(frontendUrl + "/checkout/paypal/capture")
                .cancelUrl(frontendUrl + "/checkout?paypal=cancelled")
                .build();

        PaymentResponse paymentResponse = paymentService.initiatePayment(paymentReq);

        return CheckoutResponse.builder()
                .orderId(order.getId())
                .paymentId(paymentResponse.getPaymentId())
                .status(paymentResponse.getStatus())
                .clientSecret(paymentResponse.getClientSecret())
                .redirectUrl(paymentResponse.getRedirectUrl())
                .instructions(paymentResponse.getInstructions())
                .method(paymentResponse.getMethod())
                .build();
    }

    public Page<OrderDto> getUserOrders(String userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable)
                .map(order -> toDto(order, null));
    }

    public Page<OrderSummaryDto> getAdminOrders(String status, Pageable pageable) {
        Page<Order> page = (status != null && !status.isBlank())
                ? orderRepository.findByStatus(status, pageable)
                : orderRepository.findAll(pageable);
        return page.map(this::toSummaryDto);
    }

    public OrderDto getOrder(UUID orderId, String userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        String paymentStatus = paymentRepository.findByOrderId(orderId).stream()
                .filter(p -> "COMPLETED".equals(p.getStatus()))
                .findFirst()
                .map(p -> p.getStatus())
                .orElse(null);
        return toDto(order, paymentStatus);
    }

    @Transactional
    public OrderDto updateStatus(UUID orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        String previousStatus = order.getStatus();
        order.setStatus(status);
        order = orderRepository.save(order);

        Map<String, Object> event = new HashMap<>();
        event.put("orderId", orderId.toString());
        event.put("orderNumber", order.getOrderNumber());
        event.put("userId", order.getUserId());
        event.put("previousStatus", previousStatus);
        event.put("newStatus", status);
        eventPublisher.publishOrderStatusChanged(event);

        return toDto(order, null);
    }

    @Transactional
    public OrderDto cancelOrder(UUID orderId, String userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!Set.of("PENDING", "PAID").contains(order.getStatus())) {
            throw new IllegalStateException("Order cannot be cancelled in status: " + order.getStatus());
        }

        order.setStatus("CANCELLED");
        order = orderRepository.save(order);

        List<Map<String, Object>> cancelItems = order.getItems().stream()
                .map(item -> {
                    Map<String, Object> i = new HashMap<>();
                    i.put("bookId", item.getBookId().toString());
                    i.put("bookTitle", item.getBookTitle());
                    i.put("bookType", item.getBookType());
                    i.put("quantity", item.getQuantity());
                    return i;
                })
                .collect(java.util.stream.Collectors.toList());

        Map<String, Object> event = new HashMap<>();
        event.put("orderId", orderId.toString());
        event.put("orderNumber", order.getOrderNumber());
        event.put("userId", userId);
        event.put("items", cancelItems);
        eventPublisher.publishOrderCancelled(event);

        log.info("Order {} cancelled by user {}", order.getOrderNumber(), userId);
        return toDto(order, null);
    }

    private String generateOrderNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int random = new Random().nextInt(9000) + 1000;
        return "EBS" + datePart + random;
    }

    private OrderDto toDto(Order order, String paymentStatus) {
        List<OrderItemDto> itemDtos = order.getItems().stream()
                .map(item -> OrderItemDto.builder()
                        .id(item.getId())
                        .bookId(item.getBookId())
                        .bookTitle(item.getBookTitle())
                        .bookType(item.getBookType())
                        .bookCover(item.getBookCover())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .totalPrice(item.getTotalPrice())
                        .build())
                .toList();

        AddressDto addressDto = null;
        if (order.getAddressId() != null) {
            addressDto = addressRepository.findById(order.getAddressId())
                    .map(this::toAddressDto)
                    .orElse(null);
        }

        return OrderDto.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .addressId(order.getAddressId())
                .address(addressDto)
                .status(order.getStatus())
                .subtotal(order.getSubtotal())
                .deliveryFee(order.getDeliveryFee())
                .total(order.getTotal())
                .currency(order.getCurrency())
                .notes(order.getNotes())
                .items(itemDtos)
                .paymentStatus(paymentStatus)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private AddressDto toAddressDto(Address a) {
        return AddressDto.builder()
                .id(a.getId())
                .userId(a.getUserId())
                .name(a.getName())
                .street(a.getStreet())
                .number(a.getNumber())
                .complement(a.getComplement())
                .district(a.getDistrict())
                .city(a.getCity())
                .province(a.getProvince())
                .country(a.getCountry())
                .postalCode(a.getPostalCode())
                .isDefault(a.isDefault())
                .createdAt(a.getCreatedAt())
                .build();
    }

    private OrderSummaryDto toSummaryDto(Order order) {
        return OrderSummaryDto.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .total(order.getTotal())
                .currency(order.getCurrency())
                .itemCount(order.getItems().size())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
