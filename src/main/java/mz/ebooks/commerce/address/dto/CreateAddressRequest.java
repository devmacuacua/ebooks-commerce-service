package mz.ebooks.commerce.address.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAddressRequest {

    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "street is required")
    private String street;

    private String number;
    private String complement;

    @NotBlank(message = "district is required")
    private String district;

    @NotBlank(message = "city is required")
    private String city;

    @NotBlank(message = "province is required")
    private String province;

    private String country;
    private String postalCode;
    private boolean isDefault;
}
