package com.example.ikea.controller;

import com.example.ikea.dto.ReviewRequestDto;
import com.example.ikea.dto.ReviewResponseDto;
import com.example.ikea.service.MemberService;
import com.example.ikea.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/review")
public class ReviewController {

    private final ReviewService reviewService;
    private final MemberService memberService;

    //상품별 리뷰 목록 조회
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ReviewResponseDto>> getReviewList(
            @PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.getReviewList(productId));
    }

    // 내 리뷰 목록
    @GetMapping("/my")
    public ResponseEntity<List<ReviewResponseDto>> getMyReviewList(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = memberService.getMemberIdByLoginId(userDetails.getUsername());
        return ResponseEntity.ok(reviewService.getMyReviewList(memberId));
    }
    
    //상품 리뷰 생성
    @PostMapping
    public ResponseEntity<Long> createReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid ReviewRequestDto dto) {
        Long memberId = memberService.getMemberIdByLoginId(userDetails.getUsername());
        return ResponseEntity.ok(reviewService.createReview(memberId, dto));
    }
    
    //리뷰 수정
    @PutMapping("/{reviewId}")
    public ResponseEntity<Void> updateReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long reviewId,
            @RequestBody @Valid ReviewRequestDto dto) {
        Long memberId = memberService.getMemberIdByLoginId(userDetails.getUsername());
        reviewService.updateReview(reviewId, memberId, dto);
        return ResponseEntity.ok().build();
    }
    
    //리뷰 삭제
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long reviewId) {
        Long memberId = memberService.getMemberIdByLoginId(userDetails.getUsername());
        reviewService.deleteReview(memberId, reviewId);
        return ResponseEntity.ok().build();
    }

}
