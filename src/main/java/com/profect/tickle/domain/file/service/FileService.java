package com.profect.tickle.domain.file.service;

import com.profect.tickle.domain.file.dto.response.FileUploadResponseDto;
import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.global.exception.ChatExceptions;
import com.profect.tickle.global.nas.service.WebDavService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class FileService {

    private final MemberRepository memberRepository;
    private final WebDavService webDavService;

    @Value("${file.upload.max-size:10485760}")  // 10MB
    private long maxFileSize;

    /**
     * 파일 업로드 (NAS WebDAV에 저장, Chat 엔티티는 메시지 전송시 활용)
     */
    @Transactional
    public FileUploadResponseDto uploadFile(MultipartFile file, Long uploaderId) {
        log.info("파일 업로드 요청: fileName={}, size={}, uploaderId={}",
                file.getOriginalFilename(), file.getSize(), uploaderId);

        // 1. 업로드 사용자 확인
        memberRepository.findById(uploaderId)
                .orElseThrow(() -> ChatExceptions.memberNotFoundInChat(uploaderId));

        // 2. 파일 검증
        validateFile(file);

        // 3. NAS에 파일 저장
        String storedFileName = generateStoredFileName(file.getOriginalFilename());
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String nasFilePath = datePath + "/" + storedFileName;  // chat/ 제거

        try {
            // NAS WebDAV에 파일 업로드
            webDavService.uploadFile(nasFilePath, file.getInputStream());

            // 4. 응답 DTO 생성 (DB 저장은 메시지 전송시에)
            log.info("파일 업로드 완료: storedName={}, nasPath={}", storedFileName, nasFilePath);

            return FileUploadResponseDto.of(
                    storedFileName,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    (int) file.getSize(),
                    nasFilePath  // NAS 경로 저장
            );

        } catch (IOException e) {
            log.error("NAS 파일 저장 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("파일 저장에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 파일 다운로드 (NAS WebDAV에서 Chat 엔티티의 filePath로부터)
     */
    public Resource downloadFile(String nasFilePath, String originalFileName) {
        log.info("파일 다운로드 요청: nasFilePath={}, originalName={}", nasFilePath, originalFileName);

        try {
            // NAS WebDAV에서 파일 다운로드
            InputStream inputStream = webDavService.downloadFile(nasFilePath);
            
            log.info("파일 다운로드 준비 완료: originalName={}", originalFileName);
            return new InputStreamResource(inputStream);

        } catch (IOException e) {
            log.error("NAS 파일 다운로드 중 오류 발생: {}", e.getMessage(), e);
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
