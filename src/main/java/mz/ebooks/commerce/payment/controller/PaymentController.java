package mz.ebooks.commerce.payment.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mz.ebooks.commerce.payment.dto.InitiatePaymentRequest;
import mz.ebooks.commerce.payment.dto.PaymentDto;
import mz.ebooks.commerce.payment.dto.PaymentResponse;
import mz.ebooks.commerce.payment.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/commerce/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/initiate")
    public ResponseEntity<PaymentResponse> initiatePayment(
            @Valid @RequestBody InitiatePaymentRequest req) {
        return ResponseEntity.ok(paymentService.initiatePayment(req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentDto> getPayment(@PathVariable UUID id) {
        return ResponseEntity.ok(paymentService.getPayment(id));
    }

    @PostMapping("/webhooks/stripe")
    public ResponseEntity<Void> stripeWebhook(
            @RequestBody String rawBody,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        paymentService.handleStripeWebhook(rawBody, sigHeader);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/callbacks/mpesa")
    public ResponseEntity<Void> mpesaCallback(@RequestBody Map<String, Object> body) {
        paymentService.handleMpesaCallback(body);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/callbacks/emola")
    public ResponseEntity<Void> emolaCallback(@RequestBody Map<String, Object> body) {
        paymentService.handleEmolaCallback(body);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/capture/paypal")
    public ResponseEntity<PaymentResponse> capturePaypal(@RequestParam String orderId) {
        return ResponseEntity.ok(paymentService.handlePaypalCapture(orderId));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<PaymentDto> refundPayment(@PathVariable UUID id) {
        return ResponseEntity.ok(paymentService.refundPayment(id));
    }
}
