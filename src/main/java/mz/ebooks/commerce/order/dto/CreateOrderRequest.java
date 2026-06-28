package mz.ebooks.commerce.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    private String userId;

    private UUID addressId;

    private String notes;

    @NotEmpty(message = "items must not be empty")
    @Valid
    private List<CheckoutItemRequest> items;

    @NotBlank(message = "paymentMethod is required")
    private String paymentMethod;

    private String phoneNumber;

    private String stripePaymentMethodId;

    private String currency;
}
