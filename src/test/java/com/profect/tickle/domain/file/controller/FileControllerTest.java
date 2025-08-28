package com.profect.tickle.domain.file.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.profect.tickle.domain.chat.annotation.CurrentMember;
import com.profect.tickle.domain.chat.resolver.CurrentMemberArgumentResolver;
import com.profect.tickle.domain.file.dto.response.FileUploadResponseDto;
import com.profect.tickle.domain.file.service.FileService;
import com.profect.tickle.global.security.util.JwtUtil;
import com.profect.tickle.domain.chat.config.ChatJwtAuthenticationInterceptor;
import com.profect.tickle.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FileController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisabledInAotMode
@DisplayName("FileController 단위 테스트")
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileService fileService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private ChatJwtAuthenticationInterceptor chatJwtAuthenticationInterceptor;

    @MockBean
    private CurrentMemberArgumentResolver currentMemberArgumentResolver;

    @MockBean
    private MemberRepository memberRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static final Long MEMBER_ID = 6L;
    private static final String MEMBER_EMAIL = "ahn3931@naver.com";

    @BeforeEach
    void setUp() throws Exception {
        when(currentMemberArgumentResolver.supportsParameter(any())).thenReturn(true);
        when(currentMemberArgumentResolver.resolveArgument(any(), any(), any(), any())).thenReturn(MEMBER_ID);
        when(chatJwtAuthenticationInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    // ===== 파일 업로드 테스트 =====

    @Test
    @DisplayName("TC-FILE-UPLOAD-001: 파일 업로드 성공")
    void shouldUploadFileSuccessfully() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-file.txt",
                "text/plain",
                "Hello, World!".getBytes()
        );

        FileUploadResponseDto responseDto = FileUploadResponseDto.builder()
                .fileName("test-file.txt")
                .filePath("/uploads/chat/test-file.txt")
                .fileSize(13)
                .fileType("text/plain")
                .build();

        when(fileService.uploadFile(any(), eq(MEMBER_ID))).thenReturn(responseDto);

        // When & Then
        mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileName").value("test-file.txt"))
                .andExpect(jsonPath("$.data.filePath").value("/uploads/chat/test-file.txt"))
                .andExpect(jsonPath("$.data.fileSize").value(13))
                .andExpect(jsonPath("$.data.fileType").value("text/plain"));
    }

    @Test
    @DisplayName("TC-FILE-UPLOAD-002: 파일 업로드 실패 - 파일 없음")
    void shouldFailWhenNoFileProvided() throws Exception {
        // When & Then
        mockMvc.perform(multipart("/api/v1/files/upload")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("TC-FILE-UPLOAD-003: 파일 업로드 실패 - 서비스 오류")
    void shouldFailWhenServiceThrowsException() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-file.txt",
                "text/plain",
                "Hello, World!".getBytes()
        );

        when(fileService.uploadFile(any(), eq(MEMBER_ID)))
                .thenThrow(new RuntimeException("파일 업로드 실패"));

        // When & Then
        mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isInternalServerError());
    }

    // ===== 커스텀 이미지 업로드 테스트 =====

    @Test
    @DisplayName("TC-FILE-CUSTOM-001: 프로필 이미지 업로드 성공")
    void shouldUploadProfileImageSuccessfully() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "profile.jpg",
                "image/jpeg",
                "fake-image-data".getBytes()
        );

        FileUploadResponseDto responseDto = FileUploadResponseDto.builder()
                .fileName("profile.jpg")
                .filePath("/uploads/profile/profile.jpg")
                .fileSize(15)
                .fileType("image/jpeg")
                .build();

        when(fileService.uploadUserFile(any(), eq(MEMBER_ID), eq("profile"))).thenReturn(responseDto);

        // When & Then
        mockMvc.perform(multipart("/api/v1/files/custom-image")
                        .file(file)
                        .param("imageType", "profile")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileName").value("profile.jpg"))
                .andExpect(jsonPath("$.data.filePath").value("/uploads/profile/profile.jpg"));
    }

    @Test
    @DisplayName("TC-FILE-CUSTOM-002: 공연 이미지 업로드 성공")
    void shouldUploadPerformanceImageSuccessfully() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "performance.jpg",
                "image/jpeg",
                "fake-image-data".getBytes()
        );

        FileUploadResponseDto responseDto = FileUploadResponseDto.builder()
                .fileName("performance.jpg")
                .filePath("/uploads/performance/performance.jpg")
                .fileSize(15)
                .fileType("image/jpeg")
                .build();

        when(fileService.uploadUserFile(any(), eq(MEMBER_ID), eq("performance"))).thenReturn(responseDto);

        // When & Then
        mockMvc.perform(multipart("/api/v1/files/custom-image")
                        .file(file)
                        .param("imageType", "performance")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileName").value("performance.jpg"))
                .andExpect(jsonPath("$.data.filePath").value("/uploads/performance/performance.jpg"));
    }

    @Test
    @DisplayName("TC-FILE-CUSTOM-003: 이미지 업로드 실패 - 잘못된 파일 타입")
    void shouldFailWhenUploadingNonImageFile() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "Hello, World!".getBytes()
        );

        // When & Then
        mockMvc.perform(multipart("/api/v1/files/custom-image")
                        .file(file)
                        .param("imageType", "profile")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("TC-FILE-CUSTOM-004: 이미지 업로드 실패 - 잘못된 이미지 타입")
    void shouldFailWhenUploadingWithInvalidImageType() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "fake-image-data".getBytes()
        );

        // When & Then
        mockMvc.perform(multipart("/api/v1/files/custom-image")
                        .file(file)
                        .param("imageType", "invalid")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isInternalServerError());
    }

    // ===== 커스텀 이미지 다운로드 테스트 =====

    @Test
    @DisplayName("TC-FILE-PRESIGNED-001: 프로필 이미지 다운로드 URL 생성 성공")
    void shouldGenerateProfileImageDownloadUrlSuccessfully() throws Exception {
        // Given
        String fileName = "profile.jpg";
        String imageType = "profile";
        String downloadUrl = "https://example.com/presigned-url";

        when(fileService.generatePreSignedUrl(eq(fileName), eq(MEMBER_ID))).thenReturn(downloadUrl);

        // When & Then
        mockMvc.perform(get("/api/v1/files/custom-image/download")
                        .param("fileName", fileName)
                        .param("imageType", imageType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.downloadUrl").value(downloadUrl))
                .andExpect(jsonPath("$.data.fileName").value(fileName))
                .andExpect(jsonPath("$.data.imageType").value(imageType))
                .andExpect(jsonPath("$.data.message").value("다운로드 URL이 생성되었습니다."));
    }

    @Test
    @DisplayName("TC-FILE-PRESIGNED-002: 공연 이미지 다운로드 URL 생성 성공")
    void shouldGeneratePerformanceImageDownloadUrlSuccessfully() throws Exception {
        // Given
        String fileName = "performance.jpg";
        String imageType = "performance";
        String downloadUrl = "https://example.com/presigned-url";

        when(fileService.generatePreSignedUrl(eq(fileName), eq(MEMBER_ID))).thenReturn(downloadUrl);

        // When & Then
        mockMvc.perform(get("/api/v1/files/custom-image/download")
                        .param("fileName", fileName)
                        .param("imageType", imageType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.downloadUrl").value(downloadUrl))
                .andExpect(jsonPath("$.data.fileName").value(fileName))
                .andExpect(jsonPath("$.data.imageType").value(imageType));
    }

    @Test
    @DisplayName("TC-FILE-PRESIGNED-003: 다운로드 URL 생성 실패")
    void shouldFailWhenGeneratingDownloadUrl() throws Exception {
        // Given
        String fileName = "nonexistent.jpg";
        String imageType = "profile";

        when(fileService.generatePreSignedUrl(eq(fileName), eq(MEMBER_ID)))
                .thenThrow(new RuntimeException("파일을 찾을 수 없습니다"));

        // When & Then
        mockMvc.perform(get("/api/v1/files/custom-image/download")
                        .param("fileName", fileName)
                        .param("imageType", imageType))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("서버 내부 오류가 발생했습니다."));
    }

    // ===== 채팅 파일 다운로드 테스트 =====

    @Test
    @DisplayName("TC-FILE-CHAT-001: 채팅 파일 다운로드 URL 생성 성공")
    void shouldGenerateChatFileDownloadUrlSuccessfully() throws Exception {
        // Given
        String fileName = "chat-file.txt";
        String downloadUrl = "https://example.com/presigned-url";

        when(fileService.generatePreSignedUrl(eq(fileName))).thenReturn(downloadUrl);

        // When & Then
        mockMvc.perform(get("/api/v1/files/chat/download")
                        .param("fileName", fileName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.downloadUrl").value(downloadUrl))
                .andExpect(jsonPath("$.data.fileName").value(fileName))
                .andExpect(jsonPath("$.data.message").value("채팅 파일 다운로드 URL이 생성되었습니다."));
    }

    @Test
    @DisplayName("TC-FILE-CHAT-002: 채팅 파일 다운로드 URL 생성 실패")
    void shouldFailWhenGeneratingChatFileDownloadUrl() throws Exception {
        // Given
        String fileName = "nonexistent.txt";

        when(fileService.generatePreSignedUrl(eq(fileName)))
                .thenThrow(new RuntimeException("채팅 파일을 찾을 수 없습니다"));

        // When & Then
        mockMvc.perform(get("/api/v1/files/chat/download")
                        .param("fileName", fileName))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("서버 내부 오류가 발생했습니다."));
    }

    @Test
    @DisplayName("TC-FILE-CHAT-003: 채팅 파일 다운로드 - 파일명 누락")
    void shouldFailWhenFileNameIsMissing() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/files/chat/download"))
                .andExpect(status().isInternalServerError());
    }
}
