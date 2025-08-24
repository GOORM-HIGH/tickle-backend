package com.profect.tickle.domain.chat.service;

import com.profect.tickle.domain.chat.dto.request.ChatMessageSendRequestDto;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 채팅 메시지 검증 전용 클래스
 * SRP: 메시지 검증만 담당
 */
@Component
@Slf4j
public class ChatMessageValidator {

    /**
     * 메시지 내용 검증
     */
    public void validateMessage(ChatMessageSendRequestDto requestDto) {
        log.debug("메시지 검증 시작: type={}", requestDto.getMessageType());

        switch (requestDto.getMessageType()) {
            case TEXT:
                validateTextMessage(requestDto);
                break;

            case FILE:
            case IMAGE:
                validateFileMessage(requestDto);
                break;

            case SYSTEM:
                // 시스템 메시지는 별도 검증 로직
                break;

            default:
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        log.debug("메시지 검증 완료: type={}", requestDto.getMessageType());
    }

    /**
     * 텍스트 메시지 검증
     */
    private void validateTextMessage(ChatMessageSendRequestDto requestDto) {
        if (requestDto.getContent() == null || requestDto.getContent().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.CHAT_MESSAGE_EMPTY_CONTENT);
        }
        if (requestDto.getContent().length() > 255) {
            throw new BusinessException(ErrorCode.CHAT_MESSAGE_TOO_LONG);
        }
    }

    /**
     * 파일 메시지 검증
     */
    private void validateFileMessage(ChatMessageSendRequestDto requestDto) {
        if (requestDto.getFilePath() == null || requestDto.getFileName() == null) {
            throw new BusinessException(ErrorCode.CHAT_MESSAGE_MISSING_FILE_INFO);
        }
        if (requestDto.getFileSize() == null || requestDto.getFileSize() <= 0) {
            throw new BusinessException(ErrorCode.CHAT_MESSAGE_INVALID_FILE_SIZE);
        }
    }
}
