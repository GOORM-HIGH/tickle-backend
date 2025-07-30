package com.profect.tickle.domain.notification.mapper;

import com.profect.tickle.domain.notification.dto.response.NotificationResponseDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NotificationMapper {

    List<NotificationResponseDto> getRecentNotificationListByMemberId(@Param("memberId") Long userId);
}
