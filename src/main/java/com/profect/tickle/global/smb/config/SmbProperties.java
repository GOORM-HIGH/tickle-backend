package com.profect.tickle.global.smb.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "nas.smb")
@Data
public class SmbProperties {
    private String host;
    private int port = 445;
    private String username;
    private String password;
    private String share;
}
