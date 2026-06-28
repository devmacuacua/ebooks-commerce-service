package mz.ebooks.commerce.payment;

import mz.ebooks.commerce.address.repository.AddressRepository;
import mz.ebooks.commerce.messaging.CommerceEventPublisher;
import mz.ebooks.commerce.order.entity.Order;
import mz.ebooks.commerce.order.entity.OrderItem;
import mz.ebooks.commerce.order.repository.OrderRepository;
import mz.ebooks.commerce.payment.entity.Payment;
import mz.ebooks.commerce.payment.provider.EmolaProvider;
import mz.ebooks.commerce.payment.provider.MpesaProvider;
import mz.ebooks.commerce.payment.provider.PaypalProvider;
import mz.ebooks.commerce.payment.provider.StripeProvider;
import mz.ebooks.commerce.payment.repository.PaymentRepository;
import mz.ebooks.commerce.payment.service.PaymentService;
import mz.ebooks.commerce.subscription.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock PaymentRepository paymentRepository;
    @Mock OrderRepository orderRepository;
    @Mock AddressRepository addressRepository;
    @Mock MpesaProvider mpesaProvider;
    @Mock EmolaProvider emolaProvider;
    @Mock StripeProvider stripeProvider;
    @Mock PaypalProvider paypalProvider;
    @Mock CommerceEventPublisher eventPublisher;
    @Mock SubscriptionService subscriptionService;

    @InjectMocks PaymentService paymentService;

    private Payment samplePayment;
    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        OrderItem item = OrderItem.builder()
                .id(UUID.randomUUID())
                .bookId(UUID.randomUUID())
                .bookTitle("Clean Code")
                .bookType("EBOOK")
                .quantity(1)
                .unitPrice(new BigDecimal("250.00"))
                .totalPrice(new BigDecimal("250.00"))
                .build();

        sampleOrder = Order.builder()
                .id(orderId)
                .orderNumber("EB-001")
                .userId("user-abc")
                .status("PENDING")
                .subtotal(new BigDecimal("250.00"))
                .deliveryFee(BigDecimal.ZERO)
                .total(new BigDecimal("250.00"))
                .currency("MZN")
                .items(List.of(item))
                .build();

        samplePayment = Payment.builder()
                .id(paymentId)
                .orderId(orderId)
                .userId("user-abc")
                .method("MPESA")
                .status("PENDING")
                .amount(new BigDecimal("250.00"))
                .currency("MZN")
                .build();
    }

    @Test
    void completePayment_setsStatusAndUpdatesOrder() {
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.findById(sampleOrder.getId())).thenReturn(Optional.of(sampleOrder));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(addressRepository.findById(any())).thenReturn(Optional.empty());

        paymentService.completePayment(samplePayment);

        assertThat(samplePayment.getStatus()).isEqualTo("COMPLETED");
        assertThat(samplePayment.getPaidAt()).isNotNull();
        assertThat(sampleOrder.getStatus()).isEqualTo("PAID");

        verify(eventPublisher).publishPaymentCompleted(any());
        verify(eventPublisher).publishOrderPaid(any());
    }

    @Test
    void completePayment_publishesOrderPaidWithItems() {
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.findById(sampleOrder.getId())).thenReturn(Optional.of(sampleOrder));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(addressRepository.findById(any())).thenReturn(Optional.empty());

        paymentService.completePayment(samplePayment);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(eventPublisher).publishOrderPaid(captor.capture());

        Map<String, Object> payload = captor.getValue();
        assertThat(payload).containsKey("orderId");
        assertThat(payload).containsKey("items");
        assertThat(payload.get("paymentMethod")).isEqualTo("MPESA");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");
        assertThat(items).hasSize(1);
        assertThat(items.get(0).get("bookTitle")).isEqualTo("Clean Code");
    }

    @Test
    void failPayment_setsStatusAndPublishesEvent() {
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        paymentService.failPayment(samplePayment, "Insufficient funds");

        assertThat(samplePayment.getStatus()).isEqualTo("FAILED");
        assertThat(samplePayment.getFailureReason()).isEqualTo("Insufficient funds");
        verify(eventPublisher).publishPaymentFailed(any());
    }

    @Test
    void refundPayment_throwsWhenNotCompleted() {
        samplePayment.setStatus("PENDING");
        when(paymentRepository.findById(samplePayment.getId())).thenReturn(Optional.of(samplePayment));

        assertThatThrownBy(() -> paymentService.refundPayment(samplePayment.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("completed");
    }

    @Test
    void refundPayment_updatesStatusAndPublishesEvent() {
        samplePayment.setStatus("COMPLETED");
        when(paymentRepository.findById(samplePayment.getId())).thenReturn(Optional.of(samplePayment));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        paymentService.refundPayment(samplePayment.getId());

        assertThat(samplePayment.getStatus()).isEqualTo("REFUNDED");
        verify(eventPublisher).publishPaymentRefunded(any());
    }
}
