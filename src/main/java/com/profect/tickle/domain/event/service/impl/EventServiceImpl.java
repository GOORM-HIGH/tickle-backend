package com.profect.tickle.domain.event.service.impl;

import com.profect.tickle.domain.event.dto.request.CouponCreateRequestDto;
import com.profect.tickle.domain.event.dto.request.TicketEventCreateRequestDto;
import com.profect.tickle.domain.event.dto.response.*;
import com.profect.tickle.domain.event.entity.Coupon;
import com.profect.tickle.domain.event.entity.Event;
import com.profect.tickle.domain.event.entity.EventType;
import com.profect.tickle.domain.event.mapper.EventMapper;
import com.profect.tickle.domain.event.repository.CouponRepository;
import com.profect.tickle.domain.event.repository.EventRepository;
import com.profect.tickle.domain.event.service.EventService;
import com.profect.tickle.domain.member.entity.CouponReceived;
import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.repository.CouponReceivedRepository;
import com.profect.tickle.domain.member.repository.MemberRepository;
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
import com.profect.tickle.global.status.Status;
import com.profect.tickle.global.status.repository.StatusRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final SeatRepository seatRepository;
    private final CouponRepository couponRepository;
    private final EventRepository eventRepository;
    private final StatusRepository statusRepository;
    private final EventMapper eventMapper;
    private final MemberRepository memberRepository;
    private final ReservationRepository reservationRepository;
    private final CouponReceivedRepository couponReceivedRepository;
    private final PointRepository pointRepository;

    private final PointTarget eventTarget = PointTarget.EVENT;

    Member member = new Member();  //[임의 값] 유저 개발 완료 시 삭제 코드

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
                request.valid()
        );
        couponRepository.save(coupon);

        Status status = getStatusOrThrow(9L);
        Event event = Event.create(status, coupon, request.name());

        eventRepository.save(event);
        coupon.updateEvent(event);

        return CouponResponseDto.from(coupon);
    }

    @Override
    @Transactional
    public TicketEventResponseDto createTicketEvent(TicketEventCreateRequestDto request) {
        Status status = getStatusOrThrow(4L);
        Seat seat = getSeatOrThrow(request.seatId());
        Event ticketEvent = Event.create(status, seat, request);

        eventRepository.save(ticketEvent);

        return TicketEventResponseDto.from(ticketEvent, request.performanceId());
    }

    @Override
    @Transactional
    public TicketApplyResponseDto applyTicketEvent(Long eventId) {
        Event event = getEventOrThrow(eventId);
        // [구현 코드] 현재 로그인 시 유저가 존재하는 지 확인 -> 유저 개발 완료 시 주석 삭제
        /*member = memberRepository.findById()
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));*/

        deductPoint(member, event, eventTarget);
        event.accumulate(event.getPerPrice());

        // 목표 도달 시 당첨 처리
        boolean isWinner = (event.getAccrued().equals(event.getGoalPrice()));
        if (isWinner) {
            Seat seat = getSeatOrThrow(event.getSeat().getId());

            seat.assignTo(member);
            Reservation reservation = Reservation.create(
                    member,
                    seat.getPerformance(),
                    getStatusOrThrow(9L),
                    "100",  // [임의 값]예매코드 생성 로직 따로 필요 -> 예매코드 로직 생성 시 변경
                    event.getGoalPrice(),
                    true);

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
        // [구현 코드] 현재 로그인 시 유저가 존재하는 지 확인 -> 유저 개발 완료 시 주석 삭제
        /*member = memberRepository.findById()
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));*/

        Event event = getEventOrThrow(eventId);
        Coupon coupon = event.getCoupon();

        if (coupon == null) throw new BusinessException(ErrorCode.COUPON_NOT_FOUND);

        if (couponReceivedRepository.existsByMemberIdAndCouponId(member.getId(), coupon.getId())) {
            throw new BusinessException(ErrorCode.ALREADY_ISSUED_COUPON);
        }

        if (coupon.getCount() <= 0) {
            event.updateStatus(getStatusOrThrow(6L));
            throw new BusinessException(ErrorCode.COUPON_SOLD_OUT);
        }

        coupon.decreaseCount();
        couponReceivedRepository.save(CouponReceived.create(member, coupon));
    }

    @Override
    public PagingResponse<TicketListResponseDto> searchTicketEvents(String keyword, int page, int size) {
        int offset = page * size;
        List<TicketListResponseDto> list = eventMapper.searchTicketEvents(keyword, size, offset);
        int total = eventMapper.countSearchTicketEvents(keyword);

        return PagingResponse.from(list, page, size, total);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventListResponseDto> getRandomOngoingEvents() {
        return new ArrayList<>(eventMapper.findRandomOngoingEvents());
    }

    private Event getEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));
    }

    private Status getStatusOrThrow(Long statusId) {
        return statusRepository.findById(statusId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STATUS_NOT_FOUND));
    }

    private Seat getSeatOrThrow(Long eventSeatId) {
        return seatRepository.findById(eventSeatId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SEAT_NOT_FOUND));
    }

    private void deductPoint(Member member, Event event, PointTarget target) {
        if (member.getPointBalance() < event.getPerPrice()) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_POINT);
        }
        member.usePoint(event.getPerPrice());

        Point point = Point.create(member, event.getPerPrice(), target, member.getPointBalance());
        pointRepository.save(point);
    }
}
