package mz.ebooks.commerce.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryDto {

    private UUID id;
    private String orderNumber;
    private String status;
    private BigDecimal total;
    private String currency;
    private int itemCount;
    private LocalDateTime createdAt;
}
