package com.profect.tickle.domain.event.service.lock;

import com.profect.tickle.domain.event.entity.Coupon;
import com.profect.tickle.domain.event.entity.Event;
import com.profect.tickle.domain.event.repository.CouponRepository;
import com.profect.tickle.domain.event.repository.EventRepository;
import com.profect.tickle.domain.member.entity.CouponReceived;
import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.repository.CouponReceivedRepository;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.domain.reservation.entity.Seat;
import com.profect.tickle.domain.reservation.repository.SeatRepository;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.security.util.SecurityUtil;
import com.profect.tickle.global.status.Status;
import com.profect.tickle.global.status.StatusIds;
import com.profect.tickle.global.status.service.StatusProvider;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PessimisticEventApplyExecutor {

    private final SeatRepository seatRepository;
    private final CouponRepository couponRepository;
    private final CouponReceivedRepository couponReceivedRepository;
    private final EventRepository eventRepository;
    private final MemberRepository memberRepository;
    private final StatusProvider statusProvider;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void issueCouponOnce(Long eventId) {
        Event event = getEventOrThrow(eventId);
        validateCouponEventStatus(event);
        validateCouponEventStatus(event);
        checkQuantity(event);
        Member member = getMemberOrThrow();
        
        Coupon coupon = couponRepository.findForUpdateById(event.getCoupon().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));

        if (couponReceivedRepository.existsByMemberIdAndCouponId(member.getId(), coupon.getId())) {
            throw new BusinessException(ErrorCode.ALREADY_ISSUED_COUPON);
        }

        if (coupon.getCount() <= 0) throw new BusinessException(ErrorCode.COUPON_SOLD_OUT);

        Status available = statusProvider.provide(StatusIds.Coupon.AVAILABLE);

        try {
            couponReceivedRepository.save(CouponReceived.create(member, coupon, available));
        } catch (DataIntegrityViolationException dup) {
            throw new BusinessException(ErrorCode.ALREADY_ISSUED_COUPON);
        }

        coupon.decreaseCount();

        if (coupon.getCount() == 0) event.updateStatus(statusProvider.provide(StatusIds.Event.COMPLETED));
    }

    private static void checkQuantity(Event event) {
        if (StatusIds.Event.COMPLETED.equals(event.getStatus().getId())) {
            throw new BusinessException(ErrorCode.COUPON_SOLD_OUT);
        }
    }

    private Event getEventOrThrow(Long eventId) {
        return eventRepository.findAnyTypeForUpdateById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));
    }

    private Seat getSeatOrThrow(Long eventSeatId) {
        return seatRepository.findById(eventSeatId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SEAT_NOT_FOUND));
    }

    private Member getMemberOrThrow() {
        Long memberId = SecurityUtil.getSignInMemberId();
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private void validateCouponEventStatus(Event event) {
        if (StatusIds.Event.COMPLETED.equals(event.getStatus().getId())) {
            throw new BusinessException(ErrorCode.COUPON_SOLD_OUT);
        }
        if (!StatusIds.Event.IN_PROGRESS.equals(event.getStatus().getId())) {
            throw new BusinessException(ErrorCode.EVENT_NOT_IN_PROGRESS);
        }
    }
}