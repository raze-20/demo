package com.raze.coffeeshop.repository;

import com.raze.coffeeshop.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    List<OrderItem> findByOrderId(UUID orderId);

    Optional<OrderItem> findByIdAndOrderId(UUID id, UUID orderId);
}
