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

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    public List<ProductResponseDto> getProductList() {
        return productRepository.findByDeletedFalse()
                .stream()
                .map(ProductResponseDto::new)
                .collect(Collectors.toList());
    }

    public List<ProductResponseDto> getProductListByCategory(Long categoryId) {
        return productRepository.findByCategory_IdAndDeletedFalse(categoryId)
                .stream()
                .map(ProductResponseDto::new)
                .collect(Collectors.toList());
    }

    public ProductResponseDto getDetailProduct(String productIdentifier) {
        Product product = findProductByIdentifier(productIdentifier);
        return new ProductResponseDto(product);
    }

    public List<ProductResponseDto> searchProduct(String keyword) {
        return productRepository.findByNameContainingAndDeletedFalse(keyword)
                .stream()
                .map(ProductResponseDto::new)
                .collect(Collectors.toList());
    }

    public List<ProductResponseDto> getNewProducts() {
        return productRepository.findTop4ByDeletedFalseOrderByCreatedAtDesc()
                .stream()
                .map(ProductResponseDto::new)
                .collect(Collectors.toList());
    }

    public List<ProductResponseDto> getBestProducts() {
        Pageable pageable = PageRequest.of(0, 4);
        return productRepository.findTop4ByBestProducts(pageable)
                .stream()
                .map(ProductResponseDto::new)
                .collect(Collectors.toList());
    }

    public List<ProductResponseDto> getRecommendProducts(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 카테고리입니다."));
        return productRepository.findTop3ByCategoryAndDeletedFalseOrderByCreatedAtDesc(category)
                .stream()
                .map(ProductResponseDto::new)
                .collect(Collectors.toList());
    }

    public Product findProductEntityByIdentifier(String productIdentifier) {
        return findProductByIdentifier(productIdentifier);
    }

    public Product findProductEntityByRequest(Long productId, String productCode) {
        if (productId != null) {
            return productRepository.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));
        }

        if (productCode != null && !productCode.isBlank()) {
            return productRepository.findByProductCode(productCode.trim())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));
        }

        throw new IllegalArgumentException("상품 식별자가 필요합니다.");
    }

    private Product findProductByIdentifier(String productIdentifier) {
        if (productIdentifier == null || productIdentifier.isBlank()) {
            throw new IllegalArgumentException("상품 식별자가 필요합니다.");
        }

        String trimmed = productIdentifier.trim();

        if (trimmed.matches("\\d+")) {
            return productRepository.findByProductIdAndDeletedFalse(Long.valueOf(trimmed))
                    .orElseGet(() -> productRepository.findByProductCodeAndDeletedFalse(trimmed)
                            .orElseThrow(() -> new IllegalStateException("존재하지 않는 상품입니다.")));
        }

        return productRepository.findByProductCodeAndDeletedFalse(trimmed)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 상품입니다."));
    }

    // =================== 관리자 권한 ====================

    @Transactional
    public Long createProduct(
            ProductRequestDto dto,
            List<MultipartFile> imgFile,
            List<MultipartFile> galleryFiles,
            MultipartFile dimensionFile
    ) throws IOException {
        String imgPath = saveMainImage(imgFile);
        String galleryImages = buildGalleryImagesPayload(saveImages(galleryFiles));
        String dimensionImagePath = saveOptionalImage(dimensionFile);

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));

        String productCode = normalizeProductCode(dto.getProductCode());

        if (productCode != null && productRepository.existsByProductCode(productCode)) {
            throw new IllegalArgumentException("이미 사용 중인 상품 코드입니다.");
        }

        Product product = Product.builder()
                .productCode(productCode)
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
                .galleryImages(galleryImages)
                .dimensionImagePath(dimensionImagePath)
                .build();

        Product savedProduct = productRepository.save(product);

        ProductStock productStock = ProductStock.builder()
                .product(savedProduct)
                .quantity(0)
                .build();

        productStockRepository.save(productStock);

        return savedProduct.getProductId();
    }

    @Transactional
    public ProductResponseDto updateProduct(
            Long productId,
            ProductRequestDto dto,
            List<MultipartFile> imgFile,
            List<MultipartFile> galleryFiles,
            MultipartFile dimensionFile
    ) throws IOException {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 상품입니다."));

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));

        String productCode = normalizeProductCode(dto.getProductCode());
        if (productCode != null
                && !productCode.equals(product.getProductCode())
                && productRepository.existsByProductCode(productCode)) {
            throw new IllegalArgumentException("이미 사용 중인 상품 코드입니다.");
        }

        if (imgFile != null && !imgFile.isEmpty()) {
            deleteImage(product.getImgPath());
            product.setImgPath(saveMainImage(imgFile));
        }

        if (galleryFiles != null && !galleryFiles.isEmpty()) {
            deleteImages(product.getGalleryImages());
            product.setGalleryImages(buildGalleryImagesPayload(saveImages(galleryFiles)));
        }

        if (dimensionFile != null && !dimensionFile.isEmpty()) {
            deleteImage(product.getDimensionImagePath());
            product.setDimensionImagePath(saveOptionalImage(dimensionFile));
        }

        product.setProductCode(productCode);
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
        if (galleryFiles == null || galleryFiles.isEmpty()) {
            product.setGalleryImages(dto.getGalleryImages() != null ? dto.getGalleryImages() : product.getGalleryImages());
        }
        if (dimensionFile == null || dimensionFile.isEmpty()) {
            product.setDimensionImagePath(dto.getDimensionImagePath() != null ? dto.getDimensionImagePath() : product.getDimensionImagePath());
        }

        return new ProductResponseDto(product);
    }

    @Transactional
    public void deleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 상품입니다."));
        productStockRepository.deleteByProduct_ProductId(productId);
        product.setDeleted(true);
    }

    public Long getProductCount() {
        return productRepository.countByDeletedFalse();
    }

    private String normalizeProductCode(String productCode) {
        if (productCode == null || productCode.isBlank()) {
            return null;
        }
        return productCode.trim();
    }

    private String saveMainImage(List<MultipartFile> imgFile) throws IOException {
        if (imgFile == null || imgFile.isEmpty() || imgFile.get(0).isEmpty()) {
            throw new IllegalArgumentException("상품 이미지는 필수입니다.");
        }

        return saveImage(imgFile.get(0));
    }

    private List<String> saveImages(List<MultipartFile> files) throws IOException {
        if (files == null || files.isEmpty()) {
            return List.of();
        }

        List<String> savedPaths = new java.util.ArrayList<>();
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                savedPaths.add(saveImage(file));
            }
        }
        return savedPaths;
    }

    private String saveOptionalImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        return saveImage(file);
    }

    private String saveImage(MultipartFile file) throws IOException {
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String originalFilename = file.getOriginalFilename();
        String savedFilename = UUID.randomUUID() + "_" + originalFilename;

        File dest = new File(dir, savedFilename);
        file.transferTo(dest);

        return "/uploads/products/" + savedFilename;
    }

    private String buildGalleryImagesPayload(List<String> imagePaths) {
        if (imagePaths == null || imagePaths.isEmpty()) {
            return null;
        }

        return imagePaths.stream()
                .map(path -> "\"" + path + "\"")
                .collect(Collectors.joining(",", "[", "]"));
    }

    private void deleteImage(String imgPath) {
        if (imgPath == null || imgPath.isBlank()) {
            return;
        }

        String fileName = imgPath
                .replace("/uploads/products/", "")
                .replace("/uploads/", "");
        File file = new File(uploadDir, fileName);

        if (file.exists()) {
            file.delete();
        }
    }

    private void deleteImages(String galleryImages) {
        if (galleryImages == null || galleryImages.isBlank()) {
            return;
        }

        String trimmed = galleryImages.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            deleteImage(trimmed);
            return;
        }

        String body = trimmed.substring(1, trimmed.length() - 1).trim();
        if (body.isBlank()) {
            return;
        }

        for (String token : body.split(",")) {
            deleteImage(token.replace("\"", "").trim());
        }
    }
}
