package com.example.ikea.repository;

import com.example.ikea.domain.Category;
import com.example.ikea.domain.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategory_Id(Long categoryId);

    List<Product> findByNameContaining(String keyword);

    List<Product> findTop4ByOrderByCreatedAtDesc();

    @Query("SELECT p FROM Product p " +
            "JOIN OrderItem oi ON oi.product = p " +
            "GROUP BY p " +
            "ORDER BY SUM(oi.quantity) DESC")
    List<Product> findTop4ByBestProducts(Pageable pageable);

    List<Product> findTop3ByCategoryOrderByCreatedAtDesc(Category category);

    Optional<Product> findByProductCode(String productCode);

    boolean existsByProductCode(String productCode);
}