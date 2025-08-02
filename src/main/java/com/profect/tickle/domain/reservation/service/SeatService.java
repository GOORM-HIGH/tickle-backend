package com.profect.tickle.domain.reservation.service;

import com.profect.tickle.domain.performance.entity.HallType;
import com.profect.tickle.domain.performance.entity.Performance;
import com.profect.tickle.domain.performance.repository.PerformanceRepository;
import com.profect.tickle.domain.reservation.entity.Seat;
import com.profect.tickle.domain.reservation.entity.SeatTemplate;
import com.profect.tickle.domain.reservation.repository.SeatRepository;
import com.profect.tickle.domain.reservation.repository.SeatTemplateRepository;
import com.profect.tickle.global.status.Status;
import com.profect.tickle.global.status.repository.StatusRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SeatService {

    private final PerformanceRepository performanceRepository;
    private final SeatTemplateRepository seatTemplateRepository;
    private final StatusRepository statusRepository;
    private final SeatRepository seatRepository;

    public void createSeatsForPerformance(Long performanceId) {

        // 해당 공연의 유형 찾아오기
        HallType hallType = findHallTypeByPerformanceId(performanceId);

        // 해당 공연 유형에 맞는 좌석 템플릿 찾아오기
        List<SeatTemplate> seatTemplates = findSeatTemplatesByHallType(hallType);

        // 좌석에 넣을 해당 공연 찾아오기
        Performance performance = findPerformanceById(performanceId);

        // 예매가능 상태 찾아오기
        Status available = findAvailableStatus();

        List<Seat> seats = createSeats(seatTemplates, performance, available);
        seatRepository.saveAll(seats);
    }

    private Performance findPerformanceById(Long performanceId) {
        return performanceRepository.findById(performanceId)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("ID가 %d인 공연을 찾을 수 없습니다.", performanceId)));
    }

    /**
     * 공연 ID로 홀 타입을 조회합니다.
     */
    private HallType findHallTypeByPerformanceId(Long performanceId) {
        HallType hallType = performanceRepository.findHallTypeById(performanceId);
        if (hallType == null) {
            throw new IllegalStateException(
                    String.format("공연 ID %d에 대한 홀 타입을 찾을 수 없습니다.", performanceId));
        }
        return hallType;
    }

    /**
     * 홀 타입에 맞는 좌석 템플릿들을 조회합니다.
     */
    private List<SeatTemplate> findSeatTemplatesByHallType(HallType hallType) {
        List<SeatTemplate> seatTemplates = seatTemplateRepository.findByHallType(hallType);
        if (seatTemplates.isEmpty()) {
            throw new IllegalStateException(
                    String.format("홀 타입 %s에 대한 좌석 템플릿을 찾을 수 없습니다.", hallType));
        }
        return seatTemplates;
    }

    /**
     * 예매 가능 상태를 조회합니다.
     */
    private Status findAvailableStatus() {
        return statusRepository.findById(SeatStatus.AVAILABLE.getId())
                .orElseThrow(() -> new IllegalStateException(
                        String.format("ID가 %d인 좌석 예매 가능 상태를 찾을 수 없습니다.",
                                SeatStatus.AVAILABLE.getId())));
    }

    /**
     * 좌석 템플릿을 기반으로 실제 좌석들을 생성합니다.
     */
    private List<Seat> createSeats(
            List<SeatTemplate> seatTemplates,
            Performance performance,
            Status availableStatus) {

        Instant now = Instant.now();

        return seatTemplates.stream()
                .map(template -> Seat.builder()
                        .performance(performance)
                        .seatNumber(template.getSeatNumber())
                        .seatGrade(template.getSeatGrade())
                        .seatPrice(template.getPrice())
                        .status(availableStatus)
                        .createdAt(now)
                        .build())
                .toList();
    }
}