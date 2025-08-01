package com.profect.tickle.global.config;

import com.profect.tickle.global.util.properties.DatasourceHikariProperties;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@Configuration
@MapperScan(basePackages = "com.profect.tickle.domain", annotationClass = Mapper.class)
@RequiredArgsConstructor
public class MybatisConfig {

    // DB설정 프로퍼티
    private final DataSourceProperties dataSourceProperties;
    private final DatasourceHikariProperties datasourceHikariProperties;

    /**
     * HikariCP DataSource 생성
     * - PostgreSQL 연결 정보 및 커넥션 풀 옵션을 설정
     */
    @Bean
    public HikariDataSource hikariDataSource() {
        HikariDataSource hikariDataSource = new HikariDataSource();
        // DB 기본 연결 정보 설정
        hikariDataSource.setDriverClassName(dataSourceProperties.getDriverClassName());
        hikariDataSource.setJdbcUrl(dataSourceProperties.getUrl());
        hikariDataSource.setUsername(dataSourceProperties.getUsername());
        hikariDataSource.setPassword(dataSourceProperties.getPassword());

        // HikariCP 커넥션 풀 옵션 설정
        hikariDataSource.setMaximumPoolSize(datasourceHikariProperties.getMaximumPoolSize());
        hikariDataSource.setMinimumIdle(datasourceHikariProperties.getMinimumIdle());
        hikariDataSource.setConnectionTimeout(datasourceHikariProperties.getConnectionTimeOut());
        hikariDataSource.setIdleTimeout(datasourceHikariProperties.getIdleTimeOut());
        hikariDataSource.setMaxLifetime(datasourceHikariProperties.getMaxLifetime());

        return hikariDataSource;
    }

    /**
     * MyBatis SqlSessionFactory 생성
     * - MyBatis와 HikariCP DataSource를 연결
     * - CustomMapper와 같은 매퍼를 MyBatis Configuration에 등록
     */
    @Bean
    public SqlSessionFactory sqlSessionFactory() throws Exception {
        // MyBatis 전역 설정 객체 생성
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
//        configuration.addMapper(NotificationMapper.class); // 알림 매퍼 등록
//        configuration.setMapUnderscoreToCamelCase(true);

        // SqlSessionFactoryBean 설정
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(hikariDataSource()); // DataSource 주입
        sqlSessionFactoryBean.setConfiguration(configuration);   // MyBatis 전역 설정 주입

        // **패키지 단위로 별칭 등록 (클래스 이름을 별칭으로 사용)**
        sqlSessionFactoryBean.setTypeAliasesPackage(
                "com.profect.tickle.domain.**.dto.response"
        );

        // 매퍼 XML 경로 추가
        sqlSessionFactoryBean.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources("classpath:mapper/**/*.xml")
        );

        return sqlSessionFactoryBean.getObject(); // SqlSessionFactory 객체 반환
    }

    /**
     * SqlSessionTemplate 생성
     * - MyBatis의 SqlSession을 스레드 세이프하게 사용하도록 래핑
     * - DAO/Service 계층에서 주입받아 쿼리 실행에 사용
     */
    @Bean
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}
