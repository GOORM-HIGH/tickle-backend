package com.profect.tickle.domain.chat.controller;

import com.profect.tickle.domain.chat.dto.websocket.WebSocketMessageRequestDto;
import com.profect.tickle.domain.chat.dto.websocket.WebSocketMessageResponseDto;
import com.profect.tickle.domain.chat.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import com.profect.tickle.global.security.util.JwtUtil;

/**
 * STOMP 프로토콜 기반 채팅 메시지 컨트롤러
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class StompChatController {

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate; // STOMP 메시지 전송용
    private final com.profect.tickle.domain.member.repository.MemberRepository memberRepository; // 🎯 추가
    private final JwtUtil jwtUtil; // 🎯 추가

    /**
     * 채팅방 참여 처리
     * 클라이언트에서 /app/chat.join으로 메시지 전송 시 호출
     */
    @MessageMapping("/chat.join")
    public void handleJoin(
            @Payload WebSocketMessageRequestDto message,
            SimpMessageHeaderAccessor headerAccessor) {

        log.info("🚪 사용자 채팅방 참여 요청: {} -> 채팅방 {}",
                message.getSenderNickname(), message.getChatRoomId());

        // 🎯 JWT 토큰에서 실제 사용자 ID 추출
        Long actualSenderId = extractUserIdFromToken(headerAccessor);
        log.info("🎯 JOIN - JWT에서 추출한 실제 사용자 ID: {}", actualSenderId);

        // 🎯 실제 사용자 정보 조회
        var actualMember = memberRepository.findById(actualSenderId);
        String actualNickname = actualMember.isPresent() ? actualMember.get().getNickname() : "알 수 없음";
        
        log.info("🎯 JOIN - 실제 사용자 정보: memberId={}, nickname={}", actualSenderId, actualNickname);

        // 세션에 실제 사용자 정보 저장
        headerAccessor.getSessionAttributes().put("username", actualNickname);
        headerAccessor.getSessionAttributes().put("chatRoomId", message.getChatRoomId());

        WebSocketMessageResponseDto response = WebSocketMessageResponseDto.builder()
                .type("USER_JOIN")
                .chatRoomId(message.getChatRoomId())
                .senderId(actualSenderId) // 🎯 실제 사용자 ID 사용
                .senderNickname(actualNickname) // 🎯 실제 사용자 닉네임 사용
                .messageType(com.profect.tickle.domain.chat.entity.ChatMessageType.SYSTEM)
                .content(actualNickname + "님이 채팅방에 참여했습니다.")
                .createdAt(Instant.now())
                .isMyMessage(false)
                .build();

        log.info("🎯 JOIN 메시지 응답 생성: senderId={}, senderNickname={}", 
                actualSenderId, actualNickname);

        // 🎯 채팅방 전체에 브로드캐스트
        messagingTemplate.convertAndSend(
                "/topic/chat/" + message.getChatRoomId(),
                response
        );
    }

    /**
     * 채팅 메시지 처리 (DB 저장 포함)
     */
    @MessageMapping("/chat.message")
    public void handleMessage(@Payload WebSocketMessageRequestDto message, SimpMessageHeaderAccessor headerAccessor) {
        try {
            log.info("💬 채팅 메시지 수신: {} -> {}",
                    message.getSenderNickname(), message.getContent());

            // 🎯 필수 데이터 검증
            if (message.getChatRoomId() == null) {
                log.error("❌ chatRoomId가 null입니다");
                return;
            }

            if (message.getSenderId() == null) {
                log.error("❌ senderId가 null입니다");
                return;
            }

            log.info("🔍 메시지 전송 요청: chatRoomId={}, senderId={}, type={}",
                    message.getChatRoomId(), message.getSenderId(), message.getMessageType());

            // 🎯 JWT 토큰에서 실제 사용자 ID 추출
            Long actualSenderId = extractUserIdFromToken(headerAccessor);
            log.info("🎯 JWT에서 추출한 실제 사용자 ID: {}", actualSenderId);

            // 🎯 DB에 메시지 저장 (실제 사용자 ID 사용)
            var sendRequest = com.profect.tickle.domain.chat.dto.request.ChatMessageSendRequestDto.builder()
                    .messageType(message.getMessageType())
                    .content(message.getContent())
                    .build();

            var savedMessage = chatMessageService.sendMessage(
                    message.getChatRoomId(), 
                    actualSenderId, // 🎯 실제 사용자 ID 사용
                    sendRequest
            );

            log.info("💾 메시지 DB 저장 완료: messageId={}", savedMessage.getId());

            // 🎯 실제 사용자 정보 조회
            var actualMember = memberRepository.findById(actualSenderId);
            String actualNickname = actualMember.isPresent() ? actualMember.get().getNickname() : "알 수 없음";
            
            log.info("🎯 실제 사용자 정보: memberId={}, nickname={}", actualSenderId, actualNickname);

            // 🎯 저장된 메시지로 응답 생성 (실제 닉네임 사용)
            WebSocketMessageResponseDto response = WebSocketMessageResponseDto.builder()
                    .type("MESSAGE")
                    .messageId(savedMessage.getId())
                    .chatRoomId(message.getChatRoomId())
                    .senderId(actualSenderId) // 🎯 실제 사용자 ID 사용
                    .senderNickname(actualNickname) // 🎯 실제 사용자 닉네임 사용
                    .messageType(message.getMessageType())
                    .content(message.getContent())
                    .createdAt(savedMessage.getCreatedAt())
                    .isMyMessage(false) // 기본값, 프론트엔드에서 계산
                    .build();

            log.info("🎯 메시지 응답 생성: senderId={}, senderNickname={}", 
                    actualSenderId, actualNickname);

            // 🎯 채팅방 전체에 브로드캐스트
            messagingTemplate.convertAndSend(
                    "/topic/chat/" + message.getChatRoomId(),
                    response
            );

            log.info("📤 메시지 브로드캐스트 완료: {}", response);

        } catch (Exception e) {
            log.error("❌ 메시지 처리 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 채팅방 나가기 처리
     * 클라이언트에서 /app/chat.leave로 메시지 전송 시 호출
     */
    @MessageMapping("/chat.leave")
    public void handleLeave(@Payload WebSocketMessageRequestDto message, SimpMessageHeaderAccessor headerAccessor) {
        log.info("🚪 사용자 채팅방 나가기 요청: {} -> 채팅방 {}",
                message.getSenderNickname(), message.getChatRoomId());

        // 🎯 JWT 토큰에서 실제 사용자 ID 추출
        Long actualSenderId = extractUserIdFromToken(headerAccessor);
        log.info("🎯 LEAVE - JWT에서 추출한 실제 사용자 ID: {}", actualSenderId);

        // 🎯 실제 사용자 정보 조회
        var actualMember = memberRepository.findById(actualSenderId);
        String actualNickname = actualMember.isPresent() ? actualMember.get().getNickname() : "알 수 없음";
        
        log.info("🎯 LEAVE - 실제 사용자 정보: memberId={}, nickname={}", actualSenderId, actualNickname);

        WebSocketMessageResponseDto response = WebSocketMessageResponseDto.builder()
                .type("USER_LEAVE")
                .chatRoomId(message.getChatRoomId())
                .senderId(actualSenderId) // 🎯 실제 사용자 ID 사용
                .senderNickname(actualNickname) // 🎯 실제 사용자 닉네임 사용
                .messageType(com.profect.tickle.domain.chat.entity.ChatMessageType.SYSTEM)
                .content(actualNickname + "님이 채팅방을 나갔습니다.")
                .createdAt(Instant.now())
                .isMyMessage(false)
                .build();

        log.info("🎯 LEAVE 메시지 응답 생성: senderId={}, senderNickname={}", 
                actualSenderId, actualNickname);

        // 🎯 채팅방 전체에 브로드캐스트
        messagingTemplate.convertAndSend(
                "/topic/chat/" + message.getChatRoomId(),
                response
        );
    }

    /**
     * JWT 토큰에서 사용자 ID 추출 (개선된 버전)
     */
    private Long extractUserIdFromToken(SimpMessageHeaderAccessor headerAccessor) {
        try {
            log.info("🎯 모든 헤더: {}", headerAccessor.toNativeHeaderMap());

            // 🎯 먼저 인터셉터에서 보존된 JWT 토큰 확인
            String token = (String) headerAccessor.getHeader("JWT_TOKEN");

            if (token == null) {
                // 🎯 보존된 토큰이 없으면 네이티브 헤더에서 추출
                token = headerAccessor.getFirstNativeHeader("Authorization");
                if (token != null && token.startsWith("Bearer ")) {
                    token = token.substring(7);
                }
            }

            log.info("🎯 추출된 JWT 토큰: {}", token != null ? token.substring(0, Math.min(50, token.length())) + "..." : "null");

            if (token != null) {
                // 🎯 JWT 토큰 검증
                if (!jwtUtil.validateToken(token)) {
                    log.warn("🎯 유효하지 않은 JWT 토큰");
                    return 1L;
                }

                // 🎯 JWT에서 직접 userId 추출 시도
                try {
                    Long userId = jwtUtil.getUserId(token);
                    log.info("🎯 JwtUtil.getUserId() 결과: {}", userId);
                    
                    if (userId != null) {
                        log.info("🎯 JWT에서 직접 추출한 userId: {}", userId);
                        return userId;
                    } else {
                        log.warn("🎯 JWT에 userId가 없음, 이메일로 조회 시도");
                        // fallback: 이메일로 조회
                        String email = jwtUtil.getEmail(token);
                        log.info("🎯 JWT에서 추출한 이메일: {}", email);
                        return getUserIdByEmail(email);
                    }
                } catch (Exception e) {
                    log.error("🎯 JwtUtil 사용 실패: {}", e.getMessage(), e);
                    // 기존 방식으로 fallback
                    return extractUserIdFromTokenFallback(token);
                }
            } else {
                log.warn("🎯 JWT 토큰을 찾을 수 없습니다");
            }
        } catch (Exception e) {
            log.error("🎯 JWT 토큰에서 사용자 ID 추출 실패: {}", e.getMessage(), e);
        }

        // 기본값 반환 (임시)
        log.warn("🎯 JWT 토큰에서 사용자 ID 추출 실패, 기본값 1 사용");
        return 1L;
    }

    /**
     * 기존 방식으로 JWT에서 사용자 ID 추출 (fallback)
     */
    private Long extractUserIdFromTokenFallback(String token) {
        try {
            // JWT 토큰 파싱
            String[] parts = token.split("\\.");
            log.info("🎯 JWT parts.length: {}", parts.length);

            if (parts.length == 3) {
                String payload = parts[1];
                log.info("🎯 JWT payload (원본): {}", payload);

                // Base64 디코딩 (패딩 추가)
                while (payload.length() % 4 != 0) {
                    payload += "=";
                }
                log.info("🎯 JWT payload (패딩 후): {}", payload);

                String decodedPayload = new String(java.util.Base64.getDecoder().decode(payload));
                log.info("🎯 JWT 페이로드 (디코딩 후): {}", decodedPayload);

                // Jackson을 사용한 안전한 JSON 파싱
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    com.fasterxml.jackson.databind.JsonNode jsonNode = mapper.readTree(decodedPayload);

                    log.info("🎯 JSON 파싱 성공: {}", jsonNode.toString());

                    // 🎯 userId 클레임 확인
                    if (jsonNode.has("userId")) {
                        Long userId = jsonNode.get("userId").asLong();
                        log.info("🎯 JWT에서 직접 추출한 userId: {}", userId);
                        return userId;
                    }

                    // 🎯 이메일로 조회 (기존 방식)
                    if (jsonNode.has("sub")) {
                        String email = jsonNode.get("sub").asText();
                        log.info("🎯 JWT에서 추출한 이메일: {}", email);
                        return getUserIdByEmail(email);
                    } else {
                        log.warn("🎯 JWT 페이로드에 'sub' 필드가 없습니다. 사용 가능한 필드: {}", jsonNode.fieldNames());
                    }
                } catch (Exception jsonException) {
                    log.error("🎯 JSON 파싱 실패: {}", jsonException.getMessage(), jsonException);
                    // 기존 방식으로 fallback
                    if (decodedPayload.contains("\"sub\":")) {
                        String email = extractEmailFromPayload(decodedPayload);
                        log.info("🎯 Fallback - JWT에서 추출한 이메일: {}", email);
                        return getUserIdByEmail(email);
                    }
                }
            } else {
                log.warn("🎯 JWT 토큰 형식이 올바르지 않습니다. parts.length={}", parts.length);
            }
        } catch (Exception e) {
            log.error("🎯 JWT 토큰 파싱 실패: {}", e.getMessage(), e);
        }
        return 1L;
    }

    /**
     * JWT 페이로드에서 이메일 추출
     */
    private String extractEmailFromPayload(String payload) {
        try {
            // 간단한 JSON 파싱
            int subIndex = payload.indexOf("\"sub\":");
            if (subIndex != -1) {
                int startQuote = payload.indexOf("\"", subIndex + 6);
                int endQuote = payload.indexOf("\"", startQuote + 1);
                if (startQuote != -1 && endQuote != -1) {
                    return payload.substring(startQuote + 1, endQuote);
                }
            }
        } catch (Exception e) {
            log.error("이메일 추출 실패: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 이메일로 사용자 ID 조회
     */
    private Long getUserIdByEmail(String email) {
        try {
            log.info("🎯 이메일로 사용자 조회 시작: email={}", email);
            
            // 🎯 MemberRepository를 사용해서 실제 사용자 ID 조회
            var member = memberRepository.findByEmail(email);
            
            if (member.isPresent()) {
                Long memberId = member.get().getId();
                String nickname = member.get().getNickname();
                log.info("🎯 이메일로 사용자 조회 성공: email={}, memberId={}, nickname={}", 
                        email, memberId, nickname);
                return memberId;
            } else {
                log.warn("🎯 이메일로 사용자 조회 실패: email={} - 사용자를 찾을 수 없습니다", email);
                
                // 🎯 디버깅: 전체 사용자 목록 조회
                log.info("🎯 전체 사용자 목록 조회:");
                var allMembers = memberRepository.findAll();
                for (var m : allMembers) {
                    log.info("🎯 사용자: ID={}, email={}, nickname={}", m.getId(), m.getEmail(), m.getNickname());
                }
            }
        } catch (Exception e) {
            log.error("🎯 이메일로 사용자 ID 조회 중 예외 발생: email={}, error={}", email, e.getMessage(), e);
        }
        
        log.warn("🎯 이메일로 사용자 조회 실패, 기본값 1 반환: email={}", email);
        return 1L;
    }

    /**
     * 채팅방 참여자들에게 개별적으로 메시지 전송 (isMyMessage 개별 설정)
     */
    private void sendMessageToAllParticipants(
            WebSocketMessageRequestDto request,
            com.profect.tickle.domain.chat.dto.response.ChatMessageResponseDto savedMessage) {

        // TODO: 실제로는 채팅방 참여자 목록을 조회해야 함
        // 현재는 간단한 브로드캐스트로 구현

        WebSocketMessageResponseDto response = WebSocketMessageResponseDto.builder()
                .type("MESSAGE")
                .messageId(savedMessage.getId())
                .chatRoomId(request.getChatRoomId())
                .senderId(request.getSenderId())
                .senderNickname(request.getSenderNickname())
                .messageType(request.getMessageType())
                .content(request.getContent())
                .createdAt(savedMessage.getCreatedAt())
                .isMyMessage(false) // 기본값, 실제로는 각 사용자별로 계산 필요
                .build();

        // 🎯 채팅방 전체에 브로드캐스트
        messagingTemplate.convertAndSend(
                "/topic/chat/" + request.getChatRoomId(),
                response
        );
    }
}
