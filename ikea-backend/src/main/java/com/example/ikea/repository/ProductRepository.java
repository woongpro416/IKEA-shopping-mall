package com.example.ikea.repository;

import com.example.ikea.domain.Category;
import com.example.ikea.domain.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByDeletedFalse();

    List<Product> findByCategory_IdAndDeletedFalse(Long categoryId);

    List<Product> findByNameContainingAndDeletedFalse(String keyword);

    List<Product> findTop4ByDeletedFalseOrderByCreatedAtDesc();

    @Query("SELECT p FROM Product p " +
            "JOIN OrderItem oi ON oi.product = p " +
            "WHERE p.deleted = false " +
            "GROUP BY p " +
            "ORDER BY SUM(oi.quantity) DESC")
    List<Product> findTop4ByBestProducts(Pageable pageable);

    List<Product> findTop3ByCategoryAndDeletedFalseOrderByCreatedAtDesc(Category category);

    Optional<Product> findByProductCode(String productCode);

    Optional<Product> findByProductCodeAndDeletedFalse(String productCode);

    Optional<Product> findByProductIdAndDeletedFalse(Long productId);

    boolean existsByProductCode(String productCode);

    long countByDeletedFalse();
}
