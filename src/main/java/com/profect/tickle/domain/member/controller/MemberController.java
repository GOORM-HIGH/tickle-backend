package com.profect.tickle.domain.member.controller;

import com.profect.tickle.domain.member.dto.request.CreateMemberRequestDto;
import com.profect.tickle.domain.member.dto.request.EmailValidationCodeCreateRequest;
import com.profect.tickle.domain.member.dto.request.EmailValidationRequestDto;
import com.profect.tickle.domain.member.dto.response.MemberResponseDto;
import com.profect.tickle.domain.member.service.MemberService;
import com.profect.tickle.global.response.ResultCode;
import com.profect.tickle.global.response.ResultResponse;
import com.profect.tickle.global.security.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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

        log.info("회원가입 성공: {}", createUserRequest.getEmail());
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
    public ResultResponse<?> emailVerification(@RequestBody EmailValidationCodeCreateRequest email) {
        log.info("인증번호 발송 email: {}", email.email());

        memberService.createEmailAuthenticationCode(email.email());

        return new ResultResponse<>(
                ResultCode.EMAIL_VALIDATION_CODE_CREATE,
                ResultCode.EMAIL_VALIDATION_CODE_CREATE.getMessage()
        );
    }

    @Operation(summary = "이메일 인증번호 확인", description = "이메일과 인증코드를 검증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "인증번호 확인 성공"),
            @ApiResponse(responseCode = "400", description = "인증번호가 만료됨"),
            @ApiResponse(responseCode = "404", description = "잘못된 요청 (형식 오류, 빈 값 등)"),
            @ApiResponse(responseCode = "409", description = "이미 가입된 회원")
    })
    @PostMapping("/auth/email-verification/confirm")
    public ResultResponse<?> verifyEmailCode(@Valid @RequestBody EmailValidationRequestDto request) {
        log.info("email verification: {}", request);

        memberService.verifyEmailCode(request.email(), request.code());

        return new ResultResponse<> (
                ResultCode.EMAIL_VERIFICATION_SUCCESS,
                ResultCode.EMAIL_VERIFICATION_SUCCESS.getMessage()
        );
    }
}
