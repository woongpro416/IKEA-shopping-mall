package com.example.ikea.dto;

import com.example.ikea.domain.Order;
import com.example.ikea.domain.OrderItem;
import com.example.ikea.domain.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public class OrderResponseDto {

    private Long orderId;

    private OrderStatus orderStatus;

    private Integer totalPrice;

    private Integer finalPrice;

    private String orderNo;

//    private String payment;

    private String address;

    private List<OrderItemDto> orderItems;

    public OrderResponseDto(Order order) {
        this.orderId = order.getOrderId();
        this.orderStatus = order.getOrderStatus();
        this.totalPrice = order.getTotalPrice();
        this.finalPrice = order.getFinalPrice();
        this.orderNo = order.getOrderNo();
        this.address = order.getAddress();
        this.orderItems = order.getOrderItemList().stream()
                .map(OrderItemDto::new)
                .collect(Collectors.toList());
    }
}
