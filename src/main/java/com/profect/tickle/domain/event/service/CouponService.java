package com.profect.tickle.domain.event.service;

import com.profect.tickle.domain.event.dto.response.CouponResponseDto;
import com.profect.tickle.domain.event.mapper.CouponReceivedMapper;
import com.profect.tickle.domain.member.entity.CouponReceived;
import com.profect.tickle.domain.member.repository.CouponReceivedRepository;
import com.profect.tickle.global.status.Status;
import com.profect.tickle.global.status.repository.StatusRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponService {

    private final CouponReceivedMapper couponReceivedMapper;
    private final CouponReceivedRepository couponReceivedRepository;
    private final StatusRepository statusRepository;

    public List<CouponResponseDto> getAvailableCoupons(Long memberId, Integer totalAmount) {
        return couponReceivedMapper
                .findMyCoupons(memberId, totalAmount, Integer.MAX_VALUE);
    }

    @Transactional
    public void useCoupon(Long couponId, Long memberId) {
        CouponReceived couponReceived = couponReceivedRepository
                .findByCouponIdAndMemberIdAndNotUsed(couponId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("사용할 수 없는 쿠폰입니다."));

        // 쿠폰 사용 처리
        makeCouponUsed(couponReceived);

        couponReceivedRepository.save(couponReceived);
    }

    public Integer calculateCouponDiscount(Long couponId, Long memberId, Integer totalAmount) {
        CouponReceived couponReceived = couponReceivedRepository
                .findByCouponIdAndMemberIdAndNotUsed(couponId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("사용할 수 없는 쿠폰입니다."));

        return calculateDiscountAmount(totalAmount, couponReceived.getCoupon().getRate());
    }

    private void makeCouponUsed(CouponReceived couponReceived) {
        Status usedStatus = statusRepository.findById(18L)
                .orElseThrow();

        couponReceived.setCouponStatusToUsed(usedStatus);
    }

    private Integer calculateDiscountAmount(Integer totalAmount, Short discountRate) {
        return (int) (totalAmount * discountRate / 100.0);
    }
}
