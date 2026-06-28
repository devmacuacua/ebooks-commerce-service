package mz.ebooks.commerce.cart.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mz.ebooks.commerce.cart.dto.AddItemRequest;
import mz.ebooks.commerce.cart.dto.CartDto;
import mz.ebooks.commerce.cart.dto.CartItemDto;
import mz.ebooks.commerce.cart.entity.Cart;
import mz.ebooks.commerce.cart.entity.CartItem;
import mz.ebooks.commerce.cart.repository.CartRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;

    @Transactional
    public Cart getOrCreateCart(String userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart cart = Cart.builder().userId(userId).build();
                    return cartRepository.save(cart);
                });
    }

    @Transactional
    public CartDto addItem(String userId, AddItemRequest req) {
        Cart cart = getOrCreateCart(userId);

        // Check if book already in cart — update quantity instead
        cart.getItems().stream()
                .filter(item -> item.getBookId().equals(req.getBookId()))
                .findFirst()
                .ifPresentOrElse(
                        existing -> existing.setQuantity(existing.getQuantity() + req.getQuantity()),
                        () -> {
                            CartItem newItem = CartItem.builder()
                                    .cart(cart)
                                    .bookId(req.getBookId())
                                    .bookTitle(req.getBookTitle())
                                    .bookCover(req.getBookCover())
                                    .bookType(req.getBookType())
                                    .price(req.getPrice())
                                    .quantity(req.getQuantity())
                                    .build();
                            cart.getItems().add(newItem);
                        }
                );

        cartRepository.save(cart);
        return toDto(cart);
    }

    @Transactional
    public CartDto removeItem(String userId, UUID itemId) {
        Cart cart = getOrCreateCart(userId);
        cart.getItems().removeIf(item -> item.getId().equals(itemId));
        cartRepository.save(cart);
        return toDto(cart);
    }

    @Transactional
    public CartDto updateQuantity(String userId, UUID itemId, int quantity) {
        Cart cart = getOrCreateCart(userId);
        cart.getItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .ifPresent(item -> item.setQuantity(quantity));
        cartRepository.save(cart);
        return toDto(cart);
    }

    @Transactional
    public void clearCart(String userId) {
        Cart cart = getOrCreateCart(userId);
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    public CartDto getCartDto(String userId) {
        Cart cart = getOrCreateCart(userId);
        return toDto(cart);
    }

    private CartDto toDto(Cart cart) {
        List<CartItemDto> itemDtos = cart.getItems().stream()
                .map(item -> CartItemDto.builder()
                        .id(item.getId())
                        .bookId(item.getBookId())
                        .bookTitle(item.getBookTitle())
                        .bookCover(item.getBookCover())
                        .bookType(item.getBookType())
                        .price(item.getPrice())
                        .quantity(item.getQuantity())
                        .itemTotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                        .addedAt(item.getAddedAt())
                        .build())
                .toList();

        BigDecimal subtotal = itemDtos.stream()
                .map(CartItemDto::getItemTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartDto.builder()
                .id(cart.getId())
                .userId(cart.getUserId())
                .items(itemDtos)
                .subtotal(subtotal)
                .itemCount(itemDtos.size())
                .build();
    }
}
