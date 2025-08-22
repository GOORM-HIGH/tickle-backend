package com.profect.tickle.global.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.profect.tickle.domain.member.dto.response.MemberResponseDto;
import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.global.security.util.properties.TokenProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.Base64;
import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SignInSuccessHandler implements AuthenticationSuccessHandler {

    private final TokenProperties tokenProperties;
    private final MemberRepository memberRepository; // 추가
    private final ObjectMapper objectMapper; // 추가

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        log.info("{}님 로그인 성공하였습니다.", authentication.getName());

        // 사용자 정보 조회
        Member member = memberRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 권한을 꺼내 List<String>으로 반환
        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        // Token에 들어갈 Claim 생성
        Claims claims = Jwts.claims().setSubject(authentication.getName());
        claims.put("authorities", authorities);
        // JWT에 사용자 ID 추가
        claims.put("userId", member.getId());
        claims.put("nickname", member.getNickname());

        // Base64 인코딩된 키를 디코딩하여 SecretKey 생성
        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(tokenProperties.getSecretKey()));

        // JWT 토큰 생성
        String token = Jwts.builder()
                .setClaims(claims) // 바디
                .setExpiration(new Date(System.currentTimeMillis() + tokenProperties.getExpirationTime())) // 만료시간
                .signWith(key, SignatureAlgorithm.HS512) // 서명
                .compact();

        // 여기서 토큰을 response에 넣거나 헤더로 전달하는 로직 추가
        response.setHeader("Authorization", "Bearer " + token);

        // 응답에 사용자 정보 포함
        response.setContentType("application/json;charset=UTF-8");
        
        // 로그인 응답 DTO 생성
        LoginResponseDto loginResponse = LoginResponseDto.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(tokenProperties.getExpirationTime())
                .user(MemberResponseDto.builder()
                        .id(member.getId())
                        .email(member.getEmail())
                        .nickname(member.getNickname())
                        .memberRole(member.getMemberRole())
                        .pointBalance(member.getPointBalance())
                        .createdAt(member.getCreatedAt())
                        .build())
                .build();

        // JSON으로 응답
        response.getWriter().write(objectMapper.writeValueAsString(loginResponse));
        response.getWriter().flush();
    }

            // 로그인 응답 DTO
    @lombok.Builder
    @lombok.Getter
    public static class LoginResponseDto {
        private String accessToken;
        private String tokenType;
        private long expiresIn;
        private MemberResponseDto user;
    }
}
