package mz.ebooks.commerce.address.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "addresses")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "street", nullable = false, length = 500)
    private String street;

    @Column(name = "number", length = 50)
    private String number;

    @Column(name = "complement", length = 255)
    private String complement;

    @Column(name = "district", nullable = false, length = 255)
    private String district;

    @Column(name = "city", nullable = false, length = 255)
    private String city;

    @Column(name = "province", nullable = false, length = 255)
    private String province;

    @Column(name = "country", length = 100)
    @Builder.Default
    private String country = "Moçambique";

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "is_default")
    @Builder.Default
    private boolean isDefault = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        if (country == null) {
            country = "Moçambique";
        }
    }
}
