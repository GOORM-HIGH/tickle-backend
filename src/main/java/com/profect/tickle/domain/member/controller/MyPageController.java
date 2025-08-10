package com.profect.tickle.domain.member.controller;

import com.profect.tickle.domain.event.dto.response.CouponResponseDto;
import com.profect.tickle.domain.event.service.EventService;
import com.profect.tickle.domain.member.dto.response.MemberResponseDto;
import com.profect.tickle.domain.member.service.MemberService;
import com.profect.tickle.global.paging.PagingResponse;
import com.profect.tickle.global.response.ResultCode;
import com.profect.tickle.global.response.ResultResponse;
import com.profect.tickle.global.security.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/mypage")
@Tag(name = "마이페이지", description = "마이페이지 관련 API입니다.")
@Slf4j
public class MyPageController {

    private final MemberService memberService;
    private final EventService eventService;

    @Operation(summary = "사용자 정보 조회", description = "마이페이지에 보여줄 로그인한 사용자의 정보를 조회합니다.")
    @GetMapping(value = "/my-page")
    public ResultResponse<?> getMyPage() {
        String signInMemberEmail = SecurityUtil.getSignInMemberEmail();
        log.info("{}님의 마이페이지 정보 조회 API요청이 수신되었습니다.", signInMemberEmail);

        MemberResponseDto data = memberService.getMemberDtoByEmail(signInMemberEmail);

        return new ResultResponse<>(
                ResultCode.MEMBER_MYPAGE_INFO_SUCCESS,
                data
        );
    }

    @GetMapping("/coupons")
    @Operation(summary = "내 쿠폰 목록 조회")
    public ResultResponse<PagingResponse<CouponResponseDto>> getMyCoupons(@RequestParam("page") int page,
                                                                          @RequestParam("size") int size) {
        PagingResponse<CouponResponseDto> response = eventService.getMyCoupons(page, size);
        return ResultResponse.of(ResultCode.COUPON_INFO_SUCCESS, response);
    }
}