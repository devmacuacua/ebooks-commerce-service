package mz.ebooks.commerce.subscription.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mz.ebooks.commerce.messaging.CommerceEventPublisher;
import mz.ebooks.commerce.subscription.entity.Subscription;
import mz.ebooks.commerce.subscription.service.SubscriptionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionExpiryScheduler {

    private final SubscriptionService subscriptionService;
    private final CommerceEventPublisher eventPublisher;

    @Scheduled(cron = "0 0 8 * * *")
    public void processExpiringSubscriptions() {
        log.info("Running subscription expiry scheduler");

        // 7-day warning
        List<Subscription> expiring7d = subscriptionService.getExpiringSubscriptions(7);
        for (Subscription sub : expiring7d) {
            if (!sub.isExpiryNotified7d()) {
                Map<String, Object> event = buildExpiryEvent(sub, 7);
                eventPublisher.publishSubscriptionExpiring(event);
                subscriptionService.markNotified7d(sub);
                log.info("Sent 7-day expiry notification for subscription {}", sub.getId());
            }
        }

        // 3-day warning
        List<Subscription> expiring3d = subscriptionService.getExpiringSubscriptions(3);
        for (Subscription sub : expiring3d) {
            if (!sub.isExpiryNotified3d()) {
                Map<String, Object> event = buildExpiryEvent(sub, 3);
                eventPublisher.publishSubscriptionExpiring(event);
                subscriptionService.markNotified3d(sub);
                log.info("Sent 3-day expiry notification for subscription {}", sub.getId());
            }
        }

        // 1-day warning
        List<Subscription> expiring1d = subscriptionService.getExpiringSubscriptions(1);
        for (Subscription sub : expiring1d) {
            if (!sub.isExpiryNotified1d()) {
                Map<String, Object> event = buildExpiryEvent(sub, 1);
                eventPublisher.publishSubscriptionExpiring(event);
                subscriptionService.markNotified1d(sub);
                log.info("Sent 1-day expiry notification for subscription {}", sub.getId());
            }
        }

        // Mark expired subscriptions
        List<Subscription> expired = subscriptionService.getExpiredSubscriptions();
        for (Subscription sub : expired) {
            subscriptionService.markExpired(sub);

            Map<String, Object> event = new HashMap<>();
            event.put("subscriptionId", sub.getId().toString());
            event.put("userId", sub.getUserId());
            event.put("planType", sub.getPlan() != null ? sub.getPlan().getType() : "UNKNOWN");
            eventPublisher.publishSubscriptionExpired(event);
            log.info("Subscription {} marked as expired for user {}", sub.getId(), sub.getUserId());
        }

        // Request auto-renewal for expired subscriptions with autoRenew=true
        List<Subscription> toRenew = subscriptionService.getExpiredAutoRenewSubscriptions();
        for (Subscription sub : toRenew) {
            Map<String, Object> event = new HashMap<>();
            event.put("subscriptionId", sub.getId().toString());
            event.put("userId", sub.getUserId());
            event.put("planId", sub.getPlan() != null ? sub.getPlan().getId().toString() : null);
            event.put("planType", sub.getPlan() != null ? sub.getPlan().getType() : "UNKNOWN");
            eventPublisher.publishSubscriptionRenewalRequested(event);
            log.info("Renewal requested for subscription {} (user {})", sub.getId(), sub.getUserId());
        }
    }

    private Map<String, Object> buildExpiryEvent(Subscription sub, int daysLeft) {
        Map<String, Object> event = new HashMap<>();
        event.put("subscriptionId", sub.getId().toString());
        event.put("userId", sub.getUserId());
        event.put("endDate", sub.getEndDate() != null ? sub.getEndDate().toString() : null);
        event.put("daysLeft", daysLeft);
        event.put("planType", sub.getPlan() != null ? sub.getPlan().getType() : "UNKNOWN");
        return event;
    }
}
