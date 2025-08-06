package com.profect.tickle.domain.reservation.service;

import com.profect.tickle.domain.reservation.repository.SeatRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class SeatPreemptionReleaser {

    private final SeatRepository seatRepository;

    @Transactional
    public void releaseExpiredPreemptions() {
        Instant now = Instant.now();
        int count = seatRepository.clearExpiredPreemptionsBulk(now);

        if (count > 0) {
            log.info("벌크로 {}개 좌석 선점 해제", count);
        } else {
            log.debug("해제된 좌석 없음");
        }
    }
}
