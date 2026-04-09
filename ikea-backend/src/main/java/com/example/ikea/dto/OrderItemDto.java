package com.example.ikea.dto;

import com.example.ikea.domain.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderItemDto {

    private Long orderItemId;
    private Long productId;
    private String productCode;
    private String productName;
    private Integer quantity;
    private Integer orderPrice;
    private Integer totalPrice;

    public OrderItemDto(OrderItem orderItem) {
        this.orderItemId = orderItem.getOrderItemId();
        this.productId = orderItem.getProduct().getProductId();
        this.productCode = orderItem.getProduct().getProductCode();
        this.productName = orderItem.getProduct().getName();
        this.quantity = orderItem.getQuantity();
        this.orderPrice = orderItem.getOrderPrice();
        this.totalPrice = orderItem.getOrderPrice() * orderItem.getQuantity();
    }
}