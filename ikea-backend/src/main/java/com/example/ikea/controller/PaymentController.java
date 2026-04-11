package com.example.ikea.controller;

import com.example.ikea.dto.*;
import com.example.ikea.service.MemberService;
import com.example.ikea.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payment")
public class PaymentController {

    private final PaymentService paymentService;
    private final MemberService memberService;
    
    //토스 결제 확인
    @PostMapping("/confirm/toss")
    public ResponseEntity<PaymentResponseDto> confirmToss(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid TossConfirmRequestDto dto) {
        Long memberId = memberService.getMemberIdByLoginId(userDetails.getUsername());
        return ResponseEntity.ok(paymentService.confirmTossPayment(memberId, dto));
    }

    //토스 결제 준비
    @PostMapping("/toss/ready")
    public ResponseEntity<TossReadyResponseDto> readyToss(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid TossReadyRequestDto dto) {
        Long memberId = memberService.getMemberIdByLoginId(userDetails.getUsername());
        return ResponseEntity.ok(paymentService.tossReady(memberId, dto));
    }

    //비회원 토스 결제 준비
    @PostMapping("/guest/toss/ready")
    public ResponseEntity<TossReadyResponseDto> readyGuestToss(
            @RequestBody @Valid TossReadyRequestDto dto) {
        return ResponseEntity.ok(paymentService.tossReadyForGuest(dto));
    }

    //비회원 토스 결제 확인
    @PostMapping("/guest/confirm/toss")
    public ResponseEntity<PaymentResponseDto> confirmGuestToss(
            @RequestBody @Valid TossConfirmRequestDto dto) {
        return ResponseEntity.ok(paymentService.confirmGuestTossPayment(dto));
    }

    //카카오 결제 준비
    @PostMapping("/kakao/ready")
    public ResponseEntity<KakaoReadyResponseDto> readyKakao(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid KakaoReadyRequestDto dto) {
        Long memberId = memberService.getMemberIdByLoginId(userDetails.getUsername());
        return ResponseEntity.ok(paymentService.kakaoReady(memberId, dto));
    }

    //비회원 카카오 결제 준비
    @PostMapping("/guest/kakao/ready")
    public ResponseEntity<KakaoReadyResponseDto> readyGuestKakao(
            @RequestBody @Valid KakaoReadyRequestDto dto) {
        return ResponseEntity.ok(paymentService.kakaoReadyForGuest(dto));
    }

    //카카오 결제 확인
    @PostMapping("/confirm/kakao")
    public ResponseEntity<PaymentResponseDto> confirmKakao (
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid KakaoConfirmRequestDto dto) {
        Long memberId = memberService.getMemberIdByLoginId(userDetails.getUsername());
        return ResponseEntity.ok(paymentService.confirmKakaoPayment(memberId, dto));
    }

    //비회원 카카오 결제 확인
    @PostMapping("/guest/confirm/kakao")
    public ResponseEntity<PaymentResponseDto> confirmGuestKakao(
            @RequestBody @Valid KakaoConfirmRequestDto dto) {
        return ResponseEntity.ok(paymentService.confirmGuestKakaoPayment(dto));
    }
    
    //결제 취소
    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelPayment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long orderId,
            @RequestParam String reason) {
        Long memberId = memberService.getMemberIdByLoginId(userDetails.getUsername());
        paymentService.cancelPayment(orderId, memberId, reason);
        return ResponseEntity.ok().build();
    }

    
    //내 결제 목록
    @GetMapping("/my")
    public ResponseEntity<List<PaymentResponseDto>> getMyPaymentList(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = memberService.getMemberIdByLoginId(userDetails.getUsername());
        return ResponseEntity.ok(paymentService.getMyPaymentList(memberId));
    }
}
