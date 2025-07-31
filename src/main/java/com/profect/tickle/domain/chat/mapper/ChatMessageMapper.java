package com.profect.tickle.domain.chat.mapper;

import com.profect.tickle.domain.chat.dto.response.ChatMessageResponseDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatMessageMapper {

    /**
     * 채팅방 메시지 목록 조회 (페이징)
     */
    List<ChatMessageResponseDto> findMessagesByRoomId(
            @Param("roomId") Long roomId,
            @Param("currentMemberId") Long currentMemberId,
            @Param("offset") int offset,
            @Param("size") int size,
            @Param("lastMessageId") Long lastMessageId  // 무한스크롤용
    );

    /**
     * 채팅방 전체 메시지 개수 조회
     */
    int countTotalMessages(
            @Param("roomId") Long roomId,
            @Param("lastMessageId") Long lastMessageId
    );

    /**
     * 읽지않은 메시지 개수 조회
     */
    int countUnreadMessages(
            @Param("roomId") Long roomId,
            @Param("memberId") Long memberId,
            @Param("lastReadMessageId") Long lastReadMessageId
    );

    /**
     * 채팅방의 마지막 메시지 조회 (수정 버전)
     */
    ChatMessageResponseDto findLastMessageByRoomId(
            @Param("roomId") Long roomId,
            @Param("currentMemberId") Long currentMemberId  // ✅ 파라미터 추가
    );

}
