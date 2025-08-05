package com.profect.tickle.batch;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class TransactionConfig {

    // 스프링부트의 기본 HikariDataSource 객체 주입 받아서 txManager 하나 더 생성함
    private final DataSource dataSource;

    public TransactionConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * JPA용 트랜잭션매니저
     * 'transactionManager'는 스프링부트가 자동으로 생성해주는 jpaTM의 명칭
     * 아래에서 배치 전용 tm을 PlatformTransactionManager로 생성해서 이미 tm이 있다고 판단하여 jpaTm 생성을 스킵함
     * 위 문제로 JPA용 tm 하나 더 생성하고 @Primary로 기본 tm으로 등록!
     */
    @Bean(name = "transactionManager")
    @Primary
    public PlatformTransactionManager jpaTransactionManager(
            EntityManagerFactory emf
    ) {
        return new JpaTransactionManager(emf);
    }

    /**
     * 배치 전용 트랜잭션매니저
     * 위의 JPA용 트랜잭션매니저와 분기
     */
    @Bean(name ="batchTxManager")
    public PlatformTransactionManager batchTxManager() {
        return new DataSourceTransactionManager(dataSource);
    }
}
