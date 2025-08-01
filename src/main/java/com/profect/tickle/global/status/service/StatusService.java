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

    private final StatusRepository statusRepository;

    @Transactional(readOnly = true)
    public Status getReadStatusForNotification() {
        int readStatusId = 1;
        return statusRepository.findById(readStatusId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STATUS_NOT_FOUND));
    }
}
