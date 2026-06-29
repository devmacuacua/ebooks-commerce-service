package mz.ebooks.commerce.order.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mz.ebooks.commerce.order.dto.CheckoutResponse;
import mz.ebooks.commerce.order.dto.CreateOrderRequest;
import mz.ebooks.commerce.order.dto.OrderDto;
import mz.ebooks.commerce.order.dto.OrderSummaryDto;
import mz.ebooks.commerce.order.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.HttpStatus;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/commerce/orders")
@RequiredArgsConstructor
public class OrderController {

    private static final Set<String> VALID_STATUSES = Set.of(
            "PENDING", "PAID", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED", "FAILED"
    );

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<CheckoutResponse> createOrder(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateOrderRequest req) {
        req.setUserId(userId);
        return ResponseEntity.ok(orderService.checkout(userId, req));
    }

    @GetMapping
    public ResponseEntity<Page<OrderDto>> getMyOrders(
            @RequestHeader("X-User-Id") String userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(orderService.getUserOrders(userId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrder(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(orderService.getOrder(id, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<OrderDto> cancelOrder(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(orderService.cancelOrder(id, userId));
    }

    @GetMapping("/admin")
    public ResponseEntity<Page<OrderSummaryDto>> adminListOrders(
            @RequestHeader(value = "X-User-Role", defaultValue = "") String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(403).build();
        }
        if (status != null && !status.isBlank() && !VALID_STATUSES.contains(status.toUpperCase())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        return ResponseEntity.ok(orderService.getAdminOrders(
                status != null ? status.toUpperCase() : null, search, pageable));
    }

    @PatchMapping("/admin/{id}/status")
    public ResponseEntity<OrderDto> adminUpdateOrderStatus(
            @RequestHeader(value = "X-User-Role", defaultValue = "") String role,
            @PathVariable UUID id,
            @RequestParam String status) {
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(403).build();
        }
        if (!VALID_STATUSES.contains(status.toUpperCase())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        return ResponseEntity.ok(orderService.updateStatus(id, status.toUpperCase()));
    }
}
