package com.profect.tickle.domain.reservation.controller;

import com.profect.tickle.domain.reservation.dto.response.ReservationCancelResponse;
import com.profect.tickle.domain.reservation.dto.response.ReservationDetailResponse;
import com.profect.tickle.domain.reservation.dto.response.ReservationHistoryResponse;
import com.profect.tickle.domain.reservation.service.ReservationHistoryService;
import com.profect.tickle.global.security.util.SecurityUtil;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
public class ReservationHistoryController {

    private final ReservationHistoryService reservationHistoryService;

    // 예매 내역 목록 조회
    @GetMapping("/history")
    public ResponseEntity<List<ReservationHistoryResponse>> getReservationHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {

        Long userId = SecurityUtil.getSignInMemberId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        List<ReservationHistoryResponse> history = reservationHistoryService
                .getReservationHistory(userId, pageable);

        return ResponseEntity.ok(history);
    }

    // 예매 상세 정보 조회
    @GetMapping("/{reservationId}")
    public ResponseEntity<ReservationDetailResponse> getReservationDetail(
            @PathVariable Long reservationId) {

        Long userId = SecurityUtil.getSignInMemberId();

        ReservationDetailResponse detail = reservationHistoryService
                .getReservationDetail(reservationId, userId);

        return ResponseEntity.ok(detail);
    }

    // 예매 취소
    @DeleteMapping("/{reservationId}")
    public ResponseEntity<ReservationCancelResponse> cancelReservation(
            @PathVariable Long reservationId) {

        ReservationCancelResponse response = reservationHistoryService
                .cancelReservation(reservationId);

        return ResponseEntity.ok(response);
    }
}