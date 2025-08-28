package com.profect.tickle.domain.chat.service;

import com.profect.tickle.domain.chat.dto.common.PaginationDto;
import com.profect.tickle.domain.chat.dto.request.ChatMessageSendRequestDto;
import com.profect.tickle.domain.chat.dto.response.ChatMessageFileDownloadDto;
import com.profect.tickle.domain.chat.dto.response.ChatMessageListResponseDto;
import com.profect.tickle.domain.chat.dto.response.ChatMessageResponseDto;
import com.profect.tickle.domain.chat.dto.websocket.WebSocketMessageResponseDto;
import com.profect.tickle.domain.chat.entity.Chat;
import com.profect.tickle.domain.chat.entity.ChatMessageType;
import com.profect.tickle.domain.chat.entity.ChatRoom;
import com.profect.tickle.domain.chat.mapper.ChatMessageMapper;
import com.profect.tickle.domain.chat.repository.ChatParticipantsRepository;
import com.profect.tickle.domain.chat.repository.ChatRepository;
import com.profect.tickle.domain.chat.repository.ChatRoomRepository;
import com.profect.tickle.domain.file.service.FileService;
import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ChatMessageService {

    private final ChatParticipantsRepository chatParticipantsRepository;
    private final ChatRepository chatRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final MemberRepository memberRepository;
    private final ChatMessageMapper chatMessageMapper; // MyBatis
    private final FileService fileService;
    private final SimpMessagingTemplate simpMessagingTemplate; // WebSocket 템플릿
    private final ChatMessageValidator chatMessageValidator; // 메시지 검증 전용

    /**
     * 메시지 전송 (JPA 사용)
     */
    @Transactional
    public ChatMessageResponseDto sendMessage(Long chatRoomId, Long senderId, ChatMessageSendRequestDto requestDto) {
        log.info("메시지 전송 요청: chatRoomId={}, senderId={}, type={}",
                chatRoomId, senderId, requestDto.getMessageType());

        // 1. 채팅방 존재 및 활성 상태 확인
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        if (!chatRoom.isActive()) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_INACTIVE);
        }

        // 2. 발신자 존재 확인
        Member sender = memberRepository.findById(senderId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // 3. 채팅방 참여 여부 확인
        boolean isParticipant = chatParticipantsRepository.existsByChatRoomAndMemberAndStatusTrue(chatRoom, sender);
        if (!isParticipant) {
            throw new BusinessException(ErrorCode.CHAT_NOT_PARTICIPANT);
        }

        // 4. 메시지 검증 (SRP 준수: 검증 로직을 별도 클래스로 분리)
        chatMessageValidator.validateMessage(requestDto);

        // 5. 메시지 엔티티 생성
        Chat message = Chat.builder()
                .chatRoomId(chatRoomId)
                .member(sender)
                .messageType(requestDto.getMessageType())
                .content(requestDto.getContent())
                .filePath(requestDto.getFilePath())
                .fileName(requestDto.getFileName())
                .fileSize(requestDto.getFileSize())
                .fileType(requestDto.getFileType())
                .createdAt(Instant.now())
                .senderStatus(true)
                .isDeleted(false)
                .isEdited(false)
                .build();

        // 6. 메시지 저장
        Chat savedMessage = chatRepository.save(message);
        log.info("메시지 저장 완료: messageId={}", savedMessage.getId());

        // 7. 채팅방 업데이트 시간 갱신
        updateChatRoomTimestamp(chatRoom);

        // 8. DTO 변환 및 반환 (개선된 Factory 패턴 사용)
        ChatMessageResponseDto.ChatMessageContext context = 
                new ChatMessageResponseDto.ChatMessageContext(savedMessage, sender.getNickname(), true);
        ChatMessageResponseDto response = ChatMessageResponseDto.fromContext(context);

        log.info("메시지 전송 완료: messageId={}, senderId={}, senderNickname={}, actualNickname={}", 
                savedMessage.getId(), savedMessage.getMember().getId(), 
                response.getSenderNickname(), sender.getNickname());

        return response;
    }

    /**
     * 메시지 목록 조회 (MyBatis 사용 - 페이징)
     */
    public ChatMessageListResponseDto getMessages(Long chatRoomId, Long currentMemberId,
                                                  int page, int size, Long lastMessageId) {
        log.info("메시지 목록 조회: chatRoomId={}, memberId={}, page={}, size={}, lastMessageId={}",
                chatRoomId, currentMemberId, page, size, lastMessageId);

        try {
            // 1. 채팅방 존재 확인
            ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

            // 2. 회원 확인
            Member member = memberRepository.findById(currentMemberId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

            // 3. 참여 여부 확인
            boolean isParticipant = chatParticipantsRepository.existsByChatRoomAndMemberAndStatusTrue(chatRoom, member);
            if (!isParticipant) {
                log.warn("사용자가 채팅방에 참여하지 않음: chatRoomId={}, memberId={}", chatRoomId, currentMemberId);
                return ChatMessageListResponseDto.of(List.of(), PaginationDto.of(page, size, 0));
            }

            // 4. MyBatis로 복잡한 메시지 조회 (페이징)
            int offset = page * size;
            List<ChatMessageResponseDto> messages = chatMessageMapper.findMessagesByRoomId(
                    chatRoomId, currentMemberId, offset, size, lastMessageId);

            // 5. 전체 메시지 개수 조회
            int totalMessages = chatMessageMapper.countTotalMessages(chatRoomId, lastMessageId);

            // 6. 페이징 정보 생성
            PaginationDto pagination = PaginationDto.of(page, size, totalMessages);

            log.info("메시지 조회 완료: chatRoomId={}, 조회된 메시지 {} 건, 전체 {} 건", 
                    chatRoomId, messages.size(), totalMessages);

            return ChatMessageListResponseDto.of(messages, pagination);

        } catch (Exception e) {
            log.error("메시지 목록 조회 중 오류 발생: chatRoomId={}, memberId={}, error={}", 
                    chatRoomId, currentMemberId, e.getMessage(), e);
            return ChatMessageListResponseDto.of(List.of(), PaginationDto.of(page, size, 0));
        }
    }

    /**
     * 메시지 수정 (JPA 사용)
     */
    @Transactional
    public ChatMessageResponseDto editMessage(Long messageId, Long editorId, String newContent) {
        log.info("메시지 수정 요청: messageId={}, editorId={}", messageId, editorId);

        // 1. 메시지 존재 확인
        Chat message = chatRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_MESSAGE_NOT_FOUND));

        // 2. 수정 권한 확인 (작성자만 수정 가능)
        if (!message.getMember().getId().equals(editorId)) {
            throw new BusinessException(ErrorCode.CHAT_NOT_MESSAGE_OWNER);
        }

        // 3. 수정 가능 상태 확인
        if (message.getIsDeleted()) {
            throw new BusinessException(ErrorCode.CHAT_MESSAGE_ALREADY_DELETED);
        }

        // 4. 메시지 수정 (더티 체킹)
        message.editContent(newContent);

        log.info("메시지 수정 완료: messageId={}", messageId);

        // 5. DTO 변환 (개선된 Factory 패턴 사용)
        ChatMessageResponseDto.ChatMessageContext context = 
                new ChatMessageResponseDto.ChatMessageContext(message, message.getMember().getNickname(), true);
        return ChatMessageResponseDto.fromContext(context);
    }

    /**
     * 메시지 삭제 (논리 삭제, JPA 사용)
     */
    @Transactional
    public void deleteMessage(Long messageId, Long deleterId) {
        log.info("메시지 삭제 요청: messageId={}, deleterId={}", messageId, deleterId);

        // 1. 메시지 존재 확인
        Chat message = chatRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_MESSAGE_NOT_FOUND));

        // 2. 삭제 권한 확인 (작성자만 삭제 가능)
        if (!message.getMember().getId().equals(deleterId)) {
            throw new BusinessException(ErrorCode.CHAT_NOT_MESSAGE_OWNER);
        }

        // 3. 이미 삭제된 메시지 확인
        if (message.getIsDeleted()) {
            throw new BusinessException(ErrorCode.CHAT_MESSAGE_ALREADY_DELETED);
        }

        // 4. 논리 삭제 (더티 체킹)
        message.markAsDeleted();

        log.info("메시지 삭제 완료: messageId={}", messageId);

        // 5. WebSocket을 통해 삭제 이벤트 전송
        try {
            WebSocketMessageResponseDto deleteEvent = WebSocketMessageResponseDto.builder()
                    .type(WebSocketMessageResponseDto.MessageType.DELETE)
                    .messageId(messageId)
                    .chatRoomId(message.getChatRoomId())
                    .senderId(message.getMember().getId())
                    .senderNickname(message.getMember().getNickname())
                    .build();

            simpMessagingTemplate.convertAndSend("/topic/chat/" + message.getChatRoomId(), deleteEvent);
            log.info("삭제 이벤트 WebSocket 전송 완료: messageId={}, chatRoomId={}", messageId, message.getChatRoomId());
        } catch (Exception e) {
            log.error("삭제 이벤트 WebSocket 전송 실패: messageId={}, error={}", messageId, e.getMessage());
            // WebSocket 전송 실패해도 삭제는 성공으로 처리
        }
    }

    /**
     * 채팅방의 마지막 메시지 조회 (MyBatis 사용)
     */
    public ChatMessageResponseDto getLastMessage(Long chatRoomId, Long currentMemberId) {
        log.info("마지막 메시지 조회: chatRoomId={}, memberId={}", chatRoomId, currentMemberId);

        // 채팅방 존재 여부 확인
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // MyBatis 매퍼 호출
        ChatMessageResponseDto response = chatMessageMapper.findLastMessageByRoomId(chatRoomId, currentMemberId);

        return response;
    }

    /**
     * 읽지않은 메시지 개수 조회 (MyBatis 사용)
     */
    public int getUnreadCount(Long chatRoomId, Long memberId, Long lastReadMessageId) {
        log.info("읽지않은 메시지 개수 조회: chatRoomId={}, memberId={}, lastReadMessageId={}", 
                chatRoomId, memberId, lastReadMessageId);

        try {
            // 1. 채팅방 존재 확인
            ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

            // 2. 회원 확인
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

            // 3. 참여 여부 확인
            boolean isParticipant = chatParticipantsRepository.existsByChatRoomAndMemberAndStatusTrue(chatRoom, member);
            if (!isParticipant) {
                log.warn("사용자가 채팅방에 참여하지 않음: chatRoomId={}, memberId={}", chatRoomId, memberId);
                return 0; // 참여하지 않으면 읽지 않은 메시지 0개
            }

            // 4. MyBatis로 읽지 않은 메시지 개수 조회
            int unreadCount = chatMessageMapper.countUnreadMessages(chatRoomId, memberId, lastReadMessageId);

            log.info("읽지않은 메시지 개수 조회 결과: chatRoomId={}, memberId={}, lastReadMessageId={}, unreadCount={}", 
                    chatRoomId, memberId, lastReadMessageId, unreadCount);

            return unreadCount;

        } catch (Exception e) {
            log.error("읽지않은 메시지 개수 조회 중 오류 발생: chatRoomId={}, memberId={}, error={}", 
                    chatRoomId, memberId, e.getMessage(), e);
            return 0; // 오류 발생 시 0개 반환
        }
    }

    // ===== Private 헬퍼 메서드들 =====

    /**
     * 채팅방 타임스탬프 업데이트
     */
    private void updateChatRoomTimestamp(ChatRoom chatRoom) {
        chatRoom.updateTimestamp();
    }

    /**
     * 메시지 첨부 파일 다운로드용 정보 조회
     */
    public ChatMessageFileDownloadDto getMessageFileForDownload(Long chatRoomId, Long messageId, Long currentMemberId) {
        log.info("메시지 파일 다운로드 정보 조회: messageId={}, memberId={}", messageId, currentMemberId);

        // 1. 채팅방 존재 확인
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // 2. 회원 확인
        Member member = memberRepository.findById(currentMemberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // 3. 참여 여부 확인
        boolean isParticipant = chatParticipantsRepository.existsByChatRoomAndMemberAndStatusTrue(chatRoom, member);
        if (!isParticipant) {
            throw new BusinessException(ErrorCode.CHAT_NOT_PARTICIPANT);
        }

        // 4. 메시지 존재 확인
        Chat message = chatRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_MESSAGE_NOT_FOUND));

        // 5. 파일 메시지인지 확인
        if (message.getMessageType() != ChatMessageType.FILE && message.getMessageType() != ChatMessageType.IMAGE) {
            throw new IllegalArgumentException("파일이 첨부되지 않은 메시지입니다");
        }

        if (message.getFilePath() == null) {
            throw new IllegalArgumentException("파일 경로가 존재하지 않습니다");
        }

        // 6. 파일 다운로드 정보 반환
        return ChatMessageFileDownloadDto.builder()
                .filePath(message.getFilePath())
                .fileName(message.getFileName())
                .fileType(message.getFileType())
                .fileSize(message.getFileSize())
                .build();
    }
}
