package com.example.ikea.dto;

import com.example.ikea.domain.Product;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ProductResponseDto {

    private Long productId;
    private String name;
    private Integer price;
    private Integer originalPrice;
    private String imgPath;
    private String categoryName;
    private String brand;
    private String badge;
    private String label;
    private String typeSlug;
    private String attributes;
    private String detailContent;
    private String galleryImages;
    private String dimensionImagePath;
    private LocalDateTime createdAt;

    public ProductResponseDto(Product product) {
        this.productId = product.getProductId();
        this.name = product.getName();
        this.price = product.getPrice();
        this.originalPrice = product.getOriginalPrice();
        this.imgPath = product.getImgPath();
        this.categoryName = product.getCategory() != null ? product.getCategory().getName() : null;
        this.brand = product.getBrand();
        this.badge = product.getBadge();
        this.label = product.getLabel();
        this.typeSlug = product.getTypeSlug();
        this.attributes = product.getAttributes();
        this.detailContent = product.getDetailContent();
        this.galleryImages = product.getGalleryImages();
        this.dimensionImagePath = product.getDimensionImagePath();
        this.createdAt = product.getCreatedAt();
    }
}