package com.example.ikea.repository;

import com.example.ikea.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    //상품 리뷰 목록
    List<Review> findByProduct_ProductId(Long productId);

    //내  리뷰 목록
    List<Review> findByMember_MemberId(Long memberId);

    //한 제품에 대한 리뷰 중복 방지
    boolean existsByOrder_OrderIdAndProduct_ProductId(Long orderId, Long productId);

    List<Review> findByProduct_ProductIdOrderByCreatedAtDesc(Long productId);
}
