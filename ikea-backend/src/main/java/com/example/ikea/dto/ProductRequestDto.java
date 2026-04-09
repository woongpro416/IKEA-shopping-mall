package com.example.ikea.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequestDto {

    private String productCode;

    @NotBlank(message = "상품명을 입력해주세요.")
    private String name;

    @NotNull(message = "가격을 입력해주세요.")
    private Integer price;

    @NotNull(message = "카테고리를 입력해주세요.")
    private Long categoryId;

    private Integer originalPrice;

    private String brand;

    private String badge;

    private String label;

    private String typeSlug;

    private String attributes;

    private String detailContent;

    private String galleryImages;

    private String dimensionImagePath;
}