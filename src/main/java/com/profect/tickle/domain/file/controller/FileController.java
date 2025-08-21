package com.profect.tickle.domain.file.controller;

import com.profect.tickle.domain.chat.annotation.CurrentMember;
import com.profect.tickle.domain.chat.dto.common.ApiResponseDto;
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
    public ResponseEntity<ApiResponseDto<FileUploadResponseDto>> uploadFile(
            @Parameter(description = "업로드할 파일", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "현재 사용자 ID (JWT에서 추출)", hidden = true)
            @CurrentMember Long uploaderId) {

        log.info("파일 업로드 API 호출: fileName={}, size={}, uploaderId={}",
                file.getOriginalFilename(), file.getSize(), uploaderId);

        FileUploadResponseDto response = fileService.uploadFile(file, uploaderId);

        return ResponseEntity.status(201)
                .body(ApiResponseDto.created(response));
    }
}
