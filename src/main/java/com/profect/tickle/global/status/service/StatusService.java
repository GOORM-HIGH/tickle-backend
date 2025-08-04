package com.profect.tickle.global.status.service;

import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.status.Status;
import com.profect.tickle.global.status.repository.StatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StatusService {

    public static final long NOTIFICATION_READ_YET_STATUS = 7L;
    public static final long NOTIFICATION_READ_STATUS = 8L;

    private final StatusRepository statusRepository;

    @Transactional(readOnly = true)
    public Status getReadStatusForNotification() {
        return statusRepository.findById(NOTIFICATION_READ_STATUS)
                .orElseThrow(() -> new BusinessException(ErrorCode.STATUS_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Status getReadYetStatusForNotification() {
        return statusRepository.findById(NOTIFICATION_READ_YET_STATUS)
                .orElseThrow(() -> new BusinessException(ErrorCode.STATUS_NOT_FOUND));
    }
}
