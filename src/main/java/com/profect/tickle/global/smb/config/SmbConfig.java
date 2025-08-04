package com.profect.tickle.global.smb.config;

import jcifs.DialectVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.smb.session.SmbSessionFactory;

@Configuration
public class SmbConfig {

    @Autowired
    private SmbProperties smbProperties;

    @Bean
    public SmbSessionFactory smbSessionFactory() {
        SmbSessionFactory smbSession = new SmbSessionFactory();
        smbSession.setHost(smbProperties.getHost());
        smbSession.setPort(smbProperties.getPort());
        smbSession.setDomain("");
        smbSession.setUsername(smbProperties.getUsername());
        smbSession.setPassword(smbProperties.getPassword());
        smbSession.setShareAndDir(smbProperties.getShare());
        smbSession.setSmbMinVersion(DialectVersion.SMB210);
        smbSession.setSmbMaxVersion(DialectVersion.SMB311);
        return smbSession;
    }
}
