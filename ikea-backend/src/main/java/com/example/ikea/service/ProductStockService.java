package com.example.ikea.service;

import com.example.ikea.domain.ProductStock;
import com.example.ikea.dto.ProductStockRequestDto;
import com.example.ikea.dto.ProductStockResponseDto;
import com.example.ikea.exception.ProductStockNotFoundException;
import com.example.ikea.repository.ProductStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductStockService {

    private final ProductStockRepository productStockRepository;

    @Transactional(readOnly = true)
    public ProductStockResponseDto getStockByProductId(Long productId) {
        return productStockRepository.findByProduct_ProductId(productId)
                .map(ProductStockResponseDto::new)
                .orElseGet(() -> new ProductStockResponseDto(productId, null, false, null));
    }

    @Transactional
    public ProductStockResponseDto updateStock(Long productId, ProductStockRequestDto dto) {
        ProductStock stock = productStockRepository.findByProduct_ProductId(productId)
                .orElseThrow(() -> new ProductStockNotFoundException("해당 상품의 재고 정보가 없습니다."));

        stock.setQuantity(dto.getQuantity());

        return new ProductStockResponseDto(stock);
    }

    @Transactional
    public void decreaseStock(Long productId, Integer quantity) {
        ProductStock stock = productStockRepository.findByProduct_ProductId(productId)
                .orElseThrow(() -> new ProductStockNotFoundException("해당 상품의 재고 정보가 없습니다."));

        if (stock.getQuantity() < quantity) {
            throw new IllegalArgumentException("재고가 부족합니다.");
        }

        stock.setQuantity(stock.getQuantity() - quantity);
    }

    @Transactional
    public void increaseStock(Long productId, Integer quantity) {
        ProductStock stock = productStockRepository.findByProduct_ProductId(productId)
                .orElseThrow(() -> new ProductStockNotFoundException("해당 상품의 재고 정보가 없습니다."));

        stock.setQuantity(stock.getQuantity() + quantity);
    }
}