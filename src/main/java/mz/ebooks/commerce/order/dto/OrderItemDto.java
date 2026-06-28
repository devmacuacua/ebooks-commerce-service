package mz.ebooks.commerce.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("title")
    private String bookTitle;

    @JsonProperty("type")
    private String bookType;

    @JsonProperty("coverImageUrl")
    private String bookCover;

    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
}
