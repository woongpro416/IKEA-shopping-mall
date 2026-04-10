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

    @GetMapping("/product/{productIdentifier}")
    public ResponseEntity<List<ReviewResponseDto>> getReviewListByProduct(
            @PathVariable String productIdentifier) {
        return ResponseEntity.ok(reviewService.getReviewListByProduct(productIdentifier));
    }

    @PostMapping
    public ResponseEntity<Long> createReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid ReviewRequestDto dto) {
        Long memberId = memberService.getMemberIdByLoginId(userDetails.getUsername());
        return ResponseEntity.ok(reviewService.createReview(memberId, dto));
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<Void> updateReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid ReviewRequestDto dto) {
        Long memberId = memberService.getMemberIdByLoginId(userDetails.getUsername());
        reviewService.updateReview(reviewId, memberId, dto);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = memberService.getMemberIdByLoginId(userDetails.getUsername());
        reviewService.deleteReview(reviewId, memberId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/my")
    public ResponseEntity<List<ReviewResponseDto>> getMyReviews(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = memberService.getMemberIdByLoginId(userDetails.getUsername());
        return ResponseEntity.ok(reviewService.getMyReviewList(memberId));
    }
}