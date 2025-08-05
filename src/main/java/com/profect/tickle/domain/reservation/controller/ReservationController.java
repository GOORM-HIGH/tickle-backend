package com.profect.tickle.domain.reservation.controller;

import com.profect.tickle.domain.reservation.dto.request.ReservationCompletionRequest;
import com.profect.tickle.domain.reservation.dto.request.SeatPreemptionRequest;
import com.profect.tickle.domain.reservation.dto.response.ReservationCompletionResponse;
import com.profect.tickle.domain.reservation.dto.response.ReservationInfoResponse;
import com.profect.tickle.domain.reservation.dto.response.SeatInfoResponse;
import com.profect.tickle.domain.reservation.dto.response.SeatPreemptionResponse;
import com.profect.tickle.domain.reservation.service.ReservationHistoryService;
import com.profect.tickle.domain.reservation.service.ReservationInfoService;
import com.profect.tickle.domain.reservation.service.ReservationService;
import com.profect.tickle.domain.reservation.service.SeatPreemptionService;
import com.profect.tickle.domain.reservation.service.SeatService;
import com.profect.tickle.global.security.util.SecurityUtil;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reservation")
public class ReservationController {

    private final SeatService seatService;
    private final SeatPreemptionService seatPreemptionService;
    private final ReservationInfoService reservationInfoService;
    private final ReservationService reservationService;

    @GetMapping("/{performanceId}/seats")
    public ResponseEntity<List<SeatInfoResponse>> getPerformanceSeats(
            @PathVariable Long performanceId) {

        List<SeatInfoResponse> seats = seatService.getSeatInfoListByPerformance(performanceId);
        return ResponseEntity.ok(seats);
    }

    @PostMapping("/preempt")
    public ResponseEntity<SeatPreemptionResponse> preemptSeats(
            @RequestBody @Valid SeatPreemptionRequest request) {

        Long userId = SecurityUtil.getSignInMemberId();
        SeatPreemptionResponse response = seatPreemptionService.preemptSeats(request, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/payment-info/{preemptionToken}")
    public ResponseEntity<ReservationInfoResponse> getPaymentInfo(
            @PathVariable String preemptionToken) {

        ReservationInfoResponse response = reservationInfoService.getReservationInfo(preemptionToken);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/complete")
    public ResponseEntity<ReservationCompletionResponse> completeReservation(
            @RequestBody @Valid ReservationCompletionRequest request) {

        ReservationCompletionResponse response = reservationService.completeReservation(request);
        return ResponseEntity.ok(response);
    }
}
