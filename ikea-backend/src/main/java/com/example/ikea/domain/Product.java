package com.example.ikea.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "products")
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;

    @Column(name = "product_code", unique = true, length = 100)
    private String productCode;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false)
    private Integer price;

    @Column
    private Integer originalPrice;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String imgPath;

    @Column(length = 100)
    private String brand;

    @Column(length = 100)
    private String badge;

    @Column(length = 100)
    private String label;

    @Column(length = 100)
    private String typeSlug;

    @Column(columnDefinition = "TEXT")
    private String attributes;

    @Column(columnDefinition = "TEXT")
    private String detailContent;

    @Column(columnDefinition = "TEXT")
    private String galleryImages;

    @Column(length = 500)
    private String dimensionImagePath;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    private Boolean deleted = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;
}
