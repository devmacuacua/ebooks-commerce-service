package mz.ebooks.commerce.payment.provider;

import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Service
@Slf4j
public class StripeProvider {

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    public PaymentIntentResult createPaymentIntent(BigDecimal amount, String currency,
                                                    Map<String, String> metadata) {
        try {
            long amountInCents = amount.multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValue();

            PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(currency.toLowerCase())
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    );

            if (metadata != null && !metadata.isEmpty()) {
                paramsBuilder.putAllMetadata(metadata);
            }

            PaymentIntent paymentIntent = PaymentIntent.create(paramsBuilder.build());
            return new PaymentIntentResult(true, paymentIntent.getId(),
                    paymentIntent.getClientSecret(), null);
        } catch (StripeException e) {
            log.error("Failed to create Stripe PaymentIntent: {}", e.getMessage(), e);
            return new PaymentIntentResult(false, null, null, e.getMessage());
        }
    }

    public Event handleWebhookEvent(String payload, String sigHeader) {
        try {
            return Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.error("Invalid Stripe webhook signature: {}", e.getMessage());
            throw new RuntimeException("Invalid webhook signature", e);
        } catch (Exception e) {
            log.error("Failed to parse Stripe webhook event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse webhook event", e);
        }
    }

    public RefundResult refund(String paymentIntentId, BigDecimal amount) {
        try {
            RefundCreateParams.Builder paramsBuilder = RefundCreateParams.builder()
                    .setPaymentIntent(paymentIntentId);

            if (amount != null) {
                long amountInCents = amount.multiply(BigDecimal.valueOf(100))
                        .setScale(0, RoundingMode.HALF_UP)
                        .longValue();
                paramsBuilder.setAmount(amountInCents);
            }

            Refund refund = Refund.create(paramsBuilder.build());
            return new RefundResult(true, refund.getId(), refund.getStatus(), null);
        } catch (StripeException e) {
            log.error("Failed to refund Stripe payment {}: {}", paymentIntentId, e.getMessage(), e);
            return new RefundResult(false, null, "failed", e.getMessage());
        }
    }

    public record PaymentIntentResult(boolean success, String paymentIntentId, String clientSecret,
                                      String errorMessage) {}

    public record RefundResult(boolean success, String refundId, String status, String errorMessage) {}
}
