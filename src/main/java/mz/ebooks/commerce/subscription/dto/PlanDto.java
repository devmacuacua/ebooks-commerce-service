package mz.ebooks.commerce.subscription.dto;

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
public class PlanDto {

    private UUID id;
    private String name;
    private String description;
    private String type;
    private BigDecimal price;
    private String currency;
    private boolean isActive;
    private List<String> features;
}
