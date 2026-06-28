package mz.ebooks.commerce.subscription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionDto {

    private UUID id;
    private String userId;
    private PlanDto plan;
    private String status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private boolean autoRenew;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
