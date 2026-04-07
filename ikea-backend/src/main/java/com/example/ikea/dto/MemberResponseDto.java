package com.example.ikea.dto;

import com.example.ikea.domain.Member;
import com.example.ikea.domain.MemberRole;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
public class MemberResponseDto {

    private Long memberId;

    private String loginId;

    private String name;

    private String email;

    private String phoneNumber;

    private String zoneCode;

    private String addressMain;

    private String addressDetail;

    private LocalDateTime createdAt;

    private MemberRole memberRole;

    private boolean deleted;


    public MemberResponseDto(Member member) {
        this.memberId = member.getMemberId();
        this.loginId = member.getLoginId();
        this.name = member.getName();
        this.email = member.getEmail();
        this.phoneNumber = member.getPhoneNumber();
        this.zoneCode = member.getZoneCode();
        this.addressMain = member.getAddressMain();
        this.addressDetail = member.getAddressDetail();
        this.createdAt = member.getCreatedAt();
        this.memberRole = member.getMemberRole();
        this.deleted = member.isDeleted();
    }
}
