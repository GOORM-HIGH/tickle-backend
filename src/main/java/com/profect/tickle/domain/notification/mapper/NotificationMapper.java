package com.profect.tickle.domain.notification.mapper;

import com.profect.tickle.domain.member.dto.response.MemberResponseDto;
import com.profect.tickle.domain.notification.dto.response.NotificationResponseDto;
import com.profect.tickle.global.status.Status;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;

@Mapper
public interface NotificationMapper {

    List<NotificationResponseDto> getNotificationListByMemberId(
            @Param("memberId") Long memberId,
            @Param("limit") int limit
    );

    void saveAll(
            @Param("memberList") List<MemberResponseDto> memberList,
            @Param("notificationTemplateId") Long templateId,
            @Param("subject") String subject,
            @Param("content") String content,
            @Param("createdAt") Instant createdAt,
            @Param("statusId") Long statusId
    );

    long count();
}
