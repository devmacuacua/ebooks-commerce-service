package mz.ebooks.commerce.order.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mz.ebooks.commerce.cart.entity.Cart;
import mz.ebooks.commerce.cart.entity.CartItem;
import mz.ebooks.commerce.cart.service.CartService;
import mz.ebooks.commerce.messaging.CommerceEventPublisher;
import mz.ebooks.commerce.order.dto.OrderDto;
import mz.ebooks.commerce.order.dto.OrderItemDto;
import mz.ebooks.commerce.order.dto.OrderSummaryDto;
import mz.ebooks.commerce.order.entity.Order;
import mz.ebooks.commerce.order.entity.OrderItem;
import mz.ebooks.commerce.order.repository.OrderRepository;
import mz.ebooks.commerce.payment.repository.PaymentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final CartService cartService;
    private final CommerceEventPublisher eventPublisher;

    @Transactional
    public OrderDto createOrderFromCart(String userId, UUID addressId, String notes) {
        Cart cart = cartService.getOrCreateCart(userId);

        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }

        // Calculate subtotal and delivery fee
        BigDecimal subtotal = cart.getItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean hasPhysical = cart.getItems().stream()
                .anyMatch(item -> "PHYSICAL".equalsIgnoreCase(item.getBookType()));

        BigDecimal deliveryFee = hasPhysical ? new BigDecimal("150.00") : BigDecimal.ZERO;
        BigDecimal total = subtotal.add(deliveryFee);

        String orderNumber = generateOrderNumber();

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .userId(userId)
                .addressId(addressId)
                .status("PENDING")
                .subtotal(subtotal)
                .deliveryFee(deliveryFee)
                .total(total)
                .currency("MZN")
                .notes(notes)
                .build();

        // Build order items
        List<OrderItem> orderItems = cart.getItems().stream()
                .map(cartItem -> OrderItem.builder()
                        .bookId(cartItem.getBookId())
                        .bookTitle(cartItem.getBookTitle())
                        .bookType(cartItem.getBookType())
                        .bookCover(cartItem.getBookCover())
                        .quantity(cartItem.getQuantity())
                        .unitPrice(cartItem.getPrice())
                        .totalPrice(cartItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())))
                        .build())
                .toList();

        order = orderRepository.save(order);

        order.getItems().addAll(orderItems);
        order = orderRepository.save(order);

        // Clear cart
        cartService.clearCart(userId);

        // Publish event
        Map<String, Object> event = new HashMap<>();
        event.put("orderId", order.getId().toString());
        event.put("orderNumber", order.getOrderNumber());
        event.put("userId", userId);
        event.put("total", order.getTotal());
        event.put("currency", order.getCurrency());
        eventPublisher.publishOrderCreated(event);

        log.info("Order {} created for user {}", orderNumber, userId);
        return toDto(order, null);
    }

    public Page<OrderSummaryDto> getUserOrders(String userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable)
                .map(this::toSummaryDto);
    }

    public Page<OrderSummaryDto> getAdminOrders(String status, Pageable pageable) {
        Page<Order> page = (status != null && !status.isBlank())
                ? orderRepository.findByStatus(status, pageable)
                : orderRepository.findAll(pageable);
        return page.map(this::toSummaryDto);
    }

    public OrderDto getOrder(UUID orderId, String userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        String paymentStatus = paymentRepository.findByOrderId(orderId).stream()
                .filter(p -> "COMPLETED".equals(p.getStatus()))
                .findFirst()
                .map(p -> p.getStatus())
                .orElse(null);
        return toDto(order, paymentStatus);
    }

    @Transactional
    public OrderDto updateStatus(UUID orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        String previousStatus = order.getStatus();
        order.setStatus(status);
        order = orderRepository.save(order);

        Map<String, Object> event = new HashMap<>();
        event.put("orderId", orderId.toString());
        event.put("orderNumber", order.getOrderNumber());
        event.put("userId", order.getUserId());
        event.put("previousStatus", previousStatus);
        event.put("newStatus", status);
        eventPublisher.publishOrderStatusChanged(event);

        return toDto(order, null);
    }

    @Transactional
    public OrderDto cancelOrder(UUID orderId, String userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!Set.of("PENDING", "PAID").contains(order.getStatus())) {
            throw new IllegalStateException("Order cannot be cancelled in status: " + order.getStatus());
        }

        order.setStatus("CANCELLED");
        order = orderRepository.save(order);

        Map<String, Object> event = new HashMap<>();
        event.put("orderId", orderId.toString());
        event.put("orderNumber", order.getOrderNumber());
        event.put("userId", userId);
        eventPublisher.publishOrderCancelled(event);

        log.info("Order {} cancelled by user {}", order.getOrderNumber(), userId);
        return toDto(order, null);
    }

    private String generateOrderNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int random = new Random().nextInt(9000) + 1000;
        return "EBS" + datePart + random;
    }

    private OrderDto toDto(Order order, String paymentStatus) {
        List<OrderItemDto> itemDtos = order.getItems().stream()
                .map(item -> OrderItemDto.builder()
                        .id(item.getId())
                        .bookId(item.getBookId())
                        .bookTitle(item.getBookTitle())
                        .bookType(item.getBookType())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .totalPrice(item.getTotalPrice())
                        .build())
                .toList();

        return OrderDto.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .addressId(order.getAddressId())
                .status(order.getStatus())
                .subtotal(order.getSubtotal())
                .deliveryFee(order.getDeliveryFee())
                .total(order.getTotal())
                .currency(order.getCurrency())
                .notes(order.getNotes())
                .items(itemDtos)
                .paymentStatus(paymentStatus)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private OrderSummaryDto toSummaryDto(Order order) {
        return OrderSummaryDto.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .total(order.getTotal())
                .currency(order.getCurrency())
                .itemCount(order.getItems().size())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
