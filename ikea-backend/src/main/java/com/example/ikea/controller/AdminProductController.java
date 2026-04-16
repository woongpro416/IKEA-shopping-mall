package com.example.ikea.controller;

import com.example.ikea.dto.ProductRequestDto;
import com.example.ikea.dto.ProductResponseDto;
import com.example.ikea.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/product")
public class AdminProductController {

    private final ProductService productService;

    //상품 등록
    @PostMapping
    public ResponseEntity<Long> createProduct(
            @RequestPart @Valid ProductRequestDto dto,
            @RequestPart List<MultipartFile> imgFile)
        throws IOException {
        return ResponseEntity.ok(productService.createProduct(dto, imgFile));
    }

    //상품 수정
    @PutMapping("/{productId}")
    public ResponseEntity<ProductResponseDto> updateProduct(
            @PathVariable Long productId, @RequestPart @Valid ProductRequestDto dto,
            @RequestPart(required = false) List<MultipartFile> imgFile) throws IOException {
        return ResponseEntity.ok(productService.updateProduct(productId, dto, imgFile));
    }

    //상품 삭제
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long productId) {
        productService.deleteProduct(productId);
        return ResponseEntity.ok().build();
    }

    //대시보드용 상품 수 조회
    @GetMapping("/count")
    public ResponseEntity<Long> getProductCount() {
        return ResponseEntity.ok(productService.getProductCount());
    }
}
