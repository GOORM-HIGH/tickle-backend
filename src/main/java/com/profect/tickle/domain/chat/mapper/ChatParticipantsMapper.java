package com.profect.tickle.domain.chat.mapper;

import com.profect.tickle.domain.chat.dto.response.ChatParticipantsResponseDto;
import com.profect.tickle.domain.chat.dto.response.UnreadCountResponseDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;

@Mapper
public interface ChatParticipantsMapper {

    /**
     * 채팅방 참여자 목록 조회 (닉네임 포함)
     */
    List<ChatParticipantsResponseDto> findParticipantsByRoomId(@Param("roomId") Long roomId);

    /**
     * 읽음 상태 업데이트
     */
    int updateLastReadMessage(
            @Param("roomId") Long roomId,
            @Param("memberId") Long memberId,
            @Param("messageId") Long messageId,
            @Param("readAt") Instant readAt
    );

    /**
     * 사용자의 읽음 상태 조회
     */
    UnreadCountResponseDto getReadStatus(
            @Param("roomId") Long roomId,
            @Param("memberId") Long memberId
    );

    /**
     * 채팅방 참여자 수 조회
     */
    int countActiveParticipants(@Param("roomId") Long roomId);

    /**
     * 사용자가 참여 중인 채팅방 목록 조회 (복잡한 정보 포함)
     */
    List<ChatParticipantsResponseDto> findMyChatRooms(@Param("memberId") Long memberId);
}
