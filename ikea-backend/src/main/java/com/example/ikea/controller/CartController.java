package com.example.ikea.controller;

import com.example.ikea.dto.CartItemResponseDto;
import com.example.ikea.dto.CartRequestDto;
import com.example.ikea.dto.GuestCartCreateResponseDto;
import com.example.ikea.service.CartService;
import com.example.ikea.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;
    private final MemberService memberService;

    //장바구니 조회(회원)
    @GetMapping
    public ResponseEntity<List<CartItemResponseDto>> getCartList(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = memberService.getMemberIdByLoginId(userDetails.getUsername());
        return ResponseEntity.ok(cartService.getCartList(memberId));
    }

    //장바구니 담기
    @PostMapping
    public ResponseEntity<Void> addCart(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid CartRequestDto dto) {
        Long memberId = memberService.getMemberIdByLoginId(userDetails.getUsername());
        cartService.addCart(memberId, dto);
        return ResponseEntity.ok().build();
    }

    //수량 수정
    @PatchMapping("/{cartItemId}/quantity")
    public ResponseEntity<Void> updateQuantity(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long cartItemId,
            @RequestParam int quantity) {
        Long memberId = memberService.getMemberIdByLoginId(userDetails.getUsername());
        cartService.updateQuantity(cartItemId, quantity, memberId);
        return ResponseEntity.ok().build();
    }

    //단건 삭제
    @DeleteMapping("/{cartItemId}")
    public ResponseEntity<Void> deleteCartItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long cartItemId) {
        Long memberId = memberService.getMemberIdByLoginId(userDetails.getUsername());
        cartService.deleteCartItem(cartItemId, memberId);
        return ResponseEntity.ok().build();
    }

    //전체 비우기
    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearCart(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = memberService.getMemberIdByLoginId(userDetails.getUsername());
        cartService.clearCart(memberId);
        return ResponseEntity.ok().build();
    }


    // =========== 비회원 전용 ==============

    // 장바구니 조회(비회원 전용)
    @GetMapping("/guest")
    public ResponseEntity<List<CartItemResponseDto>> getGuestCartList(
            @RequestParam String guestCartKey) {
        return ResponseEntity.ok(cartService.getGuestCartList(guestCartKey));
    }

    // 비회원 장바구니 담기
    @PostMapping("/guest")
    public ResponseEntity<GuestCartCreateResponseDto> addGuestCart(
            @RequestParam(required = false) String guestCartKey,
            @RequestBody @Valid CartRequestDto dto) {
        return ResponseEntity.ok(cartService.addGuestCart(guestCartKey, dto));
    }

    // 비회원 수량 수정
    @PatchMapping("/guest/{cartItemId}/quantity")
    public ResponseEntity<Void> updateGuestQuantity(
            @RequestParam String guestCartKey,
            @PathVariable Long cartItemId,
            @RequestParam int quantity) {
        cartService.updateGuestQuantity(guestCartKey, cartItemId, quantity);
        return ResponseEntity.ok().build();
    }

    // 비회원 단건 삭제
    @DeleteMapping("/guest/{cartItemId}")
    public ResponseEntity<Void> deleteGuestCartItem(
            @RequestParam String guestCartKey,
            @PathVariable Long cartItemId) {
        cartService.deleteGuestCartItem(guestCartKey, cartItemId);
        return ResponseEntity.ok().build();
    }

    // 비회원 전체 비우기
    @DeleteMapping("/guest/clear")
    public ResponseEntity<Void> clearGuestCart(@RequestParam String guestCartKey) {
        cartService.clearGuestCart(guestCartKey);
        return ResponseEntity.ok().build();
    }



}
