package com.profect.tickle.domain.chat.mapper;

import com.profect.tickle.domain.chat.dto.response.ChatRoomResponseDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ChatRoomMapper {

    /**
     * 채팅방 상세 정보 조회 (참여자 수, 읽지않은 메시지 등 포함)
     */
    ChatRoomResponseDto findRoomDetailsByPerformanceId(
            @Param("performanceId") Long performanceId,
            @Param("currentMemberId") Long currentMemberId
    );

    /**
     * 사용자가 해당 채팅방에 참여 중인지 확인
     */
    boolean isParticipant(
            @Param("roomId") Long roomId,
            @Param("memberId") Long memberId
    );
}
