package com.example.ikea.domain;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "members")
@Builder
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long memberId;      //회원 PK

    @Column(nullable = false, length = 50)
    private String loginId;     //로그인 아이디

    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, unique = true, length = 254)
    private String email;       //이메일 주소

    @Column(nullable = false, unique = true)
    private String phoneNumber;

    @Column(length = 10)
    private String zoneCode;        // 우편 번호

    @Column(length = 255)
    private String addressMain;     // 기본주소

    @Column(length = 255)
    private String addressDetail;   // 상세주소

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private MemberRole memberRole;

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;


}
