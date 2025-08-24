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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
     * 파일 업로드 (기본 - 채팅용)
     */
    public void uploadFile(String fileName, byte[] fileData) throws IOException {
        String fullPath = buildFilePath(fileName);
        sardine.put(fullPath, fileData);
        log.info("📤 파일 업로드 완료: {}", fileName);
    }

    /**
     * 파일 업로드 (InputStream 버전, 기본 - 채팅용)
     */
    public void uploadFile(String fileName, InputStream inputStream) throws IOException {
        // 날짜별 폴더 생성
        createDateDirectory();
        
        String fullPath = buildFilePath(fileName);
        sardine.put(fullPath, inputStream);
        log.info("📤 파일 업로드 완료: {}", fileName);
    }

    /**
     * 파일 업로드 (사용자별 분류 - 프로필/공연 이미지용)
     */
    public void uploadFile(String fileName, byte[] fileData, Long userId) throws IOException {
        // 사용자별 폴더 생성
        createUserDirectory(userId);
        
        String fullPath = buildUserFilePath(fileName, userId);
        log.info("🔍 사용자별 파일 업로드 경로: fileName={}, userId={}, fullPath={}", fileName, userId, fullPath);
        
        // 파일 존재 여부 확인
        if (sardine.exists(fullPath)) {
            log.warn("⚠️ 파일이 이미 존재합니다: {}", fullPath);
        }
        
        sardine.put(fullPath, fileData);
        log.info("📤 사용자별 파일 업로드 완료: fileName={}, userId={}", fileName, userId);
    }

    /**
     * 파일 업로드 (InputStream 버전, 사용자별 분류 - 프로필/공연 이미지용)
     */
    public void uploadFile(String fileName, InputStream inputStream, Long userId) throws IOException {
        // 사용자별 폴더 생성
        createUserDirectory(userId);
        
        String fullPath = buildUserFilePath(fileName, userId);
        sardine.put(fullPath, inputStream);
        log.info("📤 사용자별 파일 업로드 완료: fileName={}, userId={}", fileName, userId);
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
     * 사용자별 폴더 생성 (프로필/공연 이미지용)
     */
    private void createUserDirectory(Long userId) throws IOException {
        String baseUrl = webDavProperties.getUrl().replaceAll("/$", "");
        String path = webDavProperties.getPath();
        
        // 1. 먼저 users 폴더 생성
        String usersDirectoryPath;
        if (path == null || path.trim().isEmpty()) {
            usersDirectoryPath = baseUrl + "/users";
        } else {
            usersDirectoryPath = baseUrl + path + "/users";
        }
        
        log.info("users 폴더 경로 생성: path={}", usersDirectoryPath);
        
        try {
            if (!sardine.exists(usersDirectoryPath)) {
                log.info("users 폴더 생성: path={}", usersDirectoryPath);
                sardine.createDirectory(usersDirectoryPath);
                log.info("✅ users 폴더 생성 완료");
            } else {
                log.debug("users 폴더 이미 존재: path={}", usersDirectoryPath);
            }
        } catch (IOException e) {
            if (e.getMessage().contains("403") ||
                    e.getMessage().contains("405") ||
                    e.getMessage().contains("Forbidden") ||
                    e.getMessage().contains("Method Not Allowed")) {
                log.warn("⚠️ users 폴더 생성 권한 문제이지만 계속 진행합니다: error={}", e.getMessage());
            } else {
                log.error("❌ users 폴더 생성 실패: error={}", e.getMessage());
                throw e;
            }
        }
        
        // 2. 그 다음 사용자별 폴더 생성
        String userDirectoryPath;
        if (path == null || path.trim().isEmpty()) {
            userDirectoryPath = baseUrl + "/users/" + userId;
        } else {
            userDirectoryPath = baseUrl + path + "/users/" + userId;
        }
        
        log.info("사용자 폴더 경로 생성: userId={}, path={}", userId, userDirectoryPath);
        
        try {
            if (!sardine.exists(userDirectoryPath)) {
                log.info("사용자 폴더 생성: userId={}, path={}", userId, userDirectoryPath);
                sardine.createDirectory(userDirectoryPath);
                log.info("✅ 사용자 폴더 생성 완료: userId={}", userId);
            } else {
                log.debug("사용자 폴더 이미 존재: userId={}, path={}", userId, userDirectoryPath);
            }
        } catch (IOException e) {
            if (e.getMessage().contains("403") ||
                    e.getMessage().contains("405") ||
                    e.getMessage().contains("Forbidden") ||
                    e.getMessage().contains("Method Not Allowed")) {
                log.warn("⚠️ 사용자 폴더 생성 권한 문제이지만 계속 진행합니다: userId={}, error={}", userId, e.getMessage());
            } else {
                log.error("❌ 사용자 폴더 생성 실패: userId={}, error={}", userId, e.getMessage());
                throw e;
            }
        }
    }

    // 날짜별 폴더 생성 (채팅용)
    private void createDateDirectory() throws IOException {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String baseUrl = webDavProperties.getUrl().replaceAll("/$", "");
        String path = webDavProperties.getPath();
        
        String dateDirectoryPath;
        if (path == null || path.trim().isEmpty()) {
            dateDirectoryPath = baseUrl + "/" + datePath;
        } else {
            dateDirectoryPath = baseUrl + path + "/" + datePath;
        }
        
        log.info("날짜별 폴더 경로 생성: path={}", dateDirectoryPath);
        
        try {
            if (!sardine.exists(dateDirectoryPath)) {
                log.info("날짜별 폴더 생성: path={}", dateDirectoryPath);
                sardine.createDirectory(dateDirectoryPath);
                log.info("✅ 날짜별 폴더 생성 완료");
            } else {
                log.debug("날짜별 폴더 이미 존재: path={}", dateDirectoryPath);
            }
        } catch (IOException e) {
            if (e.getMessage().contains("403") ||
                    e.getMessage().contains("405") ||
                    e.getMessage().contains("Forbidden") ||
                    e.getMessage().contains("Method Not Allowed")) {
                log.warn("⚠️ 날짜별 폴더 생성 권한 문제이지만 계속 진행합니다: error={}", e.getMessage());
            } else {
                log.error("❌ 날짜별 폴더 생성 실패: error={}", e.getMessage());
                throw e;
            }
        }
    }

    /**
     * 사용자별 파일 경로 생성 (프로필/공연 이미지용)
     */
    private String buildUserFilePath(String fileName, Long userId) {
        String baseUrl = webDavProperties.getUrl().replaceAll("/$", "");
        String path = webDavProperties.getPath();
        String userPath = "users/" + userId;
        
        // 빈 문자열 path 처리
        if (path == null || path.trim().isEmpty()) {
            return baseUrl + "/" + userPath + "/" + fileName;
        } else {
            return baseUrl + path + "/" + userPath + "/" + fileName;
        }
    }

    /**
     * 전체 파일 경로 생성
     */
    private String buildFilePath(String fileName) {
        String baseUrl = webDavProperties.getUrl().replaceAll("/$", "");
        String path = webDavProperties.getPath();
        
        // 빈 문자열 path 처리
        if (path == null || path.trim().isEmpty()) {
            return baseUrl + "/" + fileName;
        } else {
            return baseUrl + path + "/" + fileName;
        }
    }

    /**
     * PreSigned URL 생성 (사용자별 분류)
     */
    public String generatePreSignedUrl(String fileName, Long userId) {
        try {
            String baseUrl = webDavProperties.getUrl().replaceAll("/$", "");
            String path = webDavProperties.getPath();
            String userPath = "users/" + userId;
            
            // 실제 PreSigned URL 생성 (인증 정보 포함)
            String filePath = userPath + "/" + fileName;
            String fileUrl = baseUrl + path + "/" + filePath;
            
            // 인증 정보가 포함된 URL 생성 (실제 구현에서는 토큰 기반 인증 사용)
            String preSignedUrl = generateAuthenticatedUrl(fileUrl, userId);
            
            log.info("PreSigned URL 생성: fileName={}, userId={}, url={}", fileName, userId, preSignedUrl);
            return preSignedUrl;
            
        } catch (Exception e) {
            log.error("PreSigned URL 생성 실패: fileName={}, userId={}, error={}", fileName, userId, e.getMessage());
            throw new RuntimeException("PreSigned URL 생성에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * PreSigned URL 생성 (기본)
     */
    public String generatePreSignedUrl(String fileName) {
        try {
            String baseUrl = webDavProperties.getUrl().replaceAll("/$", "");
            String path = webDavProperties.getPath();
            String fileUrl = baseUrl + path + "/" + fileName;
            
            // 인증 정보가 포함된 URL 생성
            String preSignedUrl = generateAuthenticatedUrl(fileUrl, null);
            
            log.info("PreSigned URL 생성: fileName={}, url={}", fileName, preSignedUrl);
            return preSignedUrl;
            
        } catch (Exception e) {
            log.error("PreSigned URL 생성 실패: fileName={}, error={}", fileName, e.getMessage());
            throw new RuntimeException("PreSigned URL 생성에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 인증된 URL 생성 (실제 구현에서는 토큰 기반 인증 사용)
     */
    private String generateAuthenticatedUrl(String fileUrl, Long userId) {
        try {
            // 현재는 단순 URL 반환 (실제 구현에서는 JWT 토큰이나 서명 추가)
            // TODO: 실제 PreSigned URL 로직 구현 (예: AWS S3 스타일 서명)
            
            // 임시로 서버를 통한 다운로드 URL 생성
            if (userId != null) {
                return "http://localhost:8081/api/v1/files/custom-image/download?fileName=" + 
                       fileUrl.substring(fileUrl.lastIndexOf("/") + 1) + 
                       "&imageType=performance";
            } else {
                return "http://localhost:8081/api/v1/files/chat/download?fileName=" + 
                       fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            }
            
        } catch (Exception e) {
            log.error("인증된 URL 생성 실패: fileUrl={}, userId={}, error={}", fileUrl, userId, e.getMessage());
            return fileUrl; // 실패시 기본 URL 반환
        }
    }
}
