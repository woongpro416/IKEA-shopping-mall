package com.example.ikea.dto;

import com.example.ikea.domain.Review;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ReviewResponseDto {

    private Long reviewId;
    private String memberName;
    private String productName;
    private String content;
    private Integer rating;
    private LocalDateTime createdAt;

    public ReviewResponseDto(Review review) {
        this.reviewId = review.getReviewId();
        this.memberName = (review.getMember() == null || review.getMember().isDeleted())
                ? "익명" : review.getMember().getName();
        this.productName = review.getProduct().getName();
        this.content = review.getContent();
        this.rating = review.getRating();
        this.createdAt = review.getCreatedAt();
    }
}
