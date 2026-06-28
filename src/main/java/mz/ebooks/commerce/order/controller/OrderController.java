package mz.ebooks.commerce.order.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mz.ebooks.commerce.order.dto.CreateOrderRequest;
import mz.ebooks.commerce.order.dto.OrderDto;
import mz.ebooks.commerce.order.dto.OrderSummaryDto;
import mz.ebooks.commerce.order.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/commerce/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderDto> createOrder(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateOrderRequest req) {
        req.setUserId(userId);
        return ResponseEntity.ok(orderService.createOrderFromCart(userId, req.getAddressId(), req.getNotes()));
    }

    @GetMapping
    public ResponseEntity<Page<OrderSummaryDto>> getMyOrders(
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
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(orderService.getAdminOrders(status, pageable));
    }

    @PatchMapping("/admin/{id}/status")
    public ResponseEntity<OrderDto> adminUpdateOrderStatus(
            @RequestHeader(value = "X-User-Role", defaultValue = "") String role,
            @PathVariable UUID id,
            @RequestParam String status) {
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(orderService.updateStatus(id, status));
    }
}
