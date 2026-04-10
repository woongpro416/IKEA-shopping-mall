package com.example.ikea.repository;

import com.example.ikea.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByProduct_ProductIdOrderByCreatedAtDesc(Long productId);

    boolean existsByMember_MemberIdAndOrder_OrderIdAndProduct_ProductId(Long memberId, Long orderId, Long productId);

    List<Review> findByMember_MemberIdOrderByCreatedAtDesc(Long memberId);
}