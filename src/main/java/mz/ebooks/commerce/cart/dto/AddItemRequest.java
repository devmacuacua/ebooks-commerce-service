package mz.ebooks.commerce.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
public class AddItemRequest {

    @NotNull(message = "bookId is required")
    private UUID bookId;

    @NotBlank(message = "bookTitle is required")
    private String bookTitle;

    @NotBlank(message = "bookType is required")
    private String bookType;

    @NotNull(message = "price is required")
    private BigDecimal price;

    private String bookCover;

    @Min(value = 1, message = "quantity must be at least 1")
    private int quantity = 1;
}
