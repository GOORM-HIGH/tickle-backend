package com.profect.tickle.domain.chat.controller;

import com.profect.tickle.domain.chat.annotation.CurrentMember;
import com.profect.tickle.global.response.ResultResponse;
import com.profect.tickle.global.response.ResultCode;
import com.profect.tickle.domain.chat.dto.request.ChatRoomJoinRequestDto;
import com.profect.tickle.domain.chat.dto.request.ReadMessageRequestDto;
import com.profect.tickle.domain.chat.dto.response.ChatParticipantsResponseDto;
import com.profect.tickle.domain.chat.dto.response.UnreadCountResponseDto;
import com.profect.tickle.domain.chat.service.ChatParticipantsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat/participants")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "채팅 참여자", description = "채팅방 참여, 나가기, 읽음 처리 API")
public class ChatParticipantsController {

    private final ChatParticipantsService chatParticipantsService;

    /**
     * 채팅방 참여
     */
    @Operation(
            summary = "채팅방 참여",
            description = "지정된 채팅방에 참여합니다. 이미 참여했던 경우 재참여됩니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "참여 성공"),
            @ApiResponse(responseCode = "400", description = "채팅방 정원 초과 또는 이미 참여 중"),
            @ApiResponse(responseCode = "404", description = "채팅방 또는 회원을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping("/rooms/{chatRoomId}/join")
    public ResultResponse<ChatParticipantsResponseDto> joinChatRoom(
            @Parameter(description = "채팅방 ID", required = true, example = "123")
            @PathVariable Long chatRoomId,
            @Parameter(description = "현재 사용자 ID (JWT에서 추출)", hidden = true)
            @CurrentMember Long currentMemberId,
            @Valid @RequestBody ChatRoomJoinRequestDto requestDto) {

        log.info("채팅방 참여 API 호출: chatRoomId={}, memberId={}", chatRoomId, currentMemberId);

        ChatParticipantsResponseDto response = chatParticipantsService.joinChatRoom(chatRoomId, currentMemberId, requestDto);

        return ResultResponse.of(ResultCode.CHAT_PARTICIPANT_JOIN_SUCCESS, response);
    }

    /**
     * 채팅방 나가기
     */
    @Operation(
            summary = "채팅방 나가기",
            description = "참여 중인 채팅방에서 나갑니다. 논리 삭제로 처리됩니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "나가기 성공"),
            @ApiResponse(responseCode = "404", description = "채팅방 또는 참여 정보를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @DeleteMapping("/rooms/{chatRoomId}/leave")
    public ResultResponse<Void> leaveChatRoom(
            @Parameter(description = "채팅방 ID", required = true, example = "123")
            @PathVariable Long chatRoomId,
            @Parameter(description = "현재 사용자 ID (JWT에서 추출)", hidden = true)
            @CurrentMember Long currentMemberId) {

        log.info("채팅방 나가기 API 호출: chatRoomId={}, memberId={}", chatRoomId, currentMemberId);

        chatParticipantsService.leaveChatRoom(chatRoomId, currentMemberId);

        return ResultResponse.ok(ResultCode.CHAT_PARTICIPANT_LEAVE_SUCCESS);
    }

    /**
     * 메시지 읽음 처리
     */
    @Operation(
            summary = "메시지 읽음 처리",
            description = "특정 메시지까지 읽음으로 표시합니다. 해당 메시지 이전의 모든 메시지도 읽음 처리됩니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "읽음 처리 성공"),
            @ApiResponse(responseCode = "404", description = "채팅방 참여 정보를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PatchMapping("/rooms/{chatRoomId}/read")
    public ResultResponse<Void> markAsRead(
            @Parameter(description = "채팅방 ID", required = true, example = "123")
            @PathVariable Long chatRoomId,
            @Parameter(description = "현재 사용자 ID (JWT에서 추출)", hidden = true)
            @CurrentMember Long currentMemberId,
            @Valid @RequestBody ReadMessageRequestDto requestDto) {

        log.info("메시지 읽음 처리 API 호출: chatRoomId={}, memberId={}, messageId={}",
                chatRoomId, currentMemberId, requestDto.getLastReadMessageId());

        chatParticipantsService.markAsRead(chatRoomId, currentMemberId, requestDto);

        return ResultResponse.ok(ResultCode.CHAT_PARTICIPANT_READ_SUCCESS);
    }

    /**
     * 읽지않은 메시지 개수 조회
     */
    @Operation(
            summary = "읽지않은 메시지 개수 조회",
            description = "현재 사용자의 특정 채팅방 내 읽지않은 메시지 개수와 읽음 상태를 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "채팅방 참여 정보를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/rooms/{chatRoomId}/unread-count")
    public ResultResponse<UnreadCountResponseDto> getUnreadCount(
            @Parameter(description = "채팅방 ID", required = true, example = "123")
            @PathVariable Long chatRoomId,
            @Parameter(description = "현재 사용자 ID (JWT에서 추출)", hidden = true)
            @CurrentMember Long currentMemberId) {

        log.info("읽지않은 메시지 개수 조회 API 호출: chatRoomId={}, memberId={}", chatRoomId, currentMemberId);

        UnreadCountResponseDto response = chatParticipantsService.getUnreadCount(chatRoomId, currentMemberId);

        return ResultResponse.of(ResultCode.CHAT_PARTICIPANT_READ_SUCCESS, response);
    }

    /**
     * 채팅방 참여자 목록 조회
     */
    @Operation(
            summary = "채팅방 참여자 목록 조회",
            description = "채팅방에 참여 중인 사용자들의 목록을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/rooms/{chatRoomId}")
    public ResultResponse<List<ChatParticipantsResponseDto>> getParticipants(
            @Parameter(description = "채팅방 ID", required = true, example = "123")
            @PathVariable Long chatRoomId) {

        log.info("채팅방 참여자 목록 조회 API 호출: chatRoomId={}", chatRoomId);

        List<ChatParticipantsResponseDto> response = chatParticipantsService.getParticipantsByRoomId(chatRoomId);

        return ResultResponse.of(ResultCode.CHAT_PARTICIPANT_READ_SUCCESS, response);
    }

    /**
     * 내가 참여한 채팅방 목록 조회
     */
    @Operation(
            summary = "내 채팅방 목록 조회",
            description = "현재 사용자가 참여 중인 모든 채팅방 목록을 조회합니다. 읽지않은 메시지 수 포함.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/my-rooms")
    public ResultResponse<List<ChatParticipantsResponseDto>> getMyChatRooms(
            @Parameter(description = "현재 사용자 ID (JWT에서 추출)", hidden = true)
            @CurrentMember Long currentMemberId) {

        log.info("내 채팅방 목록 조회 API 호출: memberId={}", currentMemberId);

        List<ChatParticipantsResponseDto> response = chatParticipantsService.getMyChatRooms(currentMemberId);

        return ResultResponse.of(ResultCode.CHAT_PARTICIPANT_READ_SUCCESS, response);
    }
}
