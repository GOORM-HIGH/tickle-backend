package com.profect.tickle.domain.file;

import com.profect.tickle.domain.chat.dto.request.ChatMessageSendRequestDto;
import com.profect.tickle.domain.chat.service.ChatMessageService;

import com.profect.tickle.domain.file.service.FileService;
import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.entity.MemberRole;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.testsecurity.WithMockMember;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 파일 업로드/다운로드 기능 통합 테스트
 * 
 * 테스트 범위:
 * - 파일 업로드 → 채팅 메시지에 첨부 → 다운로드의 전체 플로우
 * - 다양한 파일 타입과 크기 처리
 * - 파일 관련 에러 처리
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("파일 업로드/다운로드 기능 통합 테스트")
class FileIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FileService fileService;

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private MemberRepository memberRepository;

    private Member testMember;
    private Long chatRoomId;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성
        testMember = Member.builder()
                .email("filetest@example.com")
                .nickname("파일테스트사용자")
                .phoneNumber("01099999999")
                .password("encodedPassword999")
                .memberRole(MemberRole.MEMBER)
                .build();
        testMember = memberRepository.save(testMember);

        // 테스트용 채팅방 ID 설정 (실제로는 채팅방 생성 후 얻어야 함)
        chatRoomId = 1L;
    }

    @Test
    @WithMockMember(id = 6, email = "ahn3931@naver.com", roles = {"HOST"})
    @DisplayName("TC-INTEGRATION-001: 파일 업로드 → 채팅 메시지 첨부 → 다운로드 전체 플로우")
    void shouldCompleteFullFileFlow() throws Exception {
        // 1단계: 파일 업로드
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-image.jpg",
                "image/jpeg",
                "테스트 이미지 파일 내용".getBytes(StandardCharsets.UTF_8)
        );

        String uploadResponse = mockMvc.perform(multipart("/api/files/upload")
                        .file(file)
                        .param("category", "CHAT")
                        .param("description", "통합 테스트용 이미지 파일"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("파일 업로드 성공"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 응답에서 파일 정보 추출
        String fileName = objectMapper.readTree(uploadResponse).get("data").get("fileName").asText();
        String filePath = objectMapper.readTree(uploadResponse).get("data").get("filePath").asText();

        // 2단계: 채팅 메시지에 파일 첨부
        ChatMessageSendRequestDto messageRequest = ChatMessageSendRequestDto.builder()
                .content("파일이 첨부된 메시지입니다.")
                .filePath(filePath)
                .fileName(fileName)
                .build();

        mockMvc.perform(post("/api/chat/rooms/{chatRoomId}/messages", chatRoomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(messageRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("메시지 전송 성공"));

        // 3단계: 파일 다운로드 URL 생성
        mockMvc.perform(get("/api/files/download/{fileName}", fileName)
                        .param("filePath", filePath))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("다운로드 URL 생성 성공"))
                .andExpect(jsonPath("$.data.downloadUrl").exists());
    }

    @Test
    @WithMockMember(id = 6, email = "ahn3931@naver.com", roles = {"HOST"})
    @DisplayName("TC-INTEGRATION-002: 다양한 파일 타입 처리")
    void shouldHandleVariousFileTypes() throws Exception {
        // 1단계: 이미지 파일 업로드
        MockMultipartFile imageFile = new MockMultipartFile(
                "file",
                "test-image.png",
                "image/png",
                "PNG 이미지 파일 내용".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/files/upload")
                        .file(imageFile)
                        .param("category", "CHAT")
                        .param("description", "PNG 이미지 파일"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.fileName").value("test-image.png"));

        // 2단계: 문서 파일 업로드
        MockMultipartFile documentFile = new MockMultipartFile(
                "file",
                "test-document.pdf",
                "application/pdf",
                "PDF 문서 파일 내용".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/files/upload")
                        .file(documentFile)
                        .param("category", "CHAT")
                        .param("description", "PDF 문서 파일"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.fileName").value("test-document.pdf"));

        // 3단계: 텍스트 파일 업로드
        MockMultipartFile textFile = new MockMultipartFile(
                "file",
                "test-text.txt",
                "text/plain",
                "텍스트 파일 내용입니다.".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/files/upload")
                        .file(textFile)
                        .param("category", "CHAT")
                        .param("description", "텍스트 파일"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.fileName").value("test-text.txt"));
    }

    @Test
    @WithMockMember(id = 6, email = "ahn3931@naver.com", roles = {"HOST"})
    @DisplayName("TC-INTEGRATION-003: 파일 크기 및 용량 제한 처리")
    void shouldHandleFileSizeAndCapacityLimits() throws Exception {
        // 1단계: 정상 크기 파일 업로드
        byte[] normalFileContent = new byte[1024]; // 1KB
        MockMultipartFile normalFile = new MockMultipartFile(
                "file",
                "normal-file.txt",
                "text/plain",
                normalFileContent
        );

        mockMvc.perform(multipart("/api/files/upload")
                        .file(normalFile)
                        .param("category", "CHAT")
                        .param("description", "정상 크기 파일"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(201));

        // 2단계: 큰 파일 업로드 (용량 제한 테스트)
        byte[] largeFileContent = new byte[10 * 1024 * 1024]; // 10MB
        MockMultipartFile largeFile = new MockMultipartFile(
                "file",
                "large-file.dat",
                "application/octet-stream",
                largeFileContent
        );

        mockMvc.perform(multipart("/api/files/upload")
                        .file(largeFile)
                        .param("category", "CHAT")
                        .param("description", "큰 파일"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(201));
    }

    @Test
    @WithMockMember(id = 6, email = "ahn3931@naver.com", roles = {"HOST"})
    @DisplayName("TC-INTEGRATION-004: 파일 관련 에러 처리")
    void shouldHandleFileRelatedErrors() throws Exception {
        // 1단계: 파일이 없는 업로드 요청
        mockMvc.perform(multipart("/api/files/upload")
                        .param("category", "CHAT")
                        .param("description", "파일이 없는 요청"))
                .andExpect(status().isBadRequest());

        // 2단계: 지원하지 않는 파일 타입
        MockMultipartFile unsupportedFile = new MockMultipartFile(
                "file",
                "test-file.exe",
                "application/x-executable",
                "실행 파일 내용".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/files/upload")
                        .file(unsupportedFile)
                        .param("category", "CHAT")
                        .param("description", "지원하지 않는 파일 타입"))
                .andExpect(status().isBadRequest());

        // 3단계: 존재하지 않는 파일 다운로드
        mockMvc.perform(get("/api/files/download/nonexistent-file.txt")
                        .param("filePath", "/invalid/path"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockMember(id = 6, email = "ahn3931@naver.com", roles = {"HOST"})
    @DisplayName("TC-INTEGRATION-005: 파일 카테고리별 처리")
    void shouldHandleFilesByCategory() throws Exception {
        // 1단계: 채팅용 파일 업로드
        MockMultipartFile chatFile = new MockMultipartFile(
                "file",
                "chat-image.jpg",
                "image/jpeg",
                "채팅용 이미지".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/files/upload")
                        .file(chatFile)
                        .param("category", "CHAT")
                        .param("description", "채팅용 이미지"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.category").value("CHAT"));

        // 2단계: 커스텀 파일 업로드
        MockMultipartFile customFile = new MockMultipartFile(
                "file",
                "custom-document.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "커스텀 문서".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/files/upload")
                        .file(customFile)
                        .param("category", "CUSTOM")
                        .param("description", "커스텀 문서"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.category").value("CUSTOM"));
    }
}
