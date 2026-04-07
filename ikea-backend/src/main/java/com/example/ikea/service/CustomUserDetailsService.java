package com.example.ikea.service;

import com.example.ikea.domain.Member;
import com.example.ikea.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 회원입니다."));

        if (member.isDeleted()) {
            throw new UsernameNotFoundException("탈퇴한 회원입니다.");
        }

        // User는 Spring Security 제공 클래스 (우리 Member Entity와 다름)
        return User.builder()
                .username(member.getLoginId())
                .password(member.getPassword())
                .roles(member.getMemberRole().name())
                .build();
    }
}