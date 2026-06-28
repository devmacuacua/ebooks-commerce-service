package mz.ebooks.commerce.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "ebooks.events";

    // Queue names
    public static final String QUEUE_ORDER_CREATED = "commerce.order.created";
    public static final String QUEUE_ORDER_STATUS_CHANGED = "commerce.order.status-changed";
    public static final String QUEUE_ORDER_CANCELLED = "commerce.order.cancelled";
    public static final String QUEUE_PAYMENT_COMPLETED = "commerce.payment.completed";
    public static final String QUEUE_PAYMENT_FAILED = "commerce.payment.failed";
    public static final String QUEUE_PAYMENT_REFUNDED = "commerce.payment.refunded";
    public static final String QUEUE_SUBSCRIPTION_EXPIRING = "commerce.subscription.expiring";
    public static final String QUEUE_SUBSCRIPTION_EXPIRED = "commerce.subscription.expired";
    public static final String QUEUE_SUBSCRIPTION_RENEWAL_REQUESTED = "commerce.subscription.renewal-requested";
    public static final String QUEUE_SUBSCRIPTION_ACTIVATED = "commerce.subscription.activated";
    public static final String QUEUE_ORDER_CONFIRMED_INBOUND = "commerce.order.confirmed";

    // Routing keys
    public static final String RK_ORDER_PAID = "commerce.order.paid";
    public static final String RK_ORDER_CREATED = "order.created";
    public static final String RK_ORDER_STATUS_CHANGED = "order.status-changed";
    public static final String RK_ORDER_CANCELLED = "order.cancelled";
    public static final String RK_PAYMENT_COMPLETED = "payment.completed";
    public static final String RK_PAYMENT_FAILED = "payment.failed";
    public static final String RK_PAYMENT_REFUNDED = "payment.refunded";
    public static final String RK_SUBSCRIPTION_EXPIRING = "subscription.expiring";
    public static final String RK_SUBSCRIPTION_EXPIRED = "subscription.expired";
    public static final String RK_SUBSCRIPTION_RENEWAL_REQUESTED = "subscription.renewal-requested";
    public static final String RK_SUBSCRIPTION_ACTIVATED = "subscription.activated";
    public static final String RK_ORDER_CONFIRMED = "commerce.order.confirmed";

    @Bean
    public TopicExchange ebooksExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder.durable(QUEUE_ORDER_CREATED).build();
    }

    @Bean
    public Queue orderStatusChangedQueue() {
        return QueueBuilder.durable(QUEUE_ORDER_STATUS_CHANGED).build();
    }

    @Bean
    public Queue orderCancelledQueue() {
        return QueueBuilder.durable(QUEUE_ORDER_CANCELLED).build();
    }

    @Bean
    public Queue paymentCompletedQueue() {
        return QueueBuilder.durable(QUEUE_PAYMENT_COMPLETED).build();
    }

    @Bean
    public Queue paymentFailedQueue() {
        return QueueBuilder.durable(QUEUE_PAYMENT_FAILED).build();
    }

    @Bean
    public Queue paymentRefundedQueue() {
        return QueueBuilder.durable(QUEUE_PAYMENT_REFUNDED).build();
    }

    @Bean
    public Queue subscriptionExpiringQueue() {
        return QueueBuilder.durable(QUEUE_SUBSCRIPTION_EXPIRING).build();
    }

    @Bean
    public Queue subscriptionExpiredQueue() {
        return QueueBuilder.durable(QUEUE_SUBSCRIPTION_EXPIRED).build();
    }

    @Bean
    public Queue subscriptionRenewalRequestedQueue() {
        return QueueBuilder.durable(QUEUE_SUBSCRIPTION_RENEWAL_REQUESTED).build();
    }

    @Bean
    public Queue subscriptionActivatedQueue() {
        return QueueBuilder.durable(QUEUE_SUBSCRIPTION_ACTIVATED).build();
    }

    @Bean
    public Queue orderConfirmedInboundQueue() {
        return QueueBuilder.durable(QUEUE_ORDER_CONFIRMED_INBOUND).build();
    }

    @Bean
    public Binding orderCreatedBinding() {
        return BindingBuilder.bind(orderCreatedQueue()).to(ebooksExchange()).with(RK_ORDER_CREATED);
    }

    @Bean
    public Binding orderStatusChangedBinding() {
        return BindingBuilder.bind(orderStatusChangedQueue()).to(ebooksExchange()).with(RK_ORDER_STATUS_CHANGED);
    }

    @Bean
    public Binding orderCancelledBinding() {
        return BindingBuilder.bind(orderCancelledQueue()).to(ebooksExchange()).with(RK_ORDER_CANCELLED);
    }

    @Bean
    public Binding paymentCompletedBinding() {
        return BindingBuilder.bind(paymentCompletedQueue()).to(ebooksExchange()).with(RK_PAYMENT_COMPLETED);
    }

    @Bean
    public Binding paymentFailedBinding() {
        return BindingBuilder.bind(paymentFailedQueue()).to(ebooksExchange()).with(RK_PAYMENT_FAILED);
    }

    @Bean
    public Binding paymentRefundedBinding() {
        return BindingBuilder.bind(paymentRefundedQueue()).to(ebooksExchange()).with(RK_PAYMENT_REFUNDED);
    }

    @Bean
    public Binding subscriptionExpiringBinding() {
        return BindingBuilder.bind(subscriptionExpiringQueue()).to(ebooksExchange()).with(RK_SUBSCRIPTION_EXPIRING);
    }

    @Bean
    public Binding subscriptionExpiredBinding() {
        return BindingBuilder.bind(subscriptionExpiredQueue()).to(ebooksExchange()).with(RK_SUBSCRIPTION_EXPIRED);
    }

    @Bean
    public Binding subscriptionRenewalRequestedBinding() {
        return BindingBuilder.bind(subscriptionRenewalRequestedQueue()).to(ebooksExchange()).with(RK_SUBSCRIPTION_RENEWAL_REQUESTED);
    }

    @Bean
    public Binding subscriptionActivatedBinding() {
        return BindingBuilder.bind(subscriptionActivatedQueue()).to(ebooksExchange()).with(RK_SUBSCRIPTION_ACTIVATED);
    }

    @Bean
    public Binding orderConfirmedInboundBinding() {
        return BindingBuilder.bind(orderConfirmedInboundQueue()).to(ebooksExchange()).with(RK_ORDER_CONFIRMED);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
