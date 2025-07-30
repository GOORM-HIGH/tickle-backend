package com.profect.tickle.global.config;

import com.profect.tickle.domain.chat.dto.response.ChatMessageResponseDto;
import com.profect.tickle.domain.chat.dto.response.ChatParticipantsResponseDto;
import com.profect.tickle.domain.chat.dto.response.ChatRoomResponseDto;
import com.profect.tickle.domain.chat.dto.response.UnreadCountResponseDto;
import com.profect.tickle.domain.chat.mapper.ChatMessageMapper;
import com.profect.tickle.domain.chat.mapper.ChatParticipantsMapper;
import com.profect.tickle.domain.chat.mapper.ChatRoomMapper;
import com.profect.tickle.domain.notification.dto.response.NotificationResponseDTO;
import com.profect.tickle.domain.notification.mapper.NotificationMapper;
import com.profect.tickle.global.properties.DatasourceHikariProperties;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@Configuration
@RequiredArgsConstructor
public class MybatisConfig {

    private final DataSourceProperties dataSourceProperties;
    private final DatasourceHikariProperties datasourceHikariProperties;

    @Bean
    public HikariDataSource hikariDataSource() {
        // 기존 코드 그대로 유지
        HikariDataSource hikariDataSource = new HikariDataSource();
        hikariDataSource.setDriverClassName(dataSourceProperties.getDriverClassName());
        hikariDataSource.setJdbcUrl(dataSourceProperties.getUrl());
        hikariDataSource.setUsername(dataSourceProperties.getUsername());
        hikariDataSource.setPassword(dataSourceProperties.getPassword());

        hikariDataSource.setMaximumPoolSize(datasourceHikariProperties.getMaximumPoolSize());
        hikariDataSource.setMinimumIdle(datasourceHikariProperties.getMinimumIdle());
        hikariDataSource.setConnectionTimeout(datasourceHikariProperties.getConnectionTimeOut());
        hikariDataSource.setIdleTimeout(datasourceHikariProperties.getIdleTimeOut());
        hikariDataSource.setMaxLifetime(datasourceHikariProperties.getMaxLifetime());

        return hikariDataSource;
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory() throws Exception {
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();

        // ✅ 모든 Mapper 등록
        configuration.addMapper(NotificationMapper.class);
        configuration.addMapper(ChatMessageMapper.class);
        configuration.addMapper(ChatParticipantsMapper.class);
        configuration.addMapper(ChatRoomMapper.class);

        // ✅ 설정 활성화
        configuration.setMapUnderscoreToCamelCase(true);

        // ✅ DTO 별칭 등록
        configuration.getTypeAliasRegistry().registerAlias("NotificationDTO", NotificationResponseDTO.class);
        configuration.getTypeAliasRegistry().registerAlias("ChatMessageResponseDto", ChatMessageResponseDto.class);
        configuration.getTypeAliasRegistry().registerAlias("ChatParticipantsResponseDto", ChatParticipantsResponseDto.class);
        configuration.getTypeAliasRegistry().registerAlias("ChatRoomResponseDto", ChatRoomResponseDto.class);
        configuration.getTypeAliasRegistry().registerAlias("UnreadCountResponseDto", UnreadCountResponseDto.class);

        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(hikariDataSource());
        sqlSessionFactoryBean.setConfiguration(configuration);

        // ✅ XML 파일 위치 설정 (핵심!)
        sqlSessionFactoryBean.setMapperLocations(
                new PathMatchingResourcePatternResolver()
                        .getResources("classpath:mappers/**/*.xml")
        );

        return sqlSessionFactoryBean.getObject();
    }

    @Bean
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}
