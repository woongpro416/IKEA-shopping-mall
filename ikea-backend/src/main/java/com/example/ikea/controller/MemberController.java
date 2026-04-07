package com.example.ikea.controller;

import com.example.ikea.dto.*;
import com.example.ikea.security.JwtTokenProvider;
import com.example.ikea.service.MemberService;
import com.example.ikea.service.RefreshTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member")
public class MemberController {

    private final MemberService memberService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    //회원가입
    @PostMapping("/join")
    public ResponseEntity<Long> join(@RequestBody @Valid MemberJoinRequestDto request) {
        Long memberId = memberService.join(request);
        return ResponseEntity.ok(memberId);
    }

    //로그인
    @PostMapping("/login")
    public ResponseEntity<TokenResponseDto> login(@RequestBody MemberLoginRequestDto request) {
        MemberResponseDto member = memberService.login(request);

        String accessToken = jwtTokenProvider.createAccessToken(
                member.getLoginId(), member.getMemberRole().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getLoginId());

        refreshTokenService.saveRefreshToken(
                member.getLoginId(),
                refreshToken,
                member.getMemberRole().name());

        return ResponseEntity.ok(new TokenResponseDto(accessToken, refreshToken));
    }

    // 내 정보 조회
    @GetMapping("/me")
    public ResponseEntity<MemberResponseDto> getMe(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = memberService.getMemberIdByLoginId(userDetails.getUsername());
        return ResponseEntity.ok(memberService.detailMember(memberId));
    }

    //회원 정보 수정
    @PutMapping("/me")
    public ResponseEntity<MemberResponseDto> update(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody MemberUpdateDto dto) {
        return ResponseEntity.ok(memberService.update(dto, userDetails.getUsername()));
    }

    // 회원탈퇴
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMember(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = memberService.getMemberIdByLoginId(userDetails.getUsername());
        memberService.deleteMember(memberId);
        return ResponseEntity.ok().build();
    }
}
