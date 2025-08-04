package com.profect.tickle.global.nas.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "nas.webdav")
@Data
public class WebDavProperties {
    private String url;
    private String username;
    private String password;
    private String path;
}
