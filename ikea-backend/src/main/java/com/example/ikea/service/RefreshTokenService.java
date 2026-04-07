package com.example.ikea.service;

import com.example.ikea.domain.RefreshToken;
import com.example.ikea.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    // Refresh Token 저장
    public void saveRefreshToken(String loginId, String refreshToken, String memberRole) {
        // 기존 토큰 있으면 삭제 후 새로 저장
        refreshTokenRepository.deleteByLoginId(loginId);
        refreshTokenRepository.flush();
        refreshTokenRepository.save(RefreshToken.builder()
                        .loginId(loginId)
                        .refreshToken(refreshToken)
                        .memberRole(memberRole)
                        .build());
    }

    //Refresh Token 검증
    public void validateRefreshToken(String loginId, String refreshToken) {
        RefreshToken saved = refreshTokenRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("로그인이 필요합니다"));
        if (!saved.getRefreshToken().equals(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }
    }

    //memberRole 조회
    public String getMemberRole(String loginId) {
        // loginId로 Member 조회해서 memberRole 반환
        return refreshTokenRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("로그인이 필요합니다."))
                .getMemberRole();

    }

    // Refresh Token 삭제 (로그아웃)
    public void deleteRefreshToken(String loginId) {
        refreshTokenRepository.deleteByLoginId(loginId);
    }

}
