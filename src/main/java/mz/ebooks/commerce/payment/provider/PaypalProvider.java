package mz.ebooks.commerce.payment.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@Slf4j
public class PaypalProvider {

    @Value("${paypal.client-id}")
    private String clientId;

    @Value("${paypal.client-secret}")
    private String clientSecret;

    @Value("${paypal.mode:sandbox}")
    private String mode;

    private final RestTemplate restTemplate;

    public PaypalProvider(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private String getBaseUrl() {
        return "sandbox".equalsIgnoreCase(mode)
                ? "https://api-m.sandbox.paypal.com"
                : "https://api-m.paypal.com";
    }

    public String getAccessToken() {
        String url = getBaseUrl() + "/v1/oauth2/token";

        String credentials = clientId + ":" + clientSecret;
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Basic " + encoded);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null) {
                return (String) responseBody.get("access_token");
            }
            throw new RuntimeException("No access token in PayPal response");
        } catch (Exception e) {
            log.error("Failed to get PayPal access token: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to authenticate with PayPal: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public PaypalOrderResult createOrder(BigDecimal amount, String currency, String returnUrl, String cancelUrl) {
        String accessToken = getAccessToken();
        String url = getBaseUrl() + "/v2/checkout/orders";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + accessToken);

        Map<String, Object> amountMap = new HashMap<>();
        amountMap.put("currency_code", currency.toUpperCase());
        amountMap.put("value", String.format("%.2f", amount));

        Map<String, Object> purchaseUnit = new HashMap<>();
        purchaseUnit.put("amount", amountMap);

        Map<String, String> applicationContext = new HashMap<>();
        applicationContext.put("return_url", returnUrl != null ? returnUrl : "");
        applicationContext.put("cancel_url", cancelUrl != null ? cancelUrl : "");

        Map<String, Object> orderPayload = new HashMap<>();
        orderPayload.put("intent", "CAPTURE");
        orderPayload.put("purchase_units", List.of(purchaseUnit));
        orderPayload.put("application_context", applicationContext);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(orderPayload, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null) {
                String orderId = (String) responseBody.get("id");
                String approvalUrl = extractApprovalUrl(responseBody);
                return new PaypalOrderResult(true, orderId, approvalUrl, null);
            }
            return new PaypalOrderResult(false, null, null, "No response from PayPal");
        } catch (Exception e) {
            log.error("Failed to create PayPal order: {}", e.getMessage(), e);
            return new PaypalOrderResult(false, null, null, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private String extractApprovalUrl(Map<String, Object> responseBody) {
        List<Map<String, Object>> links = (List<Map<String, Object>>) responseBody.get("links");
        if (links != null) {
            return links.stream()
                    .filter(link -> "approve".equals(link.get("rel")))
                    .map(link -> (String) link.get("href"))
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    public PaypalCaptureResult captureOrder(String orderId) {
        String accessToken = getAccessToken();
        String url = getBaseUrl() + "/v2/checkout/orders/" + orderId + "/capture";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null) {
                String status = (String) responseBody.get("status");
                boolean success = "COMPLETED".equalsIgnoreCase(status);
                return new PaypalCaptureResult(success, orderId, status, null);
            }
            return new PaypalCaptureResult(false, orderId, "UNKNOWN", "No response from PayPal");
        } catch (Exception e) {
            log.error("Failed to capture PayPal order {}: {}", orderId, e.getMessage(), e);
            return new PaypalCaptureResult(false, orderId, "ERROR", e.getMessage());
        }
    }

    public record PaypalOrderResult(boolean success, String orderId, String approvalUrl, String errorMessage) {}

    public record PaypalCaptureResult(boolean success, String orderId, String status, String errorMessage) {}
}
