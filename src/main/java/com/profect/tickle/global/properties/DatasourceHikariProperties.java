package com.profect.tickle.global.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "spring.datasource.hikari")
public class DatasourceHikariProperties {

    private int maximumPoolSize;
    private int minimumIdle;
    private int connectionTimeOut;
    private int idleTimeOut;
    private int maxLifetime;
}
