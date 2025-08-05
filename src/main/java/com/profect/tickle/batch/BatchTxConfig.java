package com.profect.tickle.batch;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class BatchTxConfig {

    // 스프링부트의 기본 HikariDataSource 객체 주입 받아서 txManager 하나 더 생성함
    private final DataSource dataSource;

    public BatchTxConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 배치 전용 트랜잭션 매니저
     * 기존 JPA @Transactional에 쓰이는 JpaTransactionManager와 분기
     */
    @Bean
    public PlatformTransactionManager batchTxManager() {
        return new DataSourceTransactionManager(dataSource);
    }
}
