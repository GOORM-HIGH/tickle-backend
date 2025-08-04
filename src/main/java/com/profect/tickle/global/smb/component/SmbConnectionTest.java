package com.profect.tickle.global.smb.component;

import com.profect.tickle.global.smb.service.NasSmbService;
import com.profect.tickle.global.smb.config.SmbProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class SmbConnectionTest {

    @Autowired
    private NasSmbService nasSmbService;

    @Autowired
    private SmbProperties smbProperties;

    @PostConstruct
    public void testConnection() {
        try {
            System.out.println("=== SMB 연결 테스트 시작 ===");
            System.out.println("Host: " + smbProperties.getHost());
            System.out.println("Port: " + smbProperties.getPort());
            System.out.println("Username: " + smbProperties.getUsername());
            System.out.println("Share: " + smbProperties.getShare());

            boolean connected = nasSmbService.fileExists("/");
            System.out.println("SMB NAS 연결 상태: " + (connected ? "성공" : "실패"));

        } catch (Exception e) {
            System.err.println("SMB NAS 연결 실패: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
