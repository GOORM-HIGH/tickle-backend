package com.profect.tickle.domain.point.controller;

import com.profect.tickle.domain.point.dto.response.BootpayConfigResponseDto;
import com.profect.tickle.domain.point.dto.request.ChargePointRequestDto;
import com.profect.tickle.domain.point.dto.response.PointResponseDto;
import com.profect.tickle.domain.point.dto.response.PointSimpleResponseDto;
import com.profect.tickle.domain.point.service.BootPayService;
import com.profect.tickle.domain.point.service.PointService;
import com.profect.tickle.global.paging.PagingResponse;
import com.profect.tickle.global.response.ResultCode;
import com.profect.tickle.global.response.ResultResponse;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


@Tag(name = "포인트 결제", description = "외부 결제를 이용한 포인트 충전 관련 API입니다.")
@RestController
@RequestMapping("/api/v1/pay")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;
    private final BootPayService bootPayService;

    @Hidden // Bootpay Application ID가 도출되어 스웨거에 노출되지 않도록 설정하였습니다.
    @GetMapping("/bootpay-config")
    public BootpayConfigResponseDto getPayConfig() {
        return bootPayService.getBootpayConfig();
    }

    @Operation(summary = "포인트 충전",
            description = "Bootpay 결제 완료 후 클라이언트에서 결제 정보를 전달하면 포인트를 적립합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "결제 후 전달되는 포인트 충전 요청 데이터",
                    required = true,
                    content = @Content(schema = @Schema(implementation = ChargePointRequestDto.class))),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "포인트 충전 성공",
                            content = @Content(schema = @Schema(implementation = PointResponseDto.class))),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청 (예: 회원 없음, 값 누락 등)")})
    @PostMapping("/charge")
    public ResultResponse<PointResponseDto> chargePoint(@RequestBody ChargePointRequestDto request) {
        PointResponseDto dto = pointService.charge(request);
        return ResultResponse.of(ResultCode.POINT_CHARGE_SUCCESS,dto );
    }

    @Operation(summary = "보유 포인트 조회",
            description = "현재 로그인한 사용자의 보유 포인트를 반환합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "보유 포인트 조회 성공",
                            content = @Content(schema = @Schema(implementation = PointSimpleResponseDto.class)))})
    @GetMapping("/balance")
    public ResultResponse<PointSimpleResponseDto> getMyPointBalance() {
        PointSimpleResponseDto dto = pointService.getCurrentPoint();
        return ResultResponse.of(ResultCode.POINT_INFO_SUCCESS, dto);
    }

    @Operation(summary = "포인트 충전/사용 내역 조회",
            description = "현재 로그인한 사용자의 포인트 충전 및 차감 내역을 모두 반환합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "포인트 이력 조회 성공",
                            content = @Content(schema = @Schema(description = "PagingResponse<PointSimpleResponseDto> 구조의 응답")))})
    @GetMapping("/history")
    public ResultResponse<PagingResponse<PointSimpleResponseDto>> getMyPointHistory(@RequestParam(defaultValue = "0") int page,
                                                                                    @RequestParam(defaultValue = "10") int size) {

        PagingResponse<PointSimpleResponseDto> dto = pointService.getPointHistory(page, size);
        return ResultResponse.of(ResultCode.POINT_HISTORY_SUCCESS, dto);
    }

}
