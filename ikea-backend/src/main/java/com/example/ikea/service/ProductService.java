package com.example.ikea.service;


import com.example.ikea.domain.Category;
import com.example.ikea.domain.Product;
import com.example.ikea.domain.ProductStock;
import com.example.ikea.dto.ProductRequestDto;
import com.example.ikea.dto.ProductResponseDto;
import com.example.ikea.repository.CategoryRepository;
import com.example.ikea.repository.ProductRepository;
import com.example.ikea.repository.ProductStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductStockRepository productStockRepository;

    //상품 목록 조회
    public List<ProductResponseDto> getProductList() {
        return productRepository.findAll()
                .stream()
                .map(ProductResponseDto::new)
                .collect(Collectors.toList());
    }

    //카테고리별 상품 목록
    public List<ProductResponseDto> getProductListByCategory(Long categoryId) {
        return productRepository.findByCategory_Id(categoryId)
                .stream()
                .map(ProductResponseDto::new)
                .collect(Collectors.toList());
    }

    //상품 상세 조회
    public ProductResponseDto getDetailProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 상품입니다."));
        return new ProductResponseDto(product);
    }

    //상품 검색
    public List<ProductResponseDto> searchProduct(String keyword) {
        return productRepository.findByNameContaining(keyword)
                .stream()
                .map(ProductResponseDto::new)
                .collect(Collectors.toList());
    }

    // 신상품 4건
    public List<ProductResponseDto> getNewProducts() {
        return productRepository.findTop4ByOrderByCreatedAtDesc()
                .stream()
                .map(ProductResponseDto::new)
                .collect(Collectors.toList());
    }

    // 베스트 4건
    public List<ProductResponseDto> getBestProducts() {
        Pageable pageable = PageRequest.of(0, 4);
        return productRepository.findTop4ByBestProducts(pageable)
                .stream()
                .map(ProductResponseDto::new)
                .collect(Collectors.toList());
    }

    // 추천3건 - 특정 카테고리 기준
    public List<ProductResponseDto> getRecommendProducts(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 카테고리입니다."));
        return productRepository.findTop3ByCategoryOrderByCreatedAtDesc(category)
                .stream()
                .map(ProductResponseDto::new)
                .collect(Collectors.toList());
    }



    // ===================관리자 권한 ====================

    //상품 등록
    @Transactional
    public Long createProduct(ProductRequestDto dto, List<MultipartFile> imgFile) throws IOException {
        String imgPath = saveImage(imgFile);

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));

        Product product = Product.builder()
                .name(dto.getName())
                .price(dto.getPrice())
                .originalPrice(dto.getOriginalPrice())
                .imgPath(imgPath)
                .category(category)
                .brand(dto.getBrand())
                .badge(dto.getBadge())
                .label(dto.getLabel())
                .typeSlug(dto.getTypeSlug())
                .attributes(dto.getAttributes())
                .detailContent(dto.getDetailContent())
                .galleryImages(dto.getGalleryImages())
                .dimensionImagePath(dto.getDimensionImagePath())
                .build();

        Product savedProduct = productRepository.save(product);

        ProductStock stock = ProductStock.builder()
                .product(savedProduct)
                .quantity(0)
                .build();

        productStockRepository.save(stock);

        return savedProduct.getProductId();
    }

    //상품 수정
    @Transactional
    public ProductResponseDto updateProduct(Long productId, ProductRequestDto dto,
                                            List<MultipartFile> imgFile) throws IOException {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 상품입니다."));
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 카테고리입니다."));

        if (imgFile != null && !imgFile.isEmpty()) {
            deleteImage(product.getImgPath());
            product.setImgPath(saveImage(imgFile));
        }

        product.setName(dto.getName());
        product.setPrice(dto.getPrice());
        product.setOriginalPrice(dto.getOriginalPrice());
        product.setCategory(category);
        product.setBrand(dto.getBrand());
        product.setBadge(dto.getBadge());
        product.setLabel(dto.getLabel());
        product.setTypeSlug(dto.getTypeSlug());
        product.setAttributes(dto.getAttributes());
        product.setDetailContent(dto.getDetailContent());
        product.setGalleryImages(dto.getGalleryImages());
        product.setDimensionImagePath(dto.getDimensionImagePath());

        return new ProductResponseDto(product);
    }

    //상품 삭제
    @Transactional
    public void deleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                        .orElseThrow(() -> new IllegalStateException("존재하지 않는 상품입니다."));

        if(product.getImgPath() != null & !product.getImgPath().isBlank()) {
            deleteImage(product.getImgPath());
        }

        productStockRepository.deleteByProduct_ProductId(productId);
        productRepository.deleteById(productId);

    }

    //대시보드용 상품 수
    public Long getProductCount() {
        return productRepository.count();
    }

    // ====================이미지 처리 ===================

    @Value("${file.upload-dir}")
    private String uploadDir;

    // 이미지 저장
    private String saveImage(List<MultipartFile> imgFile) throws IOException {

        List<String> imgPaths = new ArrayList<>();

        if (imgFile == null || imgFile.isEmpty()) {
            throw new IllegalArgumentException("최소 1개의 이미지가 필요합니다.");
        }

        File dir = new File(uploadDir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("업로드 폴더 생성에 실패했습니다.");
        }

        for (MultipartFile file : imgFile) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            String originalName = file.getOriginalFilename();
            String safeName = (originalName == null || originalName.isBlank()) ? "image" : originalName;
            String fileName = UUID.randomUUID() + "_" + safeName;
            File destFile = new File(uploadDir, fileName);
            file.transferTo(destFile);
            imgPaths.add("/uploads/products/" + fileName);
        }

        if (imgPaths.isEmpty()) {
            throw new IllegalArgumentException("유효한 이미지가 없습니다.");
        }

        return String.join(",", imgPaths);
    }

    // 이미지 삭제
    private void deleteImage(String imgPath) {
        if (imgPath == null || imgPath.isBlank()) return;

        String[] paths = imgPath.split(",");

        for (String path : paths) {
            String filePath = uploadDir + new File(path.trim()).getName();
            File file = new File(filePath);
            if (file.exists()) file.delete();
        }
    }
}
