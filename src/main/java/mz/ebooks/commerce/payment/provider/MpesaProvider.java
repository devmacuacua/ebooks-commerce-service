package mz.ebooks.commerce.payment.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Cipher;
import java.math.BigDecimal;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class MpesaProvider {

    @Value("${mpesa.api-url}")
    private String apiUrl;

    @Value("${mpesa.api-key}")
    private String apiKey;

    @Value("${mpesa.public-key}")
    private String publicKey;

    @Value("${mpesa.service-provider-code}")
    private String serviceProviderCode;

    private final RestTemplate restTemplate;

    public MpesaProvider(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String generateBearerToken() {
        try {
            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKey);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey rsaPublicKey = keyFactory.generatePublic(keySpec);

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, rsaPublicKey);
            byte[] encryptedBytes = cipher.doFinal(apiKey.getBytes());
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            log.error("Failed to generate M-Pesa bearer token", e);
            throw new RuntimeException("Failed to generate M-Pesa bearer token: " + e.getMessage(), e);
        }
    }

    public MpesaPaymentResult initiateC2B(String msisdn, BigDecimal amount, String thirdPartyRef) {
        String token = generateBearerToken();
        String url = apiUrl + "/ipg/v1x/c2bPayment/singleStage/";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + token);
        headers.set("Origin", "developer.mpesa.vm.co.mz");

        Map<String, String> body = new HashMap<>();
        body.put("input_TransactionReference", "T" + System.currentTimeMillis());
        body.put("input_CustomerMSISDN", msisdn);
        body.put("input_Amount", String.valueOf(amount.intValue()));
        body.put("input_ThirdPartyReference", thirdPartyRef);
        body.put("input_ServiceProviderCode", serviceProviderCode);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null) {
                String responseCode = (String) responseBody.get("output_ResponseCode");
                String conversationId = (String) responseBody.get("output_ConversationID");
                String transactionId = (String) responseBody.get("output_TransactionID");
                boolean success = "INS-0".equals(responseCode);
                return new MpesaPaymentResult(success, conversationId, transactionId, responseCode,
                        (String) responseBody.get("output_ResponseDesc"));
            }
            return new MpesaPaymentResult(false, null, null, "NO_RESPONSE", "No response from M-Pesa API");
        } catch (Exception e) {
            log.error("M-Pesa C2B initiation failed for ref {}: {}", thirdPartyRef, e.getMessage(), e);
            return new MpesaPaymentResult(false, null, null, "ERROR", e.getMessage());
        }
    }

    public MpesaStatusResult checkStatus(String conversationId, String thirdPartyRef) {
        String token = generateBearerToken();
        String url = apiUrl + "/ipg/v1x/queryTransactionStatus/?input_QueryReference=" + conversationId
                + "&input_ServiceProviderCode=" + serviceProviderCode
                + "&input_ThirdPartyReference=" + thirdPartyRef;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("Origin", "developer.mpesa.vm.co.mz");

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null) {
                String responseCode = (String) responseBody.get("output_ResponseCode");
                boolean success = "INS-0".equals(responseCode);
                return new MpesaStatusResult(success, responseCode, (String) responseBody.get("output_ResponseDesc"));
            }
            return new MpesaStatusResult(false, "NO_RESPONSE", "No response");
        } catch (Exception e) {
            log.error("M-Pesa status check failed for conversationId {}: {}", conversationId, e.getMessage(), e);
            return new MpesaStatusResult(false, "ERROR", e.getMessage());
        }
    }

    public record MpesaPaymentResult(boolean success, String conversationId, String transactionId,
                                     String responseCode, String responseDesc) {}

    public record MpesaStatusResult(boolean success, String responseCode, String responseDesc) {}
}
