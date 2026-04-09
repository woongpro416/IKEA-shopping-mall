package com.example.ikea.dto;

import com.example.ikea.domain.Review;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ReviewResponseDto {

    private Long reviewId;
    private Long memberId;
    private String loginId;
    private Long productId;
    private String productCode;
    private String productName;
    private Long orderId;
    private String content;
    private Integer rating;
    private LocalDateTime createdAt;

    public ReviewResponseDto(Review review) {
        this.reviewId = review.getReviewId();
        this.memberId = review.getMember().getMemberId();
        this.loginId = review.getMember().getLoginId();
        this.productId = review.getProduct().getProductId();
        this.productCode = review.getProduct().getProductCode();
        this.productName = review.getProduct().getName();
        this.orderId = review.getOrder() != null ? review.getOrder().getOrderId() : null;
        this.content = review.getContent();
        this.rating = review.getRating();
        this.createdAt = review.getCreatedAt();
    }
}