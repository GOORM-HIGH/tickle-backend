package com.profect.tickle.domain.notification.mapper;

import com.profect.tickle.domain.notification.dto.response.NotificationResponseDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NotificationMapper {

    List<NotificationResponseDTO> getRecentNotificationListByMemberId(@Param("memberId") Long userId);
}
