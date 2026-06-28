package mz.ebooks.commerce.cart.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mz.ebooks.commerce.cart.dto.AddItemRequest;
import mz.ebooks.commerce.cart.dto.CartDto;
import mz.ebooks.commerce.cart.dto.UpdateQuantityRequest;
import mz.ebooks.commerce.cart.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/commerce/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartDto> getCart(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(cartService.getCartDto(userId));
    }

    @PostMapping("/items")
    public ResponseEntity<CartDto> addItem(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody AddItemRequest req) {
        return ResponseEntity.ok(cartService.addItem(userId, req));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartDto> updateQuantity(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateQuantityRequest req) {
        return ResponseEntity.ok(cartService.updateQuantity(userId, itemId, req.getQuantity()));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartDto> removeItem(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID itemId) {
        return ResponseEntity.ok(cartService.removeItem(userId, itemId));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(@RequestHeader("X-User-Id") String userId) {
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }
}
