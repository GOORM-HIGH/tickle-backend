package com.profect.tickle.domain.notification.mapper;

import com.profect.tickle.domain.notification.entity.NotificationTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface NotificationTemplateMapper {

    Optional<NotificationTemplate> findById(@Param("id") Long id);
}
