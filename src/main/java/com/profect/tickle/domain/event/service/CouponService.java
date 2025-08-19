package com.profect.tickle.domain.event.service;

import com.profect.tickle.domain.event.dto.response.CouponListResponseDto;
import com.profect.tickle.domain.event.dto.response.CouponResponseDto;
import com.profect.tickle.domain.event.mapper.CouponMapper;
import com.profect.tickle.domain.event.mapper.CouponReceivedMapper;
import com.profect.tickle.domain.member.entity.CouponReceived;
import com.profect.tickle.domain.member.repository.CouponReceivedRepository;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.status.Status;
import com.profect.tickle.global.status.StatusIds.Coupon;
import com.profect.tickle.global.status.service.StatusProvider;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponService {

    private final CouponReceivedMapper couponReceivedMapper;
    private final CouponMapper couponMapper;
    private final CouponReceivedRepository couponReceivedRepository;
    private final StatusProvider statusProvider;

    public List<CouponResponseDto> getAvailableCoupons(Long memberId) {
        return couponReceivedMapper.findMyCoupons(memberId, 10, 0);
    }

    @Transactional
    public void useCoupon(Long couponId, Long memberId) {
        CouponReceived couponReceived = findValidCoupon(couponId, memberId);

        // 쿠폰 사용 처리
        makeCouponUsed(couponReceived);

        couponReceivedRepository.save(couponReceived);
    }

    public Integer calculateCouponDiscount(Long couponId, Long memberId, Integer totalAmount) {
        CouponReceived couponReceived = findValidCoupon(couponId, memberId);

        return calculateDiscountAmount(totalAmount, couponReceived.getCoupon().getRate());
    }

    public CouponListResponseDto getSpecialCouponDetailById(Long couponId) {
        CouponListResponseDto dto = couponMapper.findCouponById(couponId);
        if (dto == null) throw new BusinessException(ErrorCode.COUPON_NOT_FOUND);

        return dto;
    }

    private CouponReceived findValidCoupon(Long couponId, Long memberId) {
        return couponReceivedRepository
                .findByCouponIdAndMemberIdAndNotUsed(couponId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("사용할 수 없는 쿠폰입니다."));
    }


    private void makeCouponUsed(CouponReceived couponReceived) {
        Status used = statusProvider.provide(Coupon.USED);
        couponReceived.setCouponStatusTo(used);
    }

    private Integer calculateDiscountAmount(Integer totalAmount, Short discountRate) {
        return (int) (totalAmount * discountRate / 100.0);
    }
}
