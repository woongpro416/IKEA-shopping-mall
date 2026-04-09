package com.example.ikea.dto;

import com.example.ikea.domain.ProductStock;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ProductStockResponseDto {

    private Long stockId;
    private Long productId;
    private Integer quantity;
    private Boolean tracked;
    private LocalDateTime updatedAt;

    public ProductStockResponseDto(ProductStock stock) {
        this.stockId = stock.getStockId();
        this.productId = stock.getProduct().getProductId();
        this.quantity = stock.getQuantity();
        this.tracked = true;
        this.updatedAt = stock.getUpdatedAt();
    }

    public ProductStockResponseDto(Long productId, Integer quantity, Boolean tracked, LocalDateTime updatedAt) {
        this.stockId = null;
        this.productId = productId;
        this.quantity = quantity;
        this.tracked = tracked;
        this.updatedAt = updatedAt;
    }
}