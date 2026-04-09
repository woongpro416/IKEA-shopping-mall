package com.example.ikea.dto;

import com.example.ikea.domain.CartItem;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CartItemResponseDto {

    private Long cartItemId;
    private Long productId;
    private String productCode;
    private String productName;
    private Integer price;
    private String imgPath;
    private Integer quantity;
    private Integer totalPrice;

    public CartItemResponseDto(CartItem cartItem) {
        this.cartItemId = cartItem.getCartItemId();
        this.productId = cartItem.getProduct().getProductId();
        this.productCode = cartItem.getProduct().getProductCode();
        this.productName = cartItem.getProduct().getName();
        this.price = cartItem.getProduct().getPrice();
        this.imgPath = cartItem.getProduct().getImgPath();
        this.quantity = cartItem.getQuantity();
        this.totalPrice = cartItem.getProduct().getPrice() * cartItem.getQuantity();
    }
}