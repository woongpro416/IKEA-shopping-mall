package com.example.ikea.controller;

import com.example.ikea.domain.MemberRole;
import com.example.ikea.dto.MemberResponseDto;
import com.example.ikea.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/member")
public class AdminMemberController {

    private final MemberService memberService;

    // 회원 목록
    @GetMapping
    public ResponseEntity<List<MemberResponseDto>> getMemberList() {
        return ResponseEntity.ok(memberService.getMemberList());
    }

    // 회원 검색
    @GetMapping("/search")
    public ResponseEntity<List<MemberResponseDto>> searchByNameContaining(@RequestParam String keyword) {
        return ResponseEntity.ok(memberService.searchMember(keyword));
    }

    // 회원 상세조회
    @GetMapping("/{memberId}")
    public ResponseEntity<MemberResponseDto> getDetailMember(@PathVariable Long memberId) {
        return ResponseEntity.ok(memberService.getAdminMemberDetail(memberId));
    }

    // 회원 강퇴
    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> kickMember(@PathVariable Long memberId) {
        memberService.kickMember(memberId);
        return ResponseEntity.ok().build();
    }

    // 회원 등급 변경
    @PatchMapping("/{memberId}/role")
    public ResponseEntity<Void> changeRole(@PathVariable Long memberId,
                                           @RequestParam MemberRole memberRole) {
        memberService.changeRole(memberId, memberRole);
        return ResponseEntity.ok().build();
    }
}