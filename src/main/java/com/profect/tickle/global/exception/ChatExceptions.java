package com.profect.tickle.global.exception;

/**
 * 채팅 관련 예외를 편리하게 생성하는 유틸리티 클래스
 * 기존 GlobalExceptionHandler와 BusinessException을 활용
 */
public class ChatExceptions {

    // ===== 채팅방 관련 예외들 =====

    /**
     * 채팅방을 찾을 수 없음 (ID로 조회)
     */
    public static BusinessException chatRoomNotFound(Long chatRoomId) {
        return new BusinessException(
                "존재하지 않는 채팅방입니다: " + chatRoomId,
                ErrorCode.CHAT_ROOM_NOT_FOUND
        );
    }

    /**
     * 채팅방을 찾을 수 없음 (공연 ID로 조회)
     */
    public static BusinessException chatRoomNotFoundByPerformance(Long performanceId) {
        return new BusinessException(
                "해당 공연의 채팅방이 존재하지 않습니다: " + performanceId,
                ErrorCode.CHAT_ROOM_NOT_FOUND
        );
    }

    /**
     * 채팅방이 이미 존재함
     */
    public static BusinessException chatRoomAlreadyExists(Long performanceId) {
        return new BusinessException(
                "해당 공연의 채팅방이 이미 존재합니다: " + performanceId,
                ErrorCode.CHAT_ROOM_ALREADY_EXISTS
        );
    }

    /**
     * 비활성화된 채팅방
     */
    public static BusinessException chatRoomInactive(Long chatRoomId) {
        return new BusinessException(
                "비활성화된 채팅방입니다: " + chatRoomId,
                ErrorCode.CHAT_ROOM_INACTIVE
        );
    }

    /**
     * 채팅방 정원 초과
     */
    public static BusinessException chatRoomCapacityExceeded() {
        return new BusinessException(ErrorCode.CHAT_ROOM_CAPACITY_EXCEEDED);
    }

    // ===== 메시지 관련 예외들 =====

    /**
     * 메시지를 찾을 수 없음
     */
    public static BusinessException chatMessageNotFound(Long messageId) {
        return new BusinessException(
                "존재하지 않는 메시지입니다: " + messageId,
                ErrorCode.CHAT_MESSAGE_NOT_FOUND
        );
    }

    /**
     * 빈 메시지 내용
     */
    public static BusinessException chatMessageEmptyContent() {
        return new BusinessException(ErrorCode.CHAT_MESSAGE_EMPTY_CONTENT);
    }

    /**
     * 메시지 내용이 너무 길음
     */
    public static BusinessException chatMessageTooLong() {
        return new BusinessException(ErrorCode.CHAT_MESSAGE_TOO_LONG);
    }

    /**
     * 파일 정보 누락
     */
    public static BusinessException chatMessageMissingFileInfo() {
        return new BusinessException(ErrorCode.CHAT_MESSAGE_MISSING_FILE_INFO);
    }

    /**
     * 잘못된 파일 크기
     */
    public static BusinessException chatMessageInvalidFileSize() {
        return new BusinessException(ErrorCode.CHAT_MESSAGE_INVALID_FILE_SIZE);
    }

    /**
     * 이미 삭제된 메시지 (ID 포함)
     */
    public static BusinessException chatMessageAlreadyDeleted(Long messageId) {
        return new BusinessException(
                "이미 삭제된 메시지입니다: " + messageId,
                ErrorCode.CHAT_MESSAGE_ALREADY_DELETED
        );
    }

    /**
     * 삭제된 메시지는 수정 불가
     */
    public static BusinessException chatMessageCannotEdit() {
        return new BusinessException(
                "삭제된 메시지는 수정할 수 없습니다",
                ErrorCode.CHAT_MESSAGE_ALREADY_DELETED
        );
    }

    // ===== 권한 관련 예외들 =====

    /**
     * 채팅방에 참여하지 않은 사용자
     */
    public static BusinessException chatNotParticipant() {
        return new BusinessException(ErrorCode.CHAT_NOT_PARTICIPANT);
    }

    /**
     * 메시지 소유자가 아님 (수정/삭제 권한 없음)
     */
    public static BusinessException chatNotMessageOwner() {
        return new BusinessException(ErrorCode.CHAT_NOT_MESSAGE_OWNER);
    }

    /**
     * 채팅 기능 권한 없음 (일반적인 권한 오류)
     */
    public static BusinessException chatPermissionDenied(String reason) {
        return new BusinessException(
                "채팅 기능에 대한 권한이 없습니다: " + reason,
                ErrorCode.CHAT_PERMISSION_DENIED
        );
    }

    // ===== 참여자 관련 예외들 =====

    /**
     * 채팅방 참여 정보를 찾을 수 없음
     */
    public static BusinessException chatParticipantNotFound(Long chatRoomId, Long memberId) {
        return new BusinessException(
                String.format("채팅방 참여 정보를 찾을 수 없습니다. 채팅방ID: %d, 회원ID: %d", chatRoomId, memberId),
                ErrorCode.CHAT_PARTICIPANT_NOT_FOUND
        );
    }

    /**
     * 이미 참여 중인 채팅방
     */
    public static BusinessException chatAlreadyParticipant(Long chatRoomId) {
        return new BusinessException(
                "이미 참여 중인 채팅방입니다: " + chatRoomId,
                ErrorCode.CHAT_ROOM_ALREADY_EXISTS  // 적절한 에러 코드 재활용
        );
    }

    // ===== 회원/공연 관련 예외들 (채팅 컨텍스트) =====

    /**
     * 존재하지 않는 회원 (채팅 컨텍스트)
     */
    public static BusinessException memberNotFoundInChat(Long memberId) {
        return new BusinessException(
                "존재하지 않는 회원입니다: " + memberId,
                ErrorCode.INVALID_INPUT_VALUE  // 기존 에러 코드 활용
        );
    }

    /**
     * 존재하지 않는 공연 (채팅 컨텍스트)
     */
    public static BusinessException performanceNotFoundInChat(Long performanceId) {
        return new BusinessException(
                "존재하지 않는 공연입니다: " + performanceId,
                ErrorCode.INVALID_INPUT_VALUE  // 기존 에러 코드 활용
        );
    }

    // ===== 편의 메서드들 (자주 사용되는 패턴) =====

    /**
     * 채팅방 접근 권한 체크용 (참여 여부 + 활성 상태)
     */
    public static BusinessException chatRoomAccessDenied(Long chatRoomId, String reason) {
        return new BusinessException(
                String.format("채팅방 접근이 거부되었습니다 (ID: %d): %s", chatRoomId, reason),
                ErrorCode.CHAT_PERMISSION_DENIED
        );
    }

    /**
     * 메시지 조작 권한 체크용 (소유자 확인)
     */
    public static BusinessException messageOperationDenied(Long messageId, String operation) {
        return new BusinessException(
                String.format("메시지 %s 권한이 없습니다 (메시지ID: %d)", operation, messageId),
                ErrorCode.CHAT_NOT_MESSAGE_OWNER
        );
    }
}
