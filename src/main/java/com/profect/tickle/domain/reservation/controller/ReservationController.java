package com.profect.tickle.domain.reservation.controller;

import com.profect.tickle.domain.reservation.dto.request.ReservationCompletionRequestDto;
import com.profect.tickle.domain.reservation.dto.request.SeatPreemptionRequestDto;
import com.profect.tickle.domain.reservation.dto.response.reservation.ReservationCompletionResponseDto;
import com.profect.tickle.domain.reservation.dto.response.reservation.ReservationInfoResponseDto;
import com.profect.tickle.domain.reservation.dto.response.reservation.SeatInfoResponseDto;
import com.profect.tickle.domain.reservation.dto.response.preemption.SeatPreemptionResponseDto;
import com.profect.tickle.domain.reservation.service.ReservationInfoService;
import com.profect.tickle.domain.reservation.service.ReservationService;
import com.profect.tickle.domain.reservation.service.SeatPreemptionService;
import com.profect.tickle.domain.reservation.service.SeatService;
import com.profect.tickle.global.response.ResultCode;
import com.profect.tickle.global.response.ResultResponse;
import com.profect.tickle.global.security.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "예매", description = "공연 예매 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reservation")
public class ReservationController {

    private final SeatService seatService;
    private final SeatPreemptionService seatPreemptionService;
    private final ReservationInfoService reservationInfoService;
    private final ReservationService reservationService;

    @Operation(summary = "공연 좌석 조회", description = "해당 공연의 좌석 정보를 조회합니다.")
    @GetMapping("/{performanceId}/seats")
    public ResultResponse<List<SeatInfoResponseDto>> getPerformanceSeats(
            @PathVariable Long performanceId) {

        List<SeatInfoResponseDto> seats = seatService.getSeatInfoListByPerformance(performanceId);
        return ResultResponse.of(ResultCode.RESERVATION_SEATS_INFO_SUCCESS, seats);
    }

    @Operation(summary = "좌석 선점", description = "예매를 위한 좌석 선점을 수행합니다.")
    @PostMapping("/preempt")
    @PreAuthorize("hasRole('MEMBER')")
    public ResultResponse<SeatPreemptionResponseDto> preemptSeats(
            @RequestBody @Valid SeatPreemptionRequestDto request) {

        Long userId = SecurityUtil.getSignInMemberId();
        SeatPreemptionResponseDto response = seatPreemptionService.preemptSeats(request, userId);
        return ResultResponse.of(ResultCode.RESERVATION_SEAT_PREEMPTION_SUCCESS, response);
    }

    @Operation(summary = "결제 정보 조회", description = "선점된 좌석의 결제 정보를 조회합니다.")
    @GetMapping("/payment-info/{preemptionToken}")
    @PreAuthorize("hasRole('MEMBER')")
    public ResultResponse<ReservationInfoResponseDto> getPaymentInfo(
            @PathVariable String preemptionToken) {

        ReservationInfoResponseDto response = reservationInfoService.getReservationInfo(preemptionToken);
        return ResultResponse.of(ResultCode.RESERVATION_PAYMENT_INFO_SUCCESS, response);
    }

    @Operation(summary = "예매 완료", description = "결제를 통한 예매를 완료합니다.")
    @PostMapping("/complete")
    @PreAuthorize("hasRole('MEMBER')")
    public ResultResponse<ReservationCompletionResponseDto> completeReservation(
            @RequestBody @Valid ReservationCompletionRequestDto request) {

        ReservationCompletionResponseDto response = reservationService.completeReservation(request);
        return ResultResponse.of(ResultCode.RESERVATION_COMPLETE_SUCCESS, response);
    }
}
