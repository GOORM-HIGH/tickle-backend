package com.profect.tickle.domain.file.controller;

import com.profect.tickle.domain.chat.annotation.CurrentMember;
import com.profect.tickle.domain.file.dto.response.FileUploadResponseDto;
import com.profect.tickle.domain.file.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "파일 관리", description = "채팅 파일 업로드 API")
public class FileController {

    private final FileService fileService;

    /**
     * 파일 업로드
     */
    @Operation(
            summary = "파일 업로드",
            description = "채팅에서 사용할 파일을 업로드합니다. 업로드 후 메시지 전송시 응답 정보를 활용하세요.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "파일 업로드 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 파일 또는 크기 초과"),
            @ApiResponse(responseCode = "413", description = "파일 크기가 너무 큼"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponseDto> uploadFile(
            @RequestParam("file") MultipartFile file,
            @CurrentMember Long uploaderId) {
        
        FileUploadResponseDto response = fileService.uploadFile(file, uploaderId);
        return ResponseEntity.ok(response);
    }

    // 사용자별 커스텀 이미지 업로드 (프로필 사진, 공연 이미지 등)
    @PostMapping("/custom-image")
    public ResponseEntity<FileUploadResponseDto> uploadCustomImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("imageType") String imageType, // "profile" 또는 "performance"
            @CurrentMember Long uploaderId) {
        
        // 이미지 파일 검증
        if (!file.getContentType().startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드 가능합니다.");
        }
        
        // 이미지 타입 검증
        if (!imageType.equals("profile") && !imageType.equals("performance")) {
            throw new IllegalArgumentException("imageType은 'profile' 또는 'performance'만 가능합니다.");
        }
        
        FileUploadResponseDto response = fileService.uploadUserFile(file, uploaderId, imageType);
        return ResponseEntity.ok(response);
    }

    // 사용자별 커스텀 이미지 다운로드 (PreSigned URL 반환)
    @GetMapping("/custom-image/download")
    public ResponseEntity<Map<String, Object>> downloadCustomImage(
            @RequestParam("fileName") String fileName,
            @RequestParam("imageType") String imageType,
            @CurrentMember Long requestUserId) {
        
        try {
            String downloadUrl = fileService.generatePreSignedUrl(fileName, requestUserId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("downloadUrl", downloadUrl);
            response.put("fileName", fileName);
            response.put("imageType", imageType);
            response.put("message", "다운로드 URL이 생성되었습니다.");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "다운로드 URL 생성에 실패했습니다: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // 채팅 메시지 첨부 파일 다운로드 (PreSigned URL 반환)
    @GetMapping("/chat/download")
    public ResponseEntity<Map<String, Object>> downloadChatFile(
            @RequestParam("fileName") String fileName,
            @CurrentMember Long requestUserId) {
        
        try {
            String downloadUrl = fileService.generatePreSignedUrl(fileName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("downloadUrl", downloadUrl);
            response.put("fileName", fileName);
            response.put("message", "채팅 파일 다운로드 URL이 생성되었습니다.");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "다운로드 URL 생성에 실패했습니다: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
