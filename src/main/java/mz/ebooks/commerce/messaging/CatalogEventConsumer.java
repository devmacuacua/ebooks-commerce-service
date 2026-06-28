package mz.ebooks.commerce.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mz.ebooks.commerce.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class CatalogEventConsumer {

    private final RestTemplate restTemplate;

    @Value("${app.catalog-service-url}")
    private String catalogServiceUrl;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_ORDER_CONFIRMED_INBOUND)
    public void handleOrderConfirmed(Map<String, Object> event) {
        log.info("Received order.confirmed event: {}", event);
        try {
            String orderId = (String) event.get("orderId");
            Object items = event.get("items");
            if (orderId == null || items == null) {
                log.warn("Invalid order.confirmed event — missing orderId or items");
                return;
            }

            String url = catalogServiceUrl + "/catalog/stock/reduce";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> request = new HttpEntity<>(event, headers);

            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.POST, request, Void.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Stock reduced successfully for order {}", orderId);
            } else {
                log.warn("Stock reduction returned non-2xx for order {}: {}", orderId, response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Failed to process order.confirmed event: {}", e.getMessage(), e);
        }
    }
}
