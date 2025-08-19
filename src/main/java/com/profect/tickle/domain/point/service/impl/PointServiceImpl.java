package com.profect.tickle.domain.point.service.impl;

import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.domain.point.dto.request.ChargePointRequestDto;
import com.profect.tickle.domain.point.dto.response.PointResponseDto;
import com.profect.tickle.domain.point.dto.response.PointSimpleResponseDto;
import com.profect.tickle.domain.point.entity.Point;
import com.profect.tickle.domain.point.repository.PointRepository;
import com.profect.tickle.domain.point.service.PointService;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.paging.PagingResponse;
import com.profect.tickle.global.security.util.SecurityUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointServiceImpl implements PointService {
    private final MemberRepository memberRepository;
    private final PointRepository pointRepository;

    @Transactional
    public PointResponseDto charge(ChargePointRequestDto request) {

        Member member = getMemberOrThrow();

        Point point = Point.charge(member, request.amount(), request.orderId());
        pointRepository.save(point);

        member.addPoint(request.amount());

        return PointResponseDto.from(point, request.orderId(), request.receiptId());
    }


    public PointSimpleResponseDto getCurrentPoint() {
        Member member = getMemberOrThrow();
        return PointSimpleResponseDto.from(member.getPointBalance());
    }

    public PagingResponse<PointSimpleResponseDto> getPointHistory(int page, int size) {
        Long memberId = SecurityUtil.getSignInMemberId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Point> pointsPage = pointRepository.findByMemberId(memberId, pageable);
        Page<PointSimpleResponseDto> dtoPage = pointsPage.map(PointSimpleResponseDto::from);
        return PagingResponse.from(dtoPage);
    }

    private Member getMemberOrThrow() {
        Long memberId = SecurityUtil.getSignInMemberId();
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
