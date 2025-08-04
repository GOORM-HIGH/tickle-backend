package com.profect.tickle.global.status.service;

import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.status.Status;
import com.profect.tickle.global.status.repository.StatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StatusService {

    private final StatusRepository statusRepository;

    @Transactional(readOnly = true)
    public Status getReadStatusForNotification() {
        long readStatusId = 1L;
        return statusRepository.findById(readStatusId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STATUS_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Status getReadYetStatusForNotification() {
        long readStatusId = 2;
        return statusRepository.findById(readStatusId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STATUS_NOT_FOUND));
    }

    private Status getStatusByDate(LocalDateTime performanceDate) {
        LocalDateTime now = LocalDateTime.now();
        short code;

        if (performanceDate.isAfter(now)) {
            code = 100;
        } else if (performanceDate.toLocalDate().isEqual(now.toLocalDate())) {
            code = 101;
        } else {
            code = 102;
        }

        return statusRepository.findByTypeAndCode("공연", code)
                .orElseThrow(() -> new IllegalStateException("공연 상태코드가 존재하지 않습니다: " + code));
    }
}

