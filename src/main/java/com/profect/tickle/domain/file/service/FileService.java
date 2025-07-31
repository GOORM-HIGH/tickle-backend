package com.profect.tickle.domain.file.service;

import com.profect.tickle.domain.file.dto.response.FileUploadResponseDto;
import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.global.exception.ChatExceptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class FileService {

    private final MemberRepository memberRepository;

    @Value("${file.upload.path:uploads/chat}")
    private String uploadPath;

    @Value("${file.upload.max-size:10485760}")  // 10MB
    private long maxFileSize;

    /**
     * 파일 업로드 (파일 시스템에만 저장, Chat 엔티티는 메시지 전송시 활용)
     */
    @Transactional
    public FileUploadResponseDto uploadFile(MultipartFile file, Long uploaderId) {
        log.info("파일 업로드 요청: fileName={}, size={}, uploaderId={}",
                file.getOriginalFilename(), file.getSize(), uploaderId);

        // 1. 업로드 사용자 확인
        Member uploader = memberRepository.findById(uploaderId)
                .orElseThrow(() -> ChatExceptions.memberNotFoundInChat(uploaderId));

        // 2. 파일 검증
        validateFile(file);

        // 3. 파일 저장
        String storedFileName = generateStoredFileName(file.getOriginalFilename());
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String fullPath = uploadPath + "/" + datePath;

        try {
            // 디렉토리 생성
            Path uploadDir = Paths.get(fullPath);
            Files.createDirectories(uploadDir);

            // 파일 저장
            Path filePath = uploadDir.resolve(storedFileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // 4. 응답 DTO 생성 (DB 저장은 메시지 전송시에)
            String savedFilePath = fullPath + "/" + storedFileName;

            log.info("파일 업로드 완료: storedName={}, path={}", storedFileName, savedFilePath);

            return FileUploadResponseDto.of(
                    storedFileName,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    (int) file.getSize(),
                    savedFilePath
            );

        } catch (IOException e) {
            log.error("파일 저장 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("파일 저장에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 파일 다운로드 (Chat 엔티티의 filePath로부터)
     */
    public Resource downloadFile(String filePath, String originalFileName) {
        log.info("파일 다운로드 요청: filePath={}, originalName={}", filePath, originalFileName);

        try {
            Path path = Paths.get(filePath);
            Resource resource = new UrlResource(path.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new RuntimeException("파일을 읽을 수 없습니다: " + originalFileName);
            }

            log.info("파일 다운로드 준비 완료: originalName={}", originalFileName);
            return resource;

        } catch (Exception e) {
            log.error("파일 다운로드 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("파일 다운로드에 실패했습니다: " + e.getMessage());
        }
    }

    // ===== Private 헬퍼 메서드들 =====

    /**
     * 파일 검증
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다");
        }

        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException(String.format("파일 크기가 최대 크기(%s)를 초과했습니다", formatFileSize(maxFileSize)));
        }

        String fileName = file.getOriginalFilename();
        if (!StringUtils.hasText(fileName)) {
            throw new IllegalArgumentException("파일명이 유효하지 않습니다");
        }

        // 허용되지 않는 확장자 검사
        String extension = getFileExtension(fileName).toLowerCase();
        if (isNotAllowedExtension(extension)) {
            throw new IllegalArgumentException("허용되지 않는 파일 형식입니다: " + extension);
        }
    }

    /**
     * 저장할 파일명 생성 (UUID 사용)
     */
    private String generateStoredFileName(String originalFileName) {
        String extension = getFileExtension(originalFileName);
        return UUID.randomUUID().toString() + "." + extension;
    }

    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String fileName) {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        }
        return "";
    }

    /**
     * 허용되지 않는 확장자 검사
     */
    private boolean isNotAllowedExtension(String extension) {
        String[] notAllowed = {"exe", "bat", "cmd", "scr", "pif", "jar", "vbs", "js"};
        for (String blocked : notAllowed) {
            if (blocked.equals(extension)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 파일 크기 포맷팅
     */
    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        return String.format("%.1f MB", size / (1024.0 * 1024));
    }
}
