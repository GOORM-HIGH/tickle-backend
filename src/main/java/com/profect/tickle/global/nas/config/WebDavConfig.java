package com.profect.tickle.global.nas.config;

import com.profect.tickle.global.nas.properties.WebDavProperties;
import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class WebDavConfig {

    private final WebDavProperties webDavProperties;

    @Bean
    public Sardine sardineClient() {
        return SardineFactory.begin(
                webDavProperties.getUsername(),
                webDavProperties.getPassword()
        );
    }
}
