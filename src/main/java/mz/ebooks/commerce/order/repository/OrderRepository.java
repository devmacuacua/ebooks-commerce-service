package mz.ebooks.commerce.order.repository;

import mz.ebooks.commerce.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Page<Order> findByUserId(String userId, Pageable pageable);

    Optional<Order> findByIdAndUserId(UUID id, String userId);

    Optional<Order> findByOrderNumber(String orderNumber);

    Page<Order> findByStatus(String status, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE " +
           "(:status IS NULL OR :status = '' OR o.status = :status) AND " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(o.userId) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Order> searchOrders(@Param("status") String status,
                              @Param("search") String search,
                              Pageable pageable);
}
