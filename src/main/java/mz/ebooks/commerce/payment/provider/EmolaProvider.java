package mz.ebooks.commerce.payment.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import org.springframework.core.ParameterizedTypeReference;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class EmolaProvider {

    @Value("${emola.api-url}")
    private String emolaApiUrl;

    @Value("${emola.api-key}")
    private String apiKey;

    @Value("${emola.merchant-code}")
    private String merchantCode;

    private final RestTemplate restTemplate;

    public EmolaProvider(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public EmolaPaymentResult initiatePayment(String phone, BigDecimal amount, String reference) {
        String url = emolaApiUrl + "/payment";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("merchantCode", merchantCode);
        body.put("amount", amount);
        body.put("msisdn", phone);
        body.put("reference", reference);
        body.put("description", "EBooks Purchase");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(url, HttpMethod.POST, request, new ParameterizedTypeReference<Map<String, Object>>() {});
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null) {
                String status = (String) responseBody.get("status");
                String ref = (String) responseBody.getOrDefault("reference", reference);
                boolean success = response.getStatusCode().is2xxSuccessful() && "PENDING".equalsIgnoreCase(status);
                return new EmolaPaymentResult(success, ref, status, null);
            }
            return new EmolaPaymentResult(false, reference, "ERROR", "No response from E-mola API");
        } catch (Exception e) {
            log.error("E-mola payment initiation failed for ref {}: {}", reference, e.getMessage(), e);
            return new EmolaPaymentResult(false, reference, "ERROR", e.getMessage());
        }
    }

    public EmolaStatusResult checkStatus(String reference) {
        String url = emolaApiUrl + "/payment/status/" + reference;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(url, HttpMethod.GET, request, new ParameterizedTypeReference<Map<String, Object>>() {});
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null) {
                String status = (String) responseBody.get("status");
                boolean success = "COMPLETED".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status);
                return new EmolaStatusResult(success, status, null);
            }
            return new EmolaStatusResult(false, "UNKNOWN", "No response");
        } catch (Exception e) {
            log.error("E-mola status check failed for ref {}: {}", reference, e.getMessage(), e);
            return new EmolaStatusResult(false, "ERROR", e.getMessage());
        }
    }

    public record EmolaPaymentResult(boolean success, String reference, String status, String errorMessage) {}

    public record EmolaStatusResult(boolean success, String status, String errorMessage) {}
}
