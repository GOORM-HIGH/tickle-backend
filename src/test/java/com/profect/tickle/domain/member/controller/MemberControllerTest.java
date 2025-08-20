package com.profect.tickle.domain.member.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.profect.tickle.domain.chat.config.ChatJwtAuthenticationInterceptor;
import com.profect.tickle.domain.member.dto.request.CreateMemberRequestDto;
import com.profect.tickle.domain.member.dto.request.CreateMemberServiceRequestDto;
import com.profect.tickle.domain.member.entity.MemberRole;
import com.profect.tickle.domain.member.service.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;


@WebMvcTest(MemberController.class)
@AutoConfigureMockMvc(addFilters = false) // 시큐리티 필터 비활성화
@DisabledInAotMode // 이 테스트는 AOT 코드 생성에서 건너뛰고, 평소처럼 JVM에서만 실행해줘
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MemberService smtpMailSender;

    @MockBean
    private ChatJwtAuthenticationInterceptor chatJwtAuthenticationInterceptor;

    @BeforeEach
    void setUp() throws Exception {
        given(chatJwtAuthenticationInterceptor.preHandle(any(), any(), any())).willReturn(true);
    }

    @Test
    @DisplayName("이메일, 비밀번호, 닉네임, 생년월일, 전화번호, 권한을 입력하고 일반 회원가입 성공")
    void TC_SIGNUP_001() throws Exception {
        // given
        Instant birthday = Instant.parse("1981-11-04T00:00:00Z");

        CreateMemberRequestDto request = createMemberRequest(
                "user@example.com", "pw1234!", birthday,
                "닉네임", null, "01012345678", MemberRole.MEMBER,
                null, null, null, null,
                null, null, null, null,
                null
        );
        String payload = objectMapper.writeValueAsString(request);

        BDDMockito.willDoNothing()
                .given(smtpMailSender)
                .createMember(Mockito.any(CreateMemberServiceRequestDto.class));

        // when
        MvcResult result = mockMvc.perform(post("/api/v1/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andDo(print())
                .andReturn();

        // then
        then(smtpMailSender).should().createMember(any(CreateMemberServiceRequestDto.class));
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("이메일, 비밀번호, 닉네임, 생년월일, 전화번호, 권한, 사업자 관련 필드들을 입력하고 사업자 회원가입 성공")
    void TC_SIGNUP_002() throws Exception {
        // given
        Instant birthday = Instant.parse("1981-11-04T00:00:00Z");
        BigDecimal hostContractCharge = new BigDecimal("0.01");
        CreateMemberRequestDto request = createMemberRequest(
                "user@example.com", "pw1234!", birthday,
                "닉네임", null, "01012345678", MemberRole.HOST,
                "4981401407", "김판매", "공연단단해지기", "서울특별시 마포구 덕우빌딩 123",
                "12345-서울마포-123",
                "KB국민은행", "김판매", "12312312312",
                hostContractCharge
        );
        String payload = objectMapper.writeValueAsString(request);

        BDDMockito.willDoNothing()
                .given(smtpMailSender)
                .createMember(Mockito.any(CreateMemberServiceRequestDto.class));

        // when
        MvcResult result = mockMvc.perform(post("/api/v1/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andDo(print())
                .andReturn();

        // then
        then(smtpMailSender).should().createMember(any(CreateMemberServiceRequestDto.class));
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
    }

    private CreateMemberRequestDto createMemberRequest(
            String email,
            String password,
            Instant birthday,
            String nickname,
            String img,
            String phoneNumber,
            MemberRole role,
            String hostBizNumber,
            String hostBizCeoName,
            String hostBizName,
            String hostBizAddress,
            String hostBizEcommerceRegistrationNumber,
            String hostBizBankName,
            String hostBizDepositor,
            String hostBizBankNumber,
            BigDecimal hostContractCharge
    ) {
        return new CreateMemberRequestDto(
                email, password, birthday, nickname, img, phoneNumber, role,
                hostBizNumber, hostBizCeoName, hostBizName, hostBizAddress,
                hostBizEcommerceRegistrationNumber, hostBizBankName,
                hostBizDepositor, hostBizBankNumber, hostContractCharge
        );
    }
}
