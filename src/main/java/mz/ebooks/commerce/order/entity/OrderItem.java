package mz.ebooks.commerce.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "book_id", nullable = false)
    private UUID bookId;

    @Column(name = "book_title", nullable = false, length = 500)
    private String bookTitle;

    @Column(name = "book_type", nullable = false, length = 20)
    private String bookType;

    @Column(name = "quantity")
    @Builder.Default
    private int quantity = 1;

    @Column(name = "book_cover", length = 1000)
    private String bookCover;

    @Column(name = "book_slug", length = 300)
    private String bookSlug;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;
}
