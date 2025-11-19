/*
package com.profect.tickle.global.redis.util;

import com.profect.tickle.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static com.profect.tickle.global.exception.ErrorCode.*;

@Slf4j
@Aspect
@Component
@Profile("!test")
@RequiredArgsConstructor
public class ConcurrencyGuardAspect {

    private final RedissonClient redissonClient;
    private final TransactionAspect transactionAspect;

    @Around("@annotation(concurrencyGuard) && (args(..))")
    public Object handleConcurrency(ProceedingJoinPoint joinPoint, ConcurrencyGuard concurrencyGuard) throws Throwable {
        String lockName = buildLockName(joinPoint, concurrencyGuard.lockName());
        RLock lock = redissonClient.getLock(lockName);

        try {
            boolean acquired = lock.tryLock(concurrencyGuard.waitTime(), concurrencyGuard.leaseTime(), concurrencyGuard.timeUnit());
            if (!acquired) {
                throw new BusinessException(INTERNAL_SERVER_ERROR);
            }

            return transactionAspect.proceed(joinPoint);
        } finally {
            try {
                lock.unlock();
            } catch (IllegalMonitorStateException e) {
                log.warn("Redisson 락 이미 해제됨: {}", lockName);
            }
        }
    }

    private String buildLockName(ProceedingJoinPoint joinPoint, String prefix) {
        Object[] args = joinPoint.getArgs();
        String key = args.length > 0 ? args[0].toString() : "default";
        return String.format("lock:%s:%s", prefix, key);
    }
}*/
