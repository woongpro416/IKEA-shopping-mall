package com.example.ikea.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;


    // Access Token 만료시간 (30분)
    private final long ACCESS_TOKEN_EXPIRE = 1000 * 60 * 30;

    // Refresh Token 만료시간 (7일)
    private final long REFRESH_TOKEN_EXPIRE = 1000 * 60 * 60 * 24 * 7;

    // 서명용 Key 객체 생성
    private Key getSignKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        System.out.println("JWT Secret 길이: " + keyBytes.length + "바이트 / " + (keyBytes.length * 8) + "비트");
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Access Token 생성
    public String createAccessToken(String loginId, String role) {
        return Jwts.builder()
                .setSubject(loginId)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date((System.currentTimeMillis() + ACCESS_TOKEN_EXPIRE)))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // Refresh Token 생성
    public String createRefreshToken(String loginId) {
        return Jwts.builder()
                .setSubject(loginId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRE))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // 토큰에서 loginId 꺼내기
    public String getLoginId(String token) {
        return getClaims(token).getSubject();
    }

    // 토큰에서 role 꺼내기
    public String getRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    // 토큰 유효성 검증
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            throw  e;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }


    //토큰 파싱
    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }



}
