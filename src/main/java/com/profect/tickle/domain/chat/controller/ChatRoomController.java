package com.profect.tickle.domain.chat.controller;

import com.profect.tickle.domain.chat.annotation.CurrentMember;
import com.profect.tickle.global.response.ResultResponse;
import com.profect.tickle.global.response.ResultCode;
import com.profect.tickle.domain.chat.dto.request.ChatRoomCreateRequestDto;
import com.profect.tickle.domain.chat.dto.response.ChatRoomResponseDto;
import com.profect.tickle.domain.chat.service.ChatRoomService;
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

import java.util.Map;

@RestController
@RequestMapping("/api/v1/chat/rooms")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "채팅방 관리", description = "채팅방 생성, 조회, 관리 API")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    /**
     * 채팅방 생성
     */
    @Operation(
            summary = "채팅방 생성",
            description = "공연에 대한 채팅방을 생성합니다. 하나의 공연당 하나의 채팅방만 생성 가능합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "채팅방 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "409", description = "이미 채팅방이 존재함"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping
    public ResultResponse<ChatRoomResponseDto> createChatRoom(
            @Valid @RequestBody ChatRoomCreateRequestDto requestDto) {

        log.info("채팅방 생성 API 호출: performanceId={}", requestDto.getPerformanceId());

        ChatRoomResponseDto response = chatRoomService.createChatRoom(requestDto);

        return ResultResponse.of(ResultCode.CHAT_ROOM_CREATE_SUCCESS, response);
    }

    /**
     * 공연별 채팅방 상세 조회
     */
    @Operation(
            summary = "공연별 채팅방 조회",
            description = "공연 ID로 채팅방의 상세 정보를 조회합니다. 참여자 수, 읽지않은 메시지 수 등 포함",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/performance/{performanceId}")
    public ResultResponse<ChatRoomResponseDto> getChatRoomByPerformanceId(
            @Parameter(description = "공연 ID", required = true, example = "123")
            @PathVariable Long performanceId,
            @Parameter(description = "현재 사용자 ID (JWT에서 추출)", hidden = true)
            @CurrentMember Long currentMemberId) {

        log.info("공연별 채팅방 조회 API 호출: performanceId={}, memberId={}", performanceId, currentMemberId);

        ChatRoomResponseDto response = chatRoomService.getChatRoomByPerformanceId(performanceId, currentMemberId);

        return ResultResponse.of(ResultCode.CHAT_ROOM_INFO_SUCCESS, response);
    }

    /**
     * 채팅방 ID로 기본 정보 조회
     */
    @Operation(
            summary = "채팅방 기본 정보 조회",
            description = "채팅방 ID로 기본 정보를 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/{chatRoomId}")
    public ResultResponse<ChatRoomResponseDto> getChatRoomById(
            @Parameter(description = "채팅방 ID", required = true, example = "456")
            @PathVariable Long chatRoomId) {

        log.info("채팅방 기본 정보 조회 API 호출: chatRoomId={}", chatRoomId);

        ChatRoomResponseDto response = chatRoomService.getChatRoomById(chatRoomId);

        return ResultResponse.of(ResultCode.CHAT_ROOM_INFO_SUCCESS, response);
    }

    /**
     * 채팅방 상태 변경 (활성화/비활성화)
     */
    @Operation(
            summary = "채팅방 상태 변경",
            description = "채팅방을 활성화하거나 비활성화합니다. 관리자 또는 공연 주최자만 가능합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상태 변경 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PatchMapping("/{chatRoomId}/status")
    public ResultResponse<Void> updateChatRoomStatus(
            @Parameter(description = "채팅방 ID", required = true, example = "456")
            @PathVariable Long chatRoomId,
            @Parameter(description = "변경할 상태", required = true, example = "true")
            @RequestParam boolean status) {

        log.info("채팅방 상태 변경 API 호출: chatRoomId={}, status={}", chatRoomId, status);

        chatRoomService.updateChatRoomStatus(chatRoomId, status);

        return ResultResponse.ok(ResultCode.CHAT_ROOM_UPDATE_SUCCESS);
    }

    /**
     * 사용자 참여 여부 확인
     */
    @Operation(
            summary = "채팅방 참여 여부 확인",
            description = "현재 사용자가 채팅방에 참여 중인지 확인합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "확인 성공"),
            @ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/{chatRoomId}/participation")
    public ResultResponse<Boolean> checkParticipation(
            @Parameter(description = "채팅방 ID", required = true, example = "456")
            @PathVariable Long chatRoomId,
            @Parameter(description = "현재 사용자 ID (JWT에서 추출)", hidden = true)
            @CurrentMember Long currentMemberId) {

        log.info("채팅방 참여 여부 확인 API 호출: chatRoomId={}, memberId={}", chatRoomId, currentMemberId);

        boolean isParticipant = chatRoomService.isParticipant(chatRoomId, currentMemberId);

        return ResultResponse.of(ResultCode.CHAT_ROOM_INFO_SUCCESS, isParticipant);
    }

    /**
     * 채팅방 온라인 사용자 조회
     */
    @Operation(
            summary = "채팅방 온라인 사용자 조회",
            description = "채팅방에 현재 접속 중인 사용자들의 목록과 수를 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/{chatRoomId}/online")
    public ResultResponse<Map<String, Object>> getOnlineUsers(
            @Parameter(description = "채팅방 ID", required = true, example = "123")
            @PathVariable Long chatRoomId) {

        log.info("채팅방 온라인 사용자 조회 API 호출: chatRoomId={}", chatRoomId);

        Map<String, Object> result = chatRoomService.getOnlineUserInfo(chatRoomId);

        return ResultResponse.of(ResultCode.CHAT_ROOM_INFO_SUCCESS, result);
    }
}
