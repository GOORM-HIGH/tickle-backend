package com.profect.tickle.global.websocket;

import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.global.security.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * STOMP 메시지에서 JWT 토큰 인증 처리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StompJwtChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final MemberRepository memberRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        // CONNECT 명령일 때만 JWT 인증 처리
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                log.info("STOMP CONNECT에서 JWT 토큰 발견: {}", token.substring(0, Math.min(50, token.length())) + "...");
                
                try {
                    // JWT 토큰 검증
                    if (jwtUtil.validateToken(token)) {
                        // JWT에서 사용자 ID 추출
                        Long userId = jwtUtil.getUserId(token);
                        if (userId == null) {
                            // userId가 없으면 이메일로 조회
                            String email = jwtUtil.getEmail(token);
                            var member = memberRepository.findByEmail(email);
                            if (member.isPresent()) {
                                userId = member.get().getId();
                            }
                        }
                        
                        if (userId != null) {
                            // 세션에 사용자 정보 저장
                            final Long finalUserId = userId;
                            accessor.setUser(() -> finalUserId.toString());
                            accessor.setHeader("userId", finalUserId);
                            
                            // 메시지를 다시 래핑하여 헤더 변경사항을 반영
                            MessageHeaderAccessor.getMutableAccessor(message);
                            
                            log.info("STOMP JWT 인증 성공: userId={}", finalUserId);
                        } else {
                            log.error("STOMP JWT에서 사용자 정보를 추출할 수 없습니다");
                            return null; // 연결 거부
                        }
                    } else {
                        log.error("STOMP JWT 토큰 검증 실패");
                        return null; // 연결 거부
                    }

                } catch (Exception e) {
                    log.error("STOMP JWT 처리 오류: {}", e.getMessage(), e);
                    return null; // 연결 거부
                }
            } else {
                log.warn("STOMP CONNECT에서 Authorization 헤더 없음");
                // 개발 환경에서는 허용, 운영에서는 거부
                // return null;
            }
        }

        return message;
    }
}
