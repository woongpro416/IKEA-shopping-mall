package com.example.ikea.controller;

import com.example.ikea.dto.ReviewResponseDto;
import com.example.ikea.service.MemberService;
import com.example.ikea.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/review")
public class AdminReviewController {

    private final ReviewService reviewService;
    private final MemberService memberService;

    // 전체 리뷰 조회
    @GetMapping
    public ResponseEntity<List<ReviewResponseDto>> getAllReviewList() {
        return ResponseEntity.ok(reviewService.getAllReviewList());
    }

    // 리뷰 삭제
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long reviewId) {
        Long memberId = memberService.getMemberIdByLoginId(userDetails.getUsername());
        reviewService.deleteReview(memberId, reviewId);
        return ResponseEntity.ok().build();
    }
}
