package mz.ebooks.commerce.order.dto;

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
public class OrderItemDto {

    private UUID id;
    private UUID bookId;
    private String bookTitle;
    private String bookType;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
}
