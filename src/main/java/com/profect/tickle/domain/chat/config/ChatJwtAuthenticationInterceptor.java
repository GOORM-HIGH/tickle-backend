package com.profect.tickle.domain.chat.config;

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

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 채팅 관련 API들만 JWT 검증 적용
        String requestURI = request.getRequestURI();
        if (!isChatApiPath(requestURI)) {
            return true; // 채팅 API가 아니면 통과
        }

        // Authorization 헤더에서 토큰 추출
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("JWT 토큰이 없습니다: {}", requestURI);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "JWT 토큰이 필요합니다.");
            return false;
        }

        String token = authHeader.substring(7); // "Bearer " 제거

        // ✅ 변수를 try 블록 밖에서 선언
        String userIdStr = null;

        try {
            // 팀원의 JwtUtil 메서드 사용
            if (!jwtUtil.validateToken(token)) {
                log.warn("유효하지 않은 JWT 토큰: {}", requestURI);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "유효하지 않은 토큰입니다.");
                return false;
            }

            // ✅ 사용자 ID 추출 (String → Long 변환)
            userIdStr = jwtUtil.getUserId(token);
            Long memberId = Long.parseLong(userIdStr);

            // request에 사용자 ID 저장
            request.setAttribute("currentMemberId", memberId);

            log.debug("JWT 인증 성공: memberId={}, uri={}", memberId, requestURI);
            return true;

        } catch (NumberFormatException e) {
            // ✅ 이제 userIdStr 변수에 접근 가능
            log.error("사용자 ID 변환 오류: userIdStr={}, error={}", userIdStr, e.getMessage(), e);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "잘못된 토큰 형식입니다.");
            return false;
        } catch (Exception e) {
            log.error("JWT 토큰 처리 중 오류: {}", e.getMessage(), e);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "토큰 처리 중 오류가 발생했습니다.");
            return false;
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
