package mz.ebooks.commerce.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutResponse {

    private UUID orderId;
    private UUID paymentId;
    private String status;
    private String clientSecret;
    private String redirectUrl;
    private String instructions;
    private String method;
}
