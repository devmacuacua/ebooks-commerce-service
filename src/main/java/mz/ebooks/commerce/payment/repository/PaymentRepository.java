package mz.ebooks.commerce.payment.repository;

import mz.ebooks.commerce.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByOrderId(UUID orderId);

    List<Payment> findBySubscriptionId(UUID subscriptionId);

    List<Payment> findByUserId(String userId);

    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);

    Optional<Payment> findByPaypalOrderId(String paypalOrderId);

    Optional<Payment> findByMpesaConversationId(String mpesaConversationId);

    Optional<Payment> findByEmolaReference(String emolaReference);
}
