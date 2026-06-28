package mz.ebooks.commerce.subscription.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mz.ebooks.commerce.payment.dto.PaymentResponse;
import mz.ebooks.commerce.subscription.dto.PlanDto;
import mz.ebooks.commerce.subscription.dto.SubscribeRequest;
import mz.ebooks.commerce.subscription.dto.SubscriptionDto;
import mz.ebooks.commerce.subscription.service.SubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/commerce/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping("/plans")
    public ResponseEntity<List<PlanDto>> getPlans() {
        return ResponseEntity.ok(subscriptionService.getPlans());
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> subscribe(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody SubscribeRequest req) {
        req.setUserId(userId);
        return ResponseEntity.ok(subscriptionService.subscribe(req));
    }

    @GetMapping("/me")
    public ResponseEntity<SubscriptionDto> getMySubscription(
            @RequestHeader("X-User-Id") String userId) {
        return subscriptionService.getUserSubscription(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/me")
    public ResponseEntity<SubscriptionDto> cancelSubscription(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(subscriptionService.cancelSubscription(userId));
    }

    // POST alias so the frontend can call POST /subscriptions/subscribe
    @PostMapping("/subscribe")
    public ResponseEntity<PaymentResponse> subscribeAlias(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody SubscribeRequest req) {
        req.setUserId(userId);
        return ResponseEntity.ok(subscriptionService.subscribe(req));
    }

    // POST alias so the frontend can call POST /subscriptions/cancel
    @PostMapping("/cancel")
    public ResponseEntity<SubscriptionDto> cancelAlias(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(subscriptionService.cancelSubscription(userId));
    }

    @GetMapping("/access")
    public ResponseEntity<Map<String, Boolean>> checkAccess(
            @RequestHeader("X-User-Id") String userId) {
        boolean hasAccess = subscriptionService.hasActiveSubscription(userId);
        return ResponseEntity.ok(Map.of("hasAccess", hasAccess));
    }
}
