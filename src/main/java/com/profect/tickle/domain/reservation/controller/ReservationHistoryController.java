package com.profect.tickle.domain.reservation.controller;

import com.profect.tickle.domain.reservation.dto.response.reservation.ReservationCancelResponseDto;
import com.profect.tickle.domain.reservation.dto.response.reservation.ReservationDetailResponseDto;
import com.profect.tickle.domain.reservation.dto.response.reservation.ReservationHistoryResponseDto;
import com.profect.tickle.domain.reservation.service.ReservationHistoryService;
import com.profect.tickle.global.response.ResultCode;
import com.profect.tickle.global.response.ResultResponse;
import com.profect.tickle.global.security.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "예매 이력", description = "예매 이력 관리 관련 API")
@RestController
@RequestMapping("/api/v1/reservation")
@RequiredArgsConstructor
@Slf4j
public class ReservationHistoryController {

    private final ReservationHistoryService reservationHistoryService;

    @Operation(summary = "예매 내역 목록 조회", description = "사용자의 예매 내역을 페이징하여 조회합니다.")
    @GetMapping("/history")
    @PreAuthorize("hasRole('MEMBER')")
    public ResultResponse<List<ReservationHistoryResponseDto>> getReservationHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long statusId) {

        log.info("예매 내역 목록 조회 API 요청이 수신되었습니다.");

        Long userId = SecurityUtil.getSignInMemberId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        List<ReservationHistoryResponseDto> history = reservationHistoryService
                .getReservationHistoryWithStatus(userId, statusId, pageable);

        return ResultResponse.of(ResultCode.RESERVATION_HISTORY_SUCCESS, history);
    }

    @Operation(summary = "예매 상세 정보 조회", description = "특정 예매의 상세 정보를 조회합니다.")
    @GetMapping("/{reservationId}")
    @PreAuthorize("hasRole('MEMBER')")
    public ResultResponse<ReservationDetailResponseDto> getReservationDetail(
            @PathVariable Long reservationId) {

        Long userId = SecurityUtil.getSignInMemberId();

        ReservationDetailResponseDto detail = reservationHistoryService
                .getReservationDetail(reservationId, userId);

        return ResultResponse.of(ResultCode.RESERVATION_DETAIL_SUCCESS, detail);
    }

    @Operation(summary = "예매 취소", description = "예매를 취소합니다.")
    @DeleteMapping("/{reservationId}")
    @PreAuthorize("hasRole('MEMBER')")
    public ResultResponse<ReservationCancelResponseDto> cancelReservation(
            @PathVariable Long reservationId) {

        log.info("{}번 예매를 취소합니다.", reservationId);

        ReservationCancelResponseDto response = reservationHistoryService
                .cancelReservation(reservationId);

        return ResultResponse.of(ResultCode.RESERVATION_CANCEL_SUCCESS, response);
    }
}