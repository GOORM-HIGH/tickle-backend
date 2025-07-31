package com.profect.tickle.global.util.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "spring.database")
public class DatasourceProperties {

    private String driverClassName;
    private String url;
    private String username;
    private String password;
}
