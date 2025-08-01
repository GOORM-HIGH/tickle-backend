package com.profect.tickle.domain.member.controller;

import com.profect.tickle.domain.member.dto.request.CreateMemberRequestDto;
import com.profect.tickle.domain.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<?> signup(@RequestBody CreateMemberRequestDto createUserRequest) {

        log.info("회원가입 요청: {}", createUserRequest);

        memberService.createMember(createUserRequest);

        return new ResponseEntity<>(HttpStatus.CREATED);
    }
}
