package com.profect.tickle.global.nas.service;

import com.profect.tickle.global.nas.properties.WebDavProperties;
import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebDavService {

    private final Sardine sardine;
    private final WebDavProperties webDavProperties;

    @PostConstruct
    public void init() {
        testConnection();
    }

    public void testConnection() {
        try {
            log.info("=== WebDAV 연결 테스트 시작 ===");
            log.info("URL: {}", webDavProperties.getUrl());
            log.info("Username: {}", webDavProperties.getUsername());
            log.info("Path: {}", webDavProperties.getPath());

            // 기본 WebDAV 경로 연결 테스트
            List<DavResource> resources = sardine.list(webDavProperties.getUrl());
            log.info("WebDAV 연결 성공! 리소스 개수: {}", resources.size());

            // tickle 폴더 확인/생성 (개선된 버전)
            String ticklePath = webDavProperties.getUrl().replaceAll("/$", "") + webDavProperties.getPath();

            try {
                // 폴더 목록 조회로 존재 확인
                List<DavResource> folderResources = sardine.list(ticklePath, 0);
                log.info("✅ tickle 폴더 존재 확인: {} (리소스 개수: {})", ticklePath, folderResources.size());

            } catch (Exception folderException) {
                // 폴더가 없을 때만 생성 시도
                try {
                    log.warn("⚠️ tickle 폴더가 없습니다. 생성합니다: {}", ticklePath);
                    sardine.createDirectory(ticklePath);
                    log.info("✅ tickle 폴더 생성 완료");

                } catch (IOException createException) {
                    // 403, 405 등은 폴더가 이미 있을 가능성이 높음
                    if (createException.getMessage().contains("403") ||
                            createException.getMessage().contains("405") ||
                            createException.getMessage().contains("Forbidden") ||
                            createException.getMessage().contains("Method Not Allowed")) {
                        log.warn("⚠️ 폴더 생성 권한 문제이지만 계속 진행합니다: {}", createException.getMessage());
                    } else {
                        throw createException;
                    }
                }
            }

            log.info("=== WebDAV NAS 연결 상태: 성공 ===");

        } catch (Exception e) {
            log.error("❌ WebDAV 연결 실패: {}", e.getMessage());
            log.error("연결 정보를 확인해주세요.");
        }
    }

    /**
     * 파일 업로드
     */
    public void uploadFile(String fileName, byte[] fileData) throws IOException {
        String fullPath = buildFilePath(fileName);
        sardine.put(fullPath, fileData);
        log.info("📤 파일 업로드 완료: {}", fileName);
    }

    /**
     * 파일 업로드 (InputStream 버전)
     */
    public void uploadFile(String fileName, InputStream inputStream) throws IOException {
        String fullPath = buildFilePath(fileName);
        sardine.put(fullPath, inputStream);
        log.info("📤 파일 업로드 완료: {}", fileName);
    }

    /**
     * 파일 다운로드
     */
    public InputStream downloadFile(String fileName) throws IOException {
        String fullPath = buildFilePath(fileName);
        log.info("📥 파일 다운로드 시작: {}", fileName);
        return sardine.get(fullPath);
    }

    /**
     * 파일 존재 여부 확인
     */
    public boolean fileExists(String fileName) throws IOException {
        String fullPath = buildFilePath(fileName);
        return sardine.exists(fullPath);
    }

    /**
     * 파일 삭제
     */
    public void deleteFile(String fileName) throws IOException {
        String fullPath = buildFilePath(fileName);
        sardine.delete(fullPath);
        log.info("🗑️ 파일 삭제 완료: {}", fileName);
    }

    /**
     * 폴더 내 파일 목록 조회
     */
    public List<DavResource> listFiles() throws IOException {
        String folderPath = webDavProperties.getUrl().replaceAll("/$", "") + webDavProperties.getPath();
        return sardine.list(folderPath);
    }

    /**
     * 전체 파일 경로 생성
     */
    private String buildFilePath(String fileName) {
        String baseUrl = webDavProperties.getUrl().replaceAll("/$", "");
        String path = webDavProperties.getPath();
        return baseUrl + path + "/" + fileName;
    }
}
