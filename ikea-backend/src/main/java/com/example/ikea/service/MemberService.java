package com.example.ikea.service;

import com.example.ikea.domain.*;
import com.example.ikea.dto.MemberJoinRequestDto;
import com.example.ikea.dto.MemberLoginRequestDto;
import com.example.ikea.dto.MemberResponseDto;
import com.example.ikea.dto.MemberUpdateDto;
import com.example.ikea.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final CartItemRepository cartItemRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final QnaRepository qnaRepository;

    // 로그인
    public MemberResponseDto login(MemberLoginRequestDto request) {
        Member member = memberRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다."));

        if (member.isDeleted()) {
            throw new IllegalArgumentException("탈퇴한 회원은 로그인할 수 없습니다.");
        }

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다.");
        }

        return new MemberResponseDto(member);
    }

    // 회원가입
    @Transactional
    public Long join(MemberJoinRequestDto request) {
        if (memberRepository.existsByLoginId(request.getLoginId())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        if (!request.isPasswordMatch()) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        Member member = request.toEntity();
        member.setPassword(passwordEncoder.encode(request.getPassword()));
        memberRepository.save(member);

        Cart cart = Cart.builder()
                .member(member)
                .build();
        cartRepository.save(cart);

        return member.getMemberId();
    }

    // 내 정보 조회
    public MemberResponseDto getMemberByLoginId(String loginId) {
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        if (member.isDeleted()) {
            throw new IllegalArgumentException("탈퇴한 회원입니다.");
        }

        return new MemberResponseDto(member);
    }

    // 마이페이지 상세 조회
    public MemberResponseDto detailMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        if (member.isDeleted()) {
            throw new IllegalArgumentException("탈퇴한 회원입니다.");
        }

        return new MemberResponseDto(member);
    }

    // 회원 정보 수정
    @Transactional
    public MemberResponseDto update(MemberUpdateDto dto, String loginId) {
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        if (member.isDeleted()) {
            throw new IllegalArgumentException("탈퇴한 회원은 정보를 수정할 수 없습니다.");
        }

        member.setName(dto.getName());
        member.setEmail(dto.getEmail());
        member.setPhoneNumber(dto.getPhoneNumber());
        member.setZoneCode(dto.getZoneCode());
        member.setAddressMain(dto.getAddressMain());
        member.setAddressDetail(dto.getAddressDetail());

        return new MemberResponseDto(member);
    }

    // 회원 탈퇴(본인)
    @Transactional
    public void deleteMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        if (member.isDeleted()) {
            throw new IllegalArgumentException("이미 탈퇴한 회원입니다.");
        }

        validateWithdrawable(member.getMemberId());
        softDeleteMember(member);
    }

    // ================= 관리자 기능 =================

    // 회원 목록
    public List<MemberResponseDto> getMemberList() {
        return memberRepository.findAll()
                .stream()
                .map(MemberResponseDto::new)
                .collect(Collectors.toList());
    }

    // 회원 검색
    public List<MemberResponseDto> searchMember(String keyword) {
        return memberRepository.findByNameContaining(keyword)
                .stream()
                .map(MemberResponseDto::new)
                .collect(Collectors.toList());
    }

    // 회원 강퇴(soft delete)
    @Transactional
    public void kickMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        if (member.isDeleted()) {
            throw new IllegalArgumentException("이미 탈퇴 처리된 회원입니다.");
        }

        validateWithdrawable(member.getMemberId());
        softDeleteMember(member);
    }

    // 회원 권한 변경
    @Transactional
    public void changeRole(Long memberId, MemberRole memberRole) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        if (member.isDeleted()) {
            throw new IllegalArgumentException("탈퇴한 회원의 권한은 변경할 수 없습니다.");
        }

        member.setMemberRole(memberRole);
    }

    // 회원 상세 조회(관리자용)
    public MemberResponseDto getAdminMemberDetail(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        return new MemberResponseDto(member);
    }

    // 대시보드용 회원 수
    public long getMemberCount() {
        return memberRepository.count();
    }
    // 활성화된 회원 수
    public long getActiveMemberCount() {
        return memberRepository.countByDeletedFalse();
    }
    // 탈퇴한 회원 수
    public long getDeletedMemberCount() {
        return memberRepository.countByDeletedTrue();
    }

    // JWT 방식 회원 조회
    public Long getMemberIdByLoginId(String loginId) {
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 회원입니다."));

        if (member.isDeleted()) {
            throw new IllegalStateException("탈퇴한 회원입니다.");
        }

        return member.getMemberId();
    }

    // soft delete 처리
    private void softDeleteMember(Member member) {
        cartRepository.findByMember_MemberId(member.getMemberId()).ifPresent(cart -> {
            cartItemRepository.deleteByCart_CartId(cart.getCartId());
            cartRepository.delete(cart);
        });

        refreshTokenRepository.deleteByLoginId(member.getLoginId());

        List<Qna> qnas = qnaRepository.findByMemberId(member.getMemberId());
        qnaRepository.deleteAll(qnas);

        member.setDeleted(true);
    }

    // 탈퇴 가능 여부 검증
    private void validateWithdrawable(Long memberId) {
        List<OrderStatus> blockedStatuses = List.of(
                OrderStatus.PENDING,
                OrderStatus.PAID,
                OrderStatus.ORDERED,
                OrderStatus.DELIVERING
        );

        if (orderRepository.existsByMember_MemberIdAndOrderStatusIn(memberId, blockedStatuses)) {
            throw new IllegalArgumentException("진행 중인 주문이 있어 탈퇴할 수 없습니다.");
        }

        if (paymentRepository.existsByMember_MemberIdAndPaymentStatus(memberId, PaymentStatus.OK)) {
            throw new IllegalArgumentException("취소되지 않은 결제가 있어 탈퇴할 수 없습니다.");
        }
    }
}