package com.profect.tickle.domain.chat.controller;

import com.profect.tickle.domain.chat.annotation.CurrentMember; // ✅ import 추가
import com.profect.tickle.domain.chat.dto.common.ApiResponseDto;
import com.profect.tickle.domain.chat.dto.request.ChatMessageSendRequestDto;
import com.profect.tickle.domain.chat.dto.response.ChatMessageListResponseDto;
import com.profect.tickle.domain.chat.dto.response.ChatMessageResponseDto;
import com.profect.tickle.domain.chat.service.ChatMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/chat/rooms/{chatRoomId}/messages")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "채팅 메시지", description = "채팅 메시지 송수신, 조회, 관리 API")
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    /**
     * 메시지 전송
     */
    @Operation(
            summary = "메시지 전송",
            description = "채팅방에 메시지를 전송합니다. 텍스트, 파일, 이미지 타입 지원",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "메시지 전송 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "403", description = "채팅방 참여 권한 없음"),
            @ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping
    public ResponseEntity<ApiResponseDto<ChatMessageResponseDto>> sendMessage(
            @Parameter(description = "채팅방 ID", required = true, example = "123")
            @PathVariable Long chatRoomId,
            @Parameter(description = "현재 사용자 ID (JWT에서 추출)", hidden = true)
            @CurrentMember Long currentMemberId, // ✅ 변경
            @Valid @RequestBody ChatMessageSendRequestDto requestDto) {

        log.info("메시지 전송 API 호출: chatRoomId={}, memberId={}, type={}",
                chatRoomId, currentMemberId, requestDto.getMessageType());

        ChatMessageResponseDto response = chatMessageService.sendMessage(chatRoomId, currentMemberId, requestDto);

        return ResponseEntity.status(201)
                .body(ApiResponseDto.created(response));
    }

    /**
     * 메시지 목록 조회 (페이징)
     */
    @Operation(
            summary = "메시지 목록 조회",
            description = "채팅방의 메시지 목록을 페이징으로 조회합니다. 최신 메시지가 먼저 표시됩니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "채팅방 참여 권한 없음"),
            @ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping
    public ResponseEntity<ApiResponseDto<ChatMessageListResponseDto>> getMessages(
            @Parameter(description = "채팅방 ID", required = true, example = "123")
            @PathVariable Long chatRoomId,
            @Parameter(description = "현재 사용자 ID (JWT에서 추출)", hidden = true)
            @CurrentMember Long currentMemberId, // ✅ 변경
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "50")
            @RequestParam(defaultValue = "50") int size,
            @Parameter(description = "마지막 메시지 ID (무한스크롤용)", example = "999")
            @RequestParam(required = false) Long lastMessageId) {

        log.info("메시지 목록 조회 API 호출: chatRoomId={}, memberId={}, page={}, size={}",
                chatRoomId, currentMemberId, page, size);

        ChatMessageListResponseDto response = chatMessageService.getMessages(
                chatRoomId, currentMemberId, page, size, lastMessageId);

        return ResponseEntity.ok(ApiResponseDto.success(response));
    }

    /**
     * 메시지 수정
     */
    @Operation(
            summary = "메시지 수정",
            description = "자신이 작성한 메시지를 수정합니다. 텍스트 메시지만 수정 가능합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "수정할 수 없는 메시지"),
            @ApiResponse(responseCode = "403", description = "수정 권한 없음"),
            @ApiResponse(responseCode = "404", description = "메시지를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PutMapping("/{messageId}")
    public ResponseEntity<ApiResponseDto<ChatMessageResponseDto>> editMessage(
            @Parameter(description = "채팅방 ID", required = true, example = "123")
            @PathVariable Long chatRoomId,
            @Parameter(description = "메시지 ID", required = true, example = "456")
            @PathVariable Long messageId,
            @Parameter(description = "현재 사용자 ID (JWT에서 추출)", hidden = true)
            @CurrentMember Long currentMemberId, // ✅ 변경
            @Parameter(description = "수정할 메시지 내용", required = true)
            @RequestBody String newContent) {

        log.info("메시지 수정 API 호출: chatRoomId={}, messageId={}, memberId={}",
                chatRoomId, messageId, currentMemberId);

        ChatMessageResponseDto response = chatMessageService.editMessage(messageId, currentMemberId, newContent);

        return ResponseEntity.ok(ApiResponseDto.success("메시지가 수정되었습니다.", response));
    }

    /**
     * 메시지 삭제
     */
    @Operation(
            summary = "메시지 삭제",
            description = "자신이 작성한 메시지를 삭제합니다. 논리 삭제로 처리됩니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "400", description = "이미 삭제된 메시지"),
            @ApiResponse(responseCode = "403", description = "삭제 권한 없음"),
            @ApiResponse(responseCode = "404", description = "메시지를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @DeleteMapping("/{messageId}")
    public ResponseEntity<ApiResponseDto<Void>> deleteMessage(
            @Parameter(description = "채팅방 ID", required = true, example = "123")
            @PathVariable Long chatRoomId,
            @Parameter(description = "메시지 ID", required = true, example = "456")
            @PathVariable Long messageId,
            @Parameter(description = "현재 사용자 ID (JWT에서 추출)", hidden = true)
            @CurrentMember Long currentMemberId) { // ✅ 변경

        log.info("메시지 삭제 API 호출: chatRoomId={}, messageId={}, memberId={}",
                chatRoomId, messageId, currentMemberId);

        chatMessageService.deleteMessage(messageId, currentMemberId);

        return ResponseEntity.ok(ApiResponseDto.success("메시지가 삭제되었습니다.", null));
    }

    /**
     * 채팅방의 마지막 메시지 조회
     */
    @Operation(
            summary = "마지막 메시지 조회",
            description = "채팅방의 가장 최근 메시지를 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/last")
    public ResponseEntity<ApiResponseDto<ChatMessageResponseDto>> getLastMessage(
            @Parameter(description = "채팅방 ID", required = true, example = "123")
            @PathVariable Long chatRoomId,
            @Parameter(description = "현재 사용자 ID (JWT에서 추출)", hidden = true)
            @CurrentMember Long currentMemberId) { // ✅ 변경

        log.info("마지막 메시지 조회 API 호출: chatRoomId={}, memberId={}", chatRoomId, currentMemberId);

        ChatMessageResponseDto response = chatMessageService.getLastMessage(chatRoomId, currentMemberId);

        if (response == null) {
            return ResponseEntity.ok(ApiResponseDto.success("메시지가 없습니다.", null));
        }

        return ResponseEntity.ok(ApiResponseDto.success(response));
    }

    /**
     * 읽지않은 메시지 개수 조회
     */
    @Operation(
            summary = "읽지않은 메시지 개수 조회",
            description = "현재 사용자의 채팅방 내 읽지않은 메시지 개수를 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "채팅방 참여 권한 없음"),
            @ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponseDto<Integer>> getUnreadCount(
            @Parameter(description = "채팅방 ID", required = true, example = "123")
            @PathVariable Long chatRoomId,
            @Parameter(description = "현재 사용자 ID (JWT에서 추출)", hidden = true)
            @CurrentMember Long currentMemberId, // ✅ 변경
            @Parameter(description = "마지막으로 읽은 메시지 ID", example = "789")
            @RequestParam(required = false) Long lastReadMessageId) {

        log.info("읽지않은 메시지 개수 조회 API 호출: chatRoomId={}, memberId={}, lastReadMessageId={}", 
                chatRoomId, currentMemberId, lastReadMessageId);

        int unreadCount = chatMessageService.getUnreadCount(chatRoomId, currentMemberId, lastReadMessageId);

        log.info("읽지않은 메시지 개수 조회 결과: chatRoomId={}, memberId={}, unreadCount={}", 
                chatRoomId, currentMemberId, unreadCount);

        return ResponseEntity.ok(ApiResponseDto.success(unreadCount));
    }
}
