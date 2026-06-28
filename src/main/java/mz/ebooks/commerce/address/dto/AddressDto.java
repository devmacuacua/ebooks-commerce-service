package mz.ebooks.commerce.address.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressDto {

    private UUID id;
    private String userId;

    @JsonProperty("label")
    private String name;

    private String street;
    private String number;
    private String complement;
    private String district;
    private String city;
    private String province;
    private String country;
    private String postalCode;
    @JsonProperty("isDefault")
    private boolean isDefault;
    private LocalDateTime createdAt;
}
