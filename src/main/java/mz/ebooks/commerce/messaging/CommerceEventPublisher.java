package mz.ebooks.commerce.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mz.ebooks.commerce.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommerceEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishOrderPaid(Map<String, Object> payload) {
        publish(RabbitMQConfig.RK_ORDER_PAID, payload);
    }

    public void publishOrderCreated(Map<String, Object> payload) {
        publish(RabbitMQConfig.RK_ORDER_CREATED, payload);
    }

    public void publishOrderStatusChanged(Map<String, Object> payload) {
        publish(RabbitMQConfig.RK_ORDER_STATUS_CHANGED, payload);
    }

    public void publishOrderCancelled(Map<String, Object> payload) {
        publish(RabbitMQConfig.RK_ORDER_CANCELLED, payload);
    }

    public void publishPaymentCompleted(Map<String, Object> payload) {
        publish(RabbitMQConfig.RK_PAYMENT_COMPLETED, payload);
    }

    public void publishPaymentFailed(Map<String, Object> payload) {
        publish(RabbitMQConfig.RK_PAYMENT_FAILED, payload);
    }

    public void publishPaymentRefunded(Map<String, Object> payload) {
        publish(RabbitMQConfig.RK_PAYMENT_REFUNDED, payload);
    }

    public void publishSubscriptionExpiring(Map<String, Object> payload) {
        publish(RabbitMQConfig.RK_SUBSCRIPTION_EXPIRING, payload);
    }

    public void publishSubscriptionExpired(Map<String, Object> payload) {
        publish(RabbitMQConfig.RK_SUBSCRIPTION_EXPIRED, payload);
    }

    public void publishSubscriptionRenewalRequested(Map<String, Object> payload) {
        publish(RabbitMQConfig.RK_SUBSCRIPTION_RENEWAL_REQUESTED, payload);
    }

    private void publish(String routingKey, Object payload) {
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, routingKey, payload);
            log.debug("Published event with routing key '{}': {}", routingKey, payload);
        } catch (Exception e) {
            log.error("Failed to publish event with routing key '{}': {}", routingKey, e.getMessage(), e);
        }
    }
}
