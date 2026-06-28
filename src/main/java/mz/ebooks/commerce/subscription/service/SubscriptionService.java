package mz.ebooks.commerce.subscription.service;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import mz.ebooks.commerce.payment.dto.InitiatePaymentRequest;
import mz.ebooks.commerce.payment.dto.PaymentResponse;
import mz.ebooks.commerce.messaging.CommerceEventPublisher;
import mz.ebooks.commerce.subscription.dto.PlanDto;
import mz.ebooks.commerce.subscription.dto.SubscribeRequest;
import mz.ebooks.commerce.subscription.dto.SubscriptionDto;
import mz.ebooks.commerce.subscription.entity.Subscription;
import mz.ebooks.commerce.subscription.entity.SubscriptionPlan;
import mz.ebooks.commerce.subscription.repository.SubscriptionPlanRepository;
import mz.ebooks.commerce.subscription.repository.SubscriptionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final CommerceEventPublisher eventPublisher;
    private final org.springframework.context.ApplicationContext applicationContext;
    private final String frontendUrl;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               SubscriptionPlanRepository planRepository,
                               CommerceEventPublisher eventPublisher,
                               org.springframework.context.ApplicationContext applicationContext,
                               @Value("${app.frontend-url}") String frontendUrl) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.eventPublisher = eventPublisher;
        this.applicationContext = applicationContext;
        this.frontendUrl = frontendUrl;
    }

    // Lazy to break circular dependency with PaymentService
    private mz.ebooks.commerce.payment.service.PaymentService getPaymentService() {
        return applicationContext.getBean(mz.ebooks.commerce.payment.service.PaymentService.class);
    }

    public List<PlanDto> getPlans() {
        return planRepository.findByIsActiveTrue().stream()
                .map(this::toPlanDto)
                .toList();
    }

    @Transactional
    public PaymentResponse subscribe(SubscribeRequest req) {
        SubscriptionPlan plan = planRepository.findById(req.getPlanId())
                .orElseThrow(() -> new RuntimeException("Plan not found: " + req.getPlanId()));

        // Cancel existing subscription if present
        subscriptionRepository.findByUserId(req.getUserId()).ifPresent(existing -> {
            if ("ACTIVE".equals(existing.getStatus()) || "PENDING".equals(existing.getStatus())) {
                existing.setStatus("CANCELLED");
                existing.setCancelledAt(LocalDateTime.now());
                subscriptionRepository.save(existing);
            }
        });

        Subscription subscription = Subscription.builder()
                .userId(req.getUserId())
                .plan(plan)
                .status("PENDING")
                .autoRenew(true)
                .build();

        subscription = subscriptionRepository.save(subscription);

        InitiatePaymentRequest paymentRequest = InitiatePaymentRequest.builder()
                .userId(req.getUserId())
                .subscriptionId(subscription.getId())
                .method(req.getMethod())
                .amount(plan.getPrice())
                .currency(plan.getCurrency())
                .phoneNumber(req.getPhoneNumber())
                .returnUrl(frontendUrl + "/checkout/paypal/capture")
                .cancelUrl(frontendUrl + "/checkout?paypal=cancelled")
                .build();

        return getPaymentService().initiatePayment(paymentRequest);
    }

    @Transactional
    public void activateSubscription(UUID subscriptionId, UUID paymentId) {
        subscriptionRepository.findById(subscriptionId).ifPresent(subscription -> {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime endDate;

            String planType = subscription.getPlan() != null ? subscription.getPlan().getType() : "MONTHLY";
            if ("ANNUAL".equalsIgnoreCase(planType)) {
                endDate = now.plusDays(365);
            } else {
                endDate = now.plusDays(30);
            }

            subscription.setStatus("ACTIVE");
            subscription.setStartDate(now);
            subscription.setEndDate(endDate);
            subscription.setExpiryNotified7d(false);
            subscription.setExpiryNotified3d(false);
            subscription.setExpiryNotified1d(false);
            subscriptionRepository.save(subscription);
            log.info("Subscription {} activated for user {} until {}", subscriptionId, subscription.getUserId(), endDate);

            Map<String, Object> event = new HashMap<>();
            event.put("userId", subscription.getUserId());
            event.put("subscriptionId", subscriptionId.toString());
            event.put("planId", subscription.getPlan() != null ? subscription.getPlan().getId().toString() : null);
            event.put("planName", subscription.getPlan() != null ? subscription.getPlan().getName() : null);
            event.put("expiresAt", endDate.toString());
            eventPublisher.publishSubscriptionActivated(event);
        });
    }

    @Transactional
    public SubscriptionDto cancelSubscription(String userId) {
        Subscription subscription = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Subscription not found for user: " + userId));

        subscription.setStatus("CANCELLED");
        subscription.setCancelledAt(LocalDateTime.now());
        subscription.setAutoRenew(false);
        subscription = subscriptionRepository.save(subscription);

        log.info("Subscription cancelled for user {}", userId);
        return toDto(subscription);
    }

    public Optional<SubscriptionDto> getUserSubscription(String userId) {
        return subscriptionRepository.findByUserId(userId).map(this::toDto);
    }

    public boolean hasActiveSubscription(String userId) {
        return subscriptionRepository.findByUserId(userId)
                .map(s -> "ACTIVE".equals(s.getStatus()) && s.getEndDate() != null
                        && s.getEndDate().isAfter(LocalDateTime.now()))
                .orElse(false);
    }

    public List<Subscription> getExpiringSubscriptions(int days) {
        LocalDateTime from = LocalDateTime.now().plusDays(days).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime to = from.plusDays(1);
        return subscriptionRepository.findExpiringSubscriptions(from, to);
    }

    public List<Subscription> getExpiredSubscriptions() {
        return subscriptionRepository.findExpiredSubscriptions(LocalDateTime.now());
    }

    public List<Subscription> getExpiredAutoRenewSubscriptions() {
        return subscriptionRepository.findExpiredAutoRenewSubscriptions();
    }

    @Transactional
    public void markExpired(Subscription subscription) {
        subscription.setStatus("EXPIRED");
        subscriptionRepository.save(subscription);
    }

    @Transactional
    public void markNotified7d(Subscription subscription) {
        subscription.setExpiryNotified7d(true);
        subscriptionRepository.save(subscription);
    }

    @Transactional
    public void markNotified3d(Subscription subscription) {
        subscription.setExpiryNotified3d(true);
        subscriptionRepository.save(subscription);
    }

    @Transactional
    public void markNotified1d(Subscription subscription) {
        subscription.setExpiryNotified1d(true);
        subscriptionRepository.save(subscription);
    }

    private PlanDto toPlanDto(SubscriptionPlan plan) {
        List<String> features = new ArrayList<>(List.of(
                "Ebooks ilimitados",
                "Leitura multi-dispositivo",
                "Novidades semanais"
        ));
        if ("ANNUAL".equalsIgnoreCase(plan.getType())) {
            features.addAll(List.of(
                    "Acesso antecipado",
                    "1 livro físico/trimestre",
                    "Suporte prioritário"
            ));
        }
        return PlanDto.builder()
                .id(plan.getId())
                .name(plan.getName())
                .description(plan.getDescription())
                .type(plan.getType())
                .price(plan.getPrice())
                .currency(plan.getCurrency())
                .isActive(plan.isActive())
                .features(features)
                .build();
    }

    private SubscriptionDto toDto(Subscription s) {
        return SubscriptionDto.builder()
                .id(s.getId())
                .userId(s.getUserId())
                .plan(s.getPlan() != null ? toPlanDto(s.getPlan()) : null)
                .status(s.getStatus())
                .startDate(s.getStartDate())
                .endDate(s.getEndDate())
                .autoRenew(s.isAutoRenew())
                .cancelledAt(s.getCancelledAt())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
