package mz.ebooks.commerce.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

@Component
@Slf4j
public class DeliveryFeeClient {

    private static final BigDecimal FALLBACK_FEE = new BigDecimal("150.00");

    @Value("${delivery.service.url:http://localhost:3006}")
    private String deliveryServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @SuppressWarnings("unchecked")
    public BigDecimal getFeeForProvince(String province) {
        if (province == null || province.isBlank()) return FALLBACK_FEE;
        try {
            String url = deliveryServiceUrl + "/deliveries/fee?province=" + province;
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.get("fee") != null) {
                return new BigDecimal(response.get("fee").toString());
            }
        } catch (Exception e) {
            log.warn("Could not fetch delivery fee for province '{}', using fallback: {}", province, e.getMessage());
        }
        return FALLBACK_FEE;
    }
}
