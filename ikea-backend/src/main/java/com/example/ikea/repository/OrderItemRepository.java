package com.example.ikea.repository;

import com.example.ikea.domain.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    boolean existsByOrder_OrderIdAndProduct_ProductId(Long orderId, Long productId);
}