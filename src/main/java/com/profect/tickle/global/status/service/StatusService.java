package com.profect.tickle.global.status.service;

import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.status.Status;
import com.profect.tickle.global.status.repository.StatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class StatusService {

    public static final long NOTIFICATION_READ_YET_STATUS = 7L;
    public static final long NOTIFICATION_READ_STATUS = 8L;

    private final StatusRepository statusRepository;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Transactional(readOnly = true)
    public Status getStatusByDate(Instant performanceDate) {
        LocalDate perf = performanceDate.atZone(KST).toLocalDate();
        LocalDate today = Instant.now().atZone(KST).toLocalDate();

        short code;
        if (perf.isAfter(today)) {
            code = 100; // 공연 예정
        } else if (perf.isEqual(today)) {
            code = 101; // 공연 진행(당일)
        } else {
            code = 102; // 공연 완료
        }

        return statusRepository.findByTypeAndCode("공연", code)
                .orElseThrow(() -> new IllegalStateException("공연 상태코드가 존재하지 않습니다: " + code));
    }
}

