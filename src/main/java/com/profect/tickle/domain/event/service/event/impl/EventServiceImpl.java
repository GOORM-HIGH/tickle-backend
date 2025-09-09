package com.profect.tickle.domain.event.service.event.impl;

import com.profect.tickle.domain.event.dto.request.CouponCreateRequestDto;
import com.profect.tickle.domain.event.dto.request.TicketEventCreateRequestDto;
import com.profect.tickle.domain.event.dto.response.*;
import com.profect.tickle.domain.event.entity.Coupon;
import com.profect.tickle.domain.event.entity.Event;
import com.profect.tickle.domain.event.entity.EventType;
import com.profect.tickle.domain.event.mapper.CouponMapper;
import com.profect.tickle.domain.event.mapper.CouponReceivedMapper;
import com.profect.tickle.domain.event.mapper.EventMapper;
import com.profect.tickle.domain.event.repository.CouponRepository;
import com.profect.tickle.domain.event.repository.EventRepository;
import com.profect.tickle.domain.event.service.event.EventService;
import com.profect.tickle.domain.event.service.message.publisher.EventPublisher;
import com.profect.tickle.domain.event.service.lock.PessimisticEventApplyExecutor;
import com.profect.tickle.domain.event.service.message.dto.TicketLockMessage;
import com.profect.tickle.domain.performance.entity.Performance;
import com.profect.tickle.domain.performance.repository.PerformanceRepository;
import com.profect.tickle.domain.point.entity.PointTarget;
import com.profect.tickle.domain.reservation.entity.Seat;
import com.profect.tickle.domain.reservation.repository.SeatRepository;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.paging.PagingResponse;
import com.profect.tickle.global.security.util.SecurityUtil;
import com.profect.tickle.global.status.Status;
import com.profect.tickle.global.status.StatusIds;
import com.profect.tickle.global.status.service.StatusProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    // utils
    private final PointTarget eventTarget = PointTarget.EVENT;
    private final Clock clock;
    private final ZoneId zone = ZoneId.systemDefault();

    // mapper & repositories
    private final PessimisticEventApplyExecutor pessimisticEventApplyExecutor;
    private final SeatRepository seatRepository;
    private final CouponRepository couponRepository;
    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final CouponMapper couponMapper;
    private final CouponReceivedMapper couponReceivedMapper;
    private final PerformanceRepository performanceRepository;
    private final StatusProvider statusProvider;
    private final EventPublisher eventPublisher;


    @Override
    @Transactional
    public CouponResponseDto createCouponEvent(CouponCreateRequestDto request) {
        if (couponRepository.existsByName(request.name()))
            throw new BusinessException(ErrorCode.DUPLICATE_COUPON_NAME);
        Coupon coupon = Coupon.create(
                request.name(),
                request.content(),
                request.count(),
                request.rate(),
                request.validDate()
        );
        couponRepository.save(coupon);

        Status status = statusProvider.provide(StatusIds.Event.SCHEDULED);
        Event event = Event.create(status, coupon, request.name());

        eventRepository.save(event);
        coupon.updateEvent(event);

        return CouponResponseDto.from(coupon);
    }

    @Override
    @Transactional
    public TicketEventResponseDto createTicketEvent(TicketEventCreateRequestDto request) {
        Status status = statusProvider.provide(StatusIds.Event.SCHEDULED);
        Seat seat = getSeatOrThrow(request.seatId());
        Event ticketEvent = Event.create(status, seat, request);
        Performance performance = getPerformanceOrThrow(request);

        eventRepository.save(ticketEvent);
        seat.assignEvent(ticketEvent);

        Status reservedStatus = statusProvider.provide(StatusIds.Seat.RESERVED);
        seat.setStatusTo(reservedStatus);

        return TicketEventResponseDto.from(ticketEvent, performance);
    }

    @Override
    public void applyTicketEvent(Long eventId) {
        Long memberId = SecurityUtil.getSignInMemberId();

        TicketLockMessage msg = new TicketLockMessage(
                eventId,
                memberId
        );

        eventPublisher.publish(msg);
    }

    @Override
    @Transactional(readOnly = true)
    public PagingResponse<EventListResponseDto> getEventList(EventType type, int page, int size) {
        int offset = page * size;

        return switch (type) {
            case COUPON -> PagingResponse.from(
                    new ArrayList<>(eventMapper.findCouponEventList(size, offset)),
                    page,
                    size,
                    eventMapper.countCouponEvents()
            );
            case TICKET -> PagingResponse.from(
                    new ArrayList<>(eventMapper.findTicketEventList(size, offset)),
                    page,
                    size,
                    eventMapper.countTicketEvents()
            );
            default -> throw new BusinessException(ErrorCode.INVALID_TYPE_VALUE);
        };
    }


    @Override
    @Transactional(readOnly = true)
    public TicketEventDetailResponseDto getTicketEventDetail(Long eventId) {
        TicketEventDetailResponseDto dto = eventMapper.findTicketEventDetail(eventId);
        if (dto == null) throw new BusinessException(ErrorCode.EVENT_NOT_FOUND);
        return dto;
    }

    @Override
    @Transactional
    public void issueCoupon(Long eventId) {
        pessimisticEventApplyExecutor.issueCouponOnce(eventId);
    }

    @Override
    public PagingResponse<TicketListResponseDto> searchTicketEvents(String keyword, int page, int size) {
        int offset = page * size;
        List<TicketListResponseDto> list = eventMapper.searchTicketEvents(keyword, size, offset);
        int total = eventMapper.countSearchTicketEvents(keyword);

        return PagingResponse.from(list, page, size, total);
    }

    @Transactional(readOnly = true)
    public PagingResponse<TicketEventResponseDto> findRandomOngoingEvents() {
        int page = 0;
        int size = 5;
        int offset = page * size;

        List<SeatProjection> raw = eventMapper.findRandomOngoingEvents(size, offset);
        long total = eventMapper.countTicketEvents();

        List<TicketEventResponseDto> content = raw.stream()
                .map(r -> {
                    String row = r.seatNumber().replaceAll("[0-9]", "");
                    String number = r.seatNumber().replaceAll("[^0-9]", "");
                    String formattedSeat = row + "열 " + number + "번";

                    return new TicketEventResponseDto(
                            r.eventId(),
                            r.performanceId(),
                            r.eventName(),
                            formattedSeat,
                            r.startDate(),
                            r.endDate()
                    );
                })
                .toList();

        return PagingResponse.from(content, page, size, total);
    }

    @Override
    @Transactional(readOnly = true)
    public PagingResponse<CouponResponseDto> getMyCoupons(int page, int size) {
        int offset = page * size;

        Long memberId = SecurityUtil.getSignInMemberId();
        List<CouponResponseDto> list = couponReceivedMapper.findMyCoupons(memberId, size, offset);
        int total = couponReceivedMapper.countMyCoupons(memberId);

        return PagingResponse.from(list, page, size, total);
    }

    @Override
    public List<ExpiringSoonCouponResponseDto> getCouponListExpiringUntil(LocalDate untilDate) {
        Instant now = Instant.now(clock);
        Instant endExclusive = untilDate.plusDays(1).atStartOfDay(zone).toInstant();

        if (endExclusive.isBefore(now)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        return couponMapper.findCouponListExpiringBefore(endExclusive);
    }

    private Seat getSeatOrThrow(Long eventSeatId) {
        return seatRepository.findById(eventSeatId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SEAT_NOT_FOUND));
    }

    private Performance getPerformanceOrThrow(TicketEventCreateRequestDto request) {
        return performanceRepository.findById(request.performanceId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PERFORMANCE_NOT_FOUND));
    }
}
