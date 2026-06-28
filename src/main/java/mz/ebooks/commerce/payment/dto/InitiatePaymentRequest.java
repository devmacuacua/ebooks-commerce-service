package mz.ebooks.commerce.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiatePaymentRequest {

    @NotNull(message = "userId is required")
    private String userId;

    private UUID orderId;

    private UUID subscriptionId;

    @NotNull(message = "method is required")
    private String method;

    @NotNull(message = "amount is required")
    private BigDecimal amount;

    private String currency;

    private String phoneNumber;

    private String returnUrl;

    private String cancelUrl;
}
