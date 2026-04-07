package com.example.ikea.repository;

import com.example.ikea.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    //멤버이름으로 조회(포함된 키워드)
    List<Member> findByNameContaining(String keyword);
    //로그인
    Optional<Member> findByLoginId(String loginId);

    //로그인 아이디 중복 체크
    boolean existsByLoginId(String loginId);
//    //이메일 중복 체크
//    boolean existsByEmail(String email);
//    //전화번호 중복 체크
//    boolean existsByPhoneNumber(String phoneNumber);

   
    // 활성화된 회원 수
    long countByDeletedFalse();

    // 탈퇴한 회원 수
    long countByDeletedTrue();

}
