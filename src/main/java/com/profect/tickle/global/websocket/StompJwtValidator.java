package com.profect.tickle.global.websocket;

import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.global.security.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * STOMP 연결을 위한 JWT 검증 전용 서비스
 * Single Responsibility Principle 적용
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StompJwtValidator {

    private final JwtUtil jwtUtil;
    private final MemberRepository memberRepository;

    /**
     * JWT 토큰 검증 및 사용자 ID 추출
     * @param token JWT 토큰
     * @return 사용자 ID (검증 실패 시 null)
     */
    public Long validateAndExtractUserId(String token) {
        try {
            if (!jwtUtil.validateToken(token)) {
                log.error("STOMP JWT 토큰 검증 실패");
                return null;
            }

            // JWT에서 사용자 ID 추출
            Long userId = jwtUtil.getUserId(token);
            if (userId != null) {
                log.info("STOMP JWT에서 사용자 ID 추출 성공: userId={}", userId);
                return userId;
            }

            // userId가 없으면 이메일로 조회
            String email = jwtUtil.getEmail(token);
            var member = memberRepository.findByEmail(email);
            if (member.isPresent()) {
                userId = member.get().getId();
                log.info("STOMP JWT에서 이메일로 사용자 ID 조회 성공: userId={}", userId);
                return userId;
            }

            log.error("STOMP JWT에서 사용자 정보를 추출할 수 없습니다");
            return null;

        } catch (Exception e) {
            log.error("STOMP JWT 처리 오류: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Authorization 헤더에서 JWT 토큰 추출
     * @param authHeader Authorization 헤더 값
     * @return JWT 토큰 (헤더가 없거나 형식이 잘못된 경우 null)
     */
    public String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            log.info("STOMP CONNECT에서 JWT 토큰 발견: {}...", 
                    token.substring(0, Math.min(50, token.length())));
            return token;
        }
        
        log.warn("STOMP CONNECT에서 Authorization 헤더 없음 또는 잘못된 형식");
        return null;
    }
}
