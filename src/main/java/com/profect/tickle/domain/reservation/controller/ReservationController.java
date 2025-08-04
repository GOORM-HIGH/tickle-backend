package com.profect.tickle.domain.reservation.controller;

import com.profect.tickle.domain.reservation.dto.response.SeatInfoResponse;
import com.profect.tickle.domain.reservation.service.SeatService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reservation")
public class ReservationController {

    private final SeatService seatService;

    @GetMapping("/{performanceId}/seats")
    public ResponseEntity<List<SeatInfoResponse>> getPerformanceSeats(
            @PathVariable Long performanceId) {

        List<SeatInfoResponse> seats = seatService.getSeatInfoListByPerformance(performanceId);
        return ResponseEntity.ok(seats);
    }
}
