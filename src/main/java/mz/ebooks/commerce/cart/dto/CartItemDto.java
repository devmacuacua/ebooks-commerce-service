package mz.ebooks.commerce.cart.dto;

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
public class CartItemDto {

    private UUID id;
    private UUID bookId;
    private String bookTitle;
    private String bookCover;
    private String bookType;
    private BigDecimal price;
    private int quantity;
    private BigDecimal itemTotal;
    private LocalDateTime addedAt;
}
