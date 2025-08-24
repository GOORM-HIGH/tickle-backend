package com.profect.tickle.domain.file.controller;

import com.profect.tickle.domain.chat.annotation.CurrentMember;
import com.profect.tickle.domain.file.dto.response.FileUploadResponseDto;
import com.profect.tickle.domain.file.service.FileService;
import com.profect.tickle.global.response.ResultCode;
import com.profect.tickle.global.response.ResultResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "파일 관리", description = "파일 업로드, 다운로드, 관리 API")
public class FileController {

    private final FileService fileService;

    /**
     * 파일 업로드
     */
    @Operation(
            summary = "파일 업로드",
            description = "파일을 업로드하고 저장 경로를 반환합니다. 이미지, 문서 등 모든 파일 타입 지원",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "파일 업로드 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 파일 형식 또는 크기 초과"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping("/upload")
    public ResultResponse<FileUploadResponseDto> uploadFile(
            @Parameter(description = "업로드할 파일", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "현재 사용자 ID (JWT에서 추출)", hidden = true)
            @CurrentMember Long uploaderId) {
        
        FileUploadResponseDto response = fileService.uploadFile(file, uploaderId);
        return ResultResponse.of(ResultCode.FILE_UPLOAD_SUCCESS, response);
    }

    /**
     * 커스텀 이미지 업로드
     */
    @Operation(
            summary = "커스텀 이미지 업로드",
            description = "프로필 사진이나 공연 이미지 등 커스텀 이미지를 업로드합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "이미지 업로드 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 이미지 형식"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping("/custom-image")
    public ResultResponse<FileUploadResponseDto> uploadCustomImage(
            @Parameter(description = "업로드할 이미지 파일", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "이미지 타입 (profile, performance)", required = true, example = "profile")
            @RequestParam("imageType") String imageType,
            @Parameter(description = "현재 사용자 ID (JWT에서 추출)", hidden = true)
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
        return ResultResponse.of(ResultCode.FILE_UPLOAD_SUCCESS, response);
    }

    /**
     * 커스텀 이미지 다운로드
     */
    @Operation(
            summary = "커스텀 이미지 다운로드",
            description = "사용자가 업로드한 커스텀 이미지를 다운로드합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "이미지 다운로드 성공"),
            @ApiResponse(responseCode = "404", description = "이미지를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/custom-image/download")
    public ResultResponse<Map<String, Object>> downloadCustomImage(
            @Parameter(description = "파일명", required = true, example = "image.jpg")
            @RequestParam("fileName") String fileName,
            @Parameter(description = "이미지 타입", required = true, example = "profile")
            @RequestParam("imageType") String imageType,
            @Parameter(description = "현재 사용자 ID (JWT에서 추출)", hidden = true)
            @CurrentMember Long requestUserId) {
        
        String downloadUrl = fileService.generatePreSignedUrl(fileName, requestUserId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("downloadUrl", downloadUrl);
        response.put("fileName", fileName);
        response.put("imageType", imageType);
        response.put("message", "다운로드 URL이 생성되었습니다.");
        
        return ResultResponse.of(ResultCode.FILE_UPLOAD_SUCCESS, response);
    }

    /**
     * 채팅 파일 다운로드
     */
    @Operation(
            summary = "채팅 파일 다운로드",
            description = "채팅에서 공유된 파일을 다운로드합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "파일 다운로드 성공"),
            @ApiResponse(responseCode = "404", description = "파일을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/chat/download")
    public ResultResponse<Map<String, Object>> downloadChatFile(
            @Parameter(description = "파일명", required = true, example = "document.pdf")
            @RequestParam("fileName") String fileName,
            @Parameter(description = "현재 사용자 ID (JWT에서 추출)", hidden = true)
            @CurrentMember Long requestUserId) {
        
        String downloadUrl = fileService.generatePreSignedUrl(fileName);
        
        Map<String, Object> response = new HashMap<>();
        response.put("downloadUrl", downloadUrl);
        response.put("fileName", fileName);
        response.put("message", "채팅 파일 다운로드 URL이 생성되었습니다.");
        
        return ResultResponse.of(ResultCode.FILE_UPLOAD_SUCCESS, response);
    }

    /**
     * 파일 다운로드
     */
    @Operation(
            summary = "파일 다운로드",
            description = "저장된 파일을 다운로드합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "파일 다운로드 성공"),
            @ApiResponse(responseCode = "404", description = "파일을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(
            @Parameter(description = "파일명", required = true, example = "filename.jpg")
            @RequestParam("fileName") String fileName,
            @Parameter(description = "원본 파일명", example = "original.jpg")
            @RequestParam(value = "originalName", required = false) String originalName,
            @Parameter(description = "현재 사용자 ID (JWT에서 추출)", hidden = true)
            @CurrentMember Long requestUserId) {
        
        try {
            Resource resource = fileService.downloadFile(fileName, originalName != null ? originalName : fileName);
            
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + 
                            (originalName != null ? originalName : fileName) + "\"")
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("파일 다운로드 실패: fileName={}, error={}", fileName, e.getMessage());
            throw new RuntimeException("파일 다운로드에 실패했습니다: " + e.getMessage());
        }
    }
}
