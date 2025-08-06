package com.profect.tickle.domain.chat.config;

import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.global.security.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatJwtAuthenticationInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final MemberRepository memberRepository; // ✅ 추가

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String requestURI = request.getRequestURI();
        if (!isChatApiPath(requestURI)) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("JWT 토큰이 없습니다: {}", requestURI);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "JWT 토큰이 필요합니다.");
            return false;
        }

        String token = authHeader.substring(7);

        try {
            if (!jwtUtil.validateToken(token)) {
                log.warn("유효하지 않은 JWT 토큰: {}", requestURI);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "유효하지 않은 토큰입니다.");
                return false;
            }

            String email = jwtUtil.getEmail(token); // 이메일 반환

            // ✅ 수정: 이메일로 사용자 ID 조회
            Long memberId = getMemberIdByEmail(email);

            if (memberId == null) {
                log.warn("존재하지 않는 사용자: email={}", email);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "존재하지 않는 사용자입니다.");
                return false;
            }

            request.setAttribute("currentMemberId", memberId);

            log.debug("JWT 인증 성공: email={}, memberId={}, uri={}", email, memberId, requestURI);
            return true;

        } catch (Exception e) {
            log.error("JWT 토큰 처리 중 오류: {}", e.getMessage(), e);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "토큰 처리 중 오류가 발생했습니다.");
            return false;
        }
    }

    // ✅ 추가: 이메일로 사용자 ID 조회 메서드
    private Long getMemberIdByEmail(String email) {
        try {
            // MemberRepository를 주입받아서 사용
            return memberRepository.findByEmail(email)
                    .map(Member::getId)
                    .orElse(null);
        } catch (Exception e) {
            log.error("사용자 ID 조회 중 오류: email={}", email, e);
            return null;
        }
    }


    /**
     * 채팅 관련 API 경로인지 확인
     */
    private boolean isChatApiPath(String requestURI) {
        return requestURI.startsWith("/api/v1/chat") ||
                requestURI.startsWith("/api/v1/files");
        // WebSocket 정보 API는 JWT 불필요하므로 제외
    }
}
