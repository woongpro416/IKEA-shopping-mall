package com.example.ikea.dto;

import com.example.ikea.domain.OrderItem;
import com.example.ikea.domain.Product;
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
        Product product = orderItem.getProduct();

        this.orderItemId = orderItem.getOrderItemId();
        this.productId = product != null ? product.getProductId() : null;
        this.productCode = product != null ? product.getProductCode() : null;
        this.productName = product != null ? product.getName() : "삭제되었거나 조회할 수 없는 상품";
        this.quantity = orderItem.getQuantity();
        this.orderPrice = orderItem.getOrderPrice();
        this.totalPrice = (orderItem.getOrderPrice() != null && orderItem.getQuantity() != null)
                ? orderItem.getOrderPrice() * orderItem.getQuantity()
                : null;
    }
}
