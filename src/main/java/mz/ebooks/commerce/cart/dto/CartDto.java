package mz.ebooks.commerce.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartDto {

    private UUID id;
    private String userId;
    private List<CartItemDto> items;
    private BigDecimal subtotal;
    private int itemCount;
}
