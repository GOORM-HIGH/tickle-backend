package com.profect.tickle.domain.member.controller;

import com.profect.tickle.domain.member.dto.request.CreateMemberRequestDto;
import com.profect.tickle.domain.member.dto.request.EmailRequestDto;
import com.profect.tickle.domain.member.service.MemberService;
import com.profect.tickle.global.response.ResultCode;
import com.profect.tickle.global.response.ResultResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "회원관리", description = "회원관리 API")
public class MemberController {

    private final MemberService memberService;

    @PostMapping(value = "/sign-up")
    @Operation(summary = "회원가입", description = "회원가입")
    public ResultResponse<?> signup(@RequestBody CreateMemberRequestDto createUserRequest) {
        log.info("회원가입 요청: {}", createUserRequest);

        memberService.createMember(createUserRequest);

        return new ResultResponse<>(
                ResultCode.MEMBER_CREATE_SUCCESS,
                ResultCode.MEMBER_CREATE_SUCCESS.getMessage()
        );
    }

    @PostMapping(value = "/auth/email-verification")
    @Operation(summary = "이메일 인증번호 생성", description = "이메일 인증번호를 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "이메일 인증번호 생성 완료"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (이메일 형식 오류 등)"),
            @ApiResponse(responseCode = "409", description = "이미 가입된 회원"),
            @ApiResponse(responseCode = "429", description = "인증번호를 너무 자주 요청함 (쿨타임 미충족)")
    })
    public ResultResponse<?> emailVerification(@RequestBody EmailRequestDto email) {
        log.info("인증번호 발송 email: {}", email.email());

        memberService.createEmailValidationCode(email.email());

        return new ResultResponse<>(
                ResultCode.EMAIL_VALIDATION_CODE_CREATE,
                ResultCode.EMAIL_VALIDATION_CODE_CREATE.getMessage()
        );
    }
}
