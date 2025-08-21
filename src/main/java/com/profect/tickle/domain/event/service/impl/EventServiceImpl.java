package com.profect.tickle.domain.event.service.impl;

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
import com.profect.tickle.domain.event.service.EventService;
import com.profect.tickle.domain.member.entity.CouponReceived;
import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.repository.CouponReceivedRepository;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.domain.performance.entity.Performance;
import com.profect.tickle.domain.performance.repository.PerformanceRepository;
import com.profect.tickle.domain.point.entity.Point;
import com.profect.tickle.domain.point.entity.PointTarget;
import com.profect.tickle.domain.point.repository.PointRepository;
import com.profect.tickle.domain.reservation.entity.Reservation;
import com.profect.tickle.domain.reservation.entity.Seat;
import com.profect.tickle.domain.reservation.repository.ReservationRepository;
import com.profect.tickle.domain.reservation.repository.SeatRepository;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.paging.PagingResponse;
import com.profect.tickle.global.security.util.SecurityUtil;
import com.profect.tickle.global.status.Status;
import com.profect.tickle.global.status.StatusIds;
import com.profect.tickle.global.status.service.StatusProvider;
import jakarta.validation.constraints.NotNull;
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
    private final StatusProvider statusProvider;
    private final Clock clock;
    private final ZoneId zone = ZoneId.systemDefault();

    // mapper & repositories
    private final SeatRepository seatRepository;
    private final CouponRepository couponRepository;
    private final EventRepository eventRepository;
    private final MemberRepository memberRepository;
    private final ReservationRepository reservationRepository;
    private final CouponReceivedRepository couponReceivedRepository;
    private final PerformanceRepository performanceRepository;
    private final PointRepository pointRepository;
    private final EventMapper eventMapper;
    private final CouponMapper couponMapper;
    private final CouponReceivedMapper couponReceivedMapper;

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
    @Transactional
    public TicketApplyResponseDto applyTicketEvent(Long eventId) {
        Event event = getEventOrThrow(eventId);
        Member member = getMemberOrThrow();

        Point point = member.deductPoint(event.getPerPrice(), eventTarget);
        pointRepository.save(point);

        event.accumulate(event.getPerPrice());

        boolean isWinner = (event.getAccrued() >= event.getGoalPrice());
        if (isWinner) {
            Seat seat = getSeatOrThrow(event.getSeat().getId());
            event.updateStatus(statusProvider.provide(StatusIds.Event.COMPLETED));

            seat.assignTo(member);
            Reservation reservation = Reservation.create(
                    member,
                    seat.getPerformance(),
                    statusProvider.provide(StatusIds.Reservation.PAID),
                    event.getAccrued()
            );

            reservation.assignSeat(seat);

            reservationRepository.save(reservation);
        }

        return TicketApplyResponseDto.from(eventId, member.getId(), isWinner);
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
        Event event = getEventOrThrow(eventId);
        Coupon coupon = event.getCoupon();
        Member member = getMemberOrThrow();

        if (coupon == null) throw new BusinessException(ErrorCode.COUPON_NOT_FOUND);

        if (couponReceivedRepository.existsByMemberIdAndCouponId(member.getId(), coupon.getId())) {
            throw new BusinessException(ErrorCode.ALREADY_ISSUED_COUPON);
        }

        if (coupon.getCount() <= 0) {
            event.updateStatus(statusProvider.provide(StatusIds.Event.COMPLETED));
            throw new BusinessException(ErrorCode.COUPON_SOLD_OUT);
        }

        coupon.decreaseCount();
        Status issuedStatus = statusProvider.provide(StatusIds.Coupon.AVAILABLE);
        couponReceivedRepository.save(CouponReceived.create(member, coupon, issuedStatus));
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

    private Event getEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
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

    private Performance getPerformanceOrThrow(TicketEventCreateRequestDto request) {
        return performanceRepository.findById(request.performanceId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PERFORMANCE_NOT_FOUND));
    }

    @Override
    public List<ExpiringSoonCouponResponseDto> getCouponListExpiringUntil(@NotNull LocalDate untilDate) {
        Instant now = Instant.now(clock);
        Instant endExclusive = untilDate.plusDays(1).atStartOfDay(zone).toInstant();

        if (endExclusive.isBefore(now)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        return couponMapper.findCouponListExpiringBefore(endExclusive);
    }
}
