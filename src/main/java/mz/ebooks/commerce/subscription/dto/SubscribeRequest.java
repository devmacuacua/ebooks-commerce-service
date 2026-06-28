package mz.ebooks.commerce.subscription.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscribeRequest {

    private String userId;

    @NotNull(message = "planId is required")
    private UUID planId;

    @NotNull(message = "method is required")
    @JsonAlias("paymentMethod")
    private String method;

    private String phoneNumber;

    private String returnUrl;

    private String cancelUrl;
}
