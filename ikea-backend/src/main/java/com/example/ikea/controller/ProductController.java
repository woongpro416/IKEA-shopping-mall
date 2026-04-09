package com.example.ikea.controller;

import com.example.ikea.dto.ProductResponseDto;
import com.example.ikea.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/product")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<ProductResponseDto>> getProductList() {
        return ResponseEntity.ok(productService.getProductList());
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProductResponseDto>> getProductListByCategory(
            @PathVariable Long categoryId) {
        return ResponseEntity.ok(productService.getProductListByCategory(categoryId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductResponseDto>> searchProduct(@RequestParam String keyword) {
        return ResponseEntity.ok(productService.searchProduct(keyword));
    }

    @GetMapping("/{productIdentifier}")
    public ResponseEntity<ProductResponseDto> getDetailProduct(@PathVariable String productIdentifier) {
        return ResponseEntity.ok(productService.getDetailProduct(productIdentifier));
    }

    @GetMapping("/new")
    public ResponseEntity<List<ProductResponseDto>> getNewProducts() {
        return ResponseEntity.ok(productService.getNewProducts());
    }

    @GetMapping("/best")
    public ResponseEntity<List<ProductResponseDto>> getBestProducts() {
        return ResponseEntity.ok(productService.getBestProducts());
    }

    @GetMapping("/recommend")
    public ResponseEntity<List<ProductResponseDto>> getRecommendProducts(@RequestParam Long categoryId) {
        return ResponseEntity.ok(productService.getRecommendProducts(categoryId));
    }
}