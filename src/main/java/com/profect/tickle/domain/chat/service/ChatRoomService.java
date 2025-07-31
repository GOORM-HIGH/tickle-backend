package com.profect.tickle.domain.chat.service;

import com.profect.tickle.domain.chat.dto.request.ChatRoomCreateRequestDto;
import com.profect.tickle.domain.chat.dto.response.ChatRoomResponseDto;
import com.profect.tickle.domain.chat.entity.ChatRoom;
import com.profect.tickle.domain.chat.mapper.ChatRoomMapper;
import com.profect.tickle.domain.chat.repository.ChatRoomRepository;
import com.profect.tickle.domain.performance.entity.Performance;
import com.profect.tickle.domain.performance.repository.PerformanceRepository;
import com.profect.tickle.global.exception.ChatExceptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final PerformanceRepository performanceRepository; // 팀원이 만든 Repository
    private final ChatRoomMapper chatRoomMapper; // MyBatis Mapper
    private final OnlineUserService onlineUserService;

    /**
     * 채팅방 생성 (JPA 사용)
     */
    @Transactional
    public ChatRoomResponseDto createChatRoom(ChatRoomCreateRequestDto requestDto) {
        log.info("채팅방 생성 요청: performanceId={}, roomName={}",
                requestDto.getPerformanceId(), requestDto.getRoomName());

        // 1. 공연 존재 여부 확인
        Performance performance = performanceRepository.findById(requestDto.getPerformanceId())
                .orElseThrow(() -> ChatExceptions.performanceNotFoundInChat(requestDto.getPerformanceId())); // ✅ 수정

        // 2. 이미 채팅방이 있는지 확인
        Optional<ChatRoom> existingRoom = chatRoomRepository.findByPerformanceId(requestDto.getPerformanceId());
        if (existingRoom.isPresent()) {
            throw ChatExceptions.chatRoomAlreadyExists(requestDto.getPerformanceId()); // ✅ 수정
        }


        // 3. 새 채팅방 생성
        ChatRoom chatRoom = ChatRoom.builder()
                .performanceId(requestDto.getPerformanceId())  // ✅ Long 값 전달
                .name(requestDto.getRoomName())           // ✅ 올바른 getter 메서드
                .status(true)
                .maxParticipants(requestDto.getMaxParticipants())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        ChatRoom savedRoom = chatRoomRepository.save(chatRoom);
        log.info("채팅방 생성 완료: chatRoomId={}", savedRoom.getId());

        return ChatRoomResponseDto.fromEntity(savedRoom);
    }


    /**
     * 공연별 채팅방 조회 (MyBatis 사용 - 복잡한 정보 포함)
     */
    public ChatRoomResponseDto getChatRoomByPerformanceId(Long performanceId, Long currentMemberId) {
        log.info("채팅방 조회 요청: performanceId={}, memberId={}", performanceId, currentMemberId);

        // MyBatis로 복잡한 정보와 함께 조회
        ChatRoomResponseDto roomDetails = chatRoomMapper.findRoomDetailsByPerformanceId(performanceId, currentMemberId);

        if (roomDetails == null) {
            throw ChatExceptions.chatRoomNotFoundByPerformance(performanceId); // ✅ 수정
        }

        return roomDetails;
    }

    /**
     * 채팅방 기본 정보 조회 (JPA 사용)
     */
    public ChatRoomResponseDto getChatRoomById(Long chatRoomId) {
        log.info("채팅방 기본 정보 조회: chatRoomId={}", chatRoomId);

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> ChatExceptions.chatRoomNotFound(chatRoomId)); // ✅ 수정

        return ChatRoomResponseDto.fromEntity(chatRoom);
    }

    /**
     * 사용자가 채팅방에 참여 중인지 확인 (MyBatis 사용)
     */
    public boolean isParticipant(Long chatRoomId, Long memberId) {
        return chatRoomMapper.isParticipant(chatRoomId, memberId);
    }

    /**
     * 채팅방 상태 업데이트 (JPA 사용)
     */
    @Transactional
    public void updateChatRoomStatus(Long chatRoomId, boolean status) {
        log.info("채팅방 상태 변경: chatRoomId={}, status={}", chatRoomId, status);

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> ChatExceptions.chatRoomNotFound(chatRoomId)); // ✅ 수정

        // Entity의 상태 변경 (더티 체킹으로 자동 업데이트)
        chatRoom.updateStatus(status); // Entity에 이 메서드를 추가해야 함
    }

    /**
     * 채팅방 온라인 사용자 정보 조회
     */
    public Map<String, Object> getOnlineUserInfo(Long chatRoomId) {
        log.info("채팅방 온라인 사용자 정보 조회: chatRoomId={}", chatRoomId);

        // 채팅방 존재 확인
        chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> ChatExceptions.chatRoomNotFound(chatRoomId));

        // 온라인 사용자 정보 수집
        int onlineCount = onlineUserService.getOnlineCount(chatRoomId);
        Set<Long> onlineUserIds = onlineUserService.getOnlineUsers(chatRoomId);

        Map<String, Object> result = new HashMap<>();
        result.put("chatRoomId", chatRoomId);
        result.put("onlineCount", onlineCount);
        result.put("onlineUserIds", onlineUserIds);
        result.put("timestamp", Instant.now());

        return result;
    }

}
