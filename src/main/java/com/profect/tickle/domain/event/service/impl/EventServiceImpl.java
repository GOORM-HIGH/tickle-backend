package com.profect.tickle.domain.event.service.impl;

import com.profect.tickle.domain.event.dto.request.CouponCreateRequestDto;
import com.profect.tickle.domain.event.dto.request.TicketEventCreateRequestDto;
import com.profect.tickle.domain.event.dto.response.CouponResponseDto;
import com.profect.tickle.domain.event.dto.response.TicketApplyResponseDto;
import com.profect.tickle.domain.event.dto.response.TicketEventResponseDto;
import com.profect.tickle.domain.event.entity.Coupon;
import com.profect.tickle.domain.event.entity.Event;
import com.profect.tickle.domain.event.repository.CouponRepository;
import com.profect.tickle.domain.event.repository.EventRepository;
import com.profect.tickle.domain.event.service.EventService;
import com.profect.tickle.domain.member.entity.Member;
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
import com.profect.tickle.global.status.Status;
import com.profect.tickle.global.status.repository.StatusRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final SeatRepository seatRepository;
    private final PointRepository pointRepository;
    private final CouponRepository couponRepository;
    private final EventRepository eventRepository;
    private final StatusRepository statusRepository;
    private final MemberRepository memberRepository;
    private final ReservationRepository reservationRepository;

    private final PointTarget eventTarget = PointTarget.EVENT;



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

        Status status = getStatusOrThrow("EVENT", (short) 100);
        Event event = Event.create(status, coupon, request.name());

        eventRepository.save(event);
        coupon.updateEvent(event);

        return CouponResponseDto.from(coupon);
    }

    @Override
    public TicketEventResponseDto createTicketEvent(TicketEventCreateRequestDto request) {
        Status status = getStatusOrThrow("EVENT", (short) 100);
        Seat seat = getSeatOrThrow(request.seatId());
        Event ticketEvent = Event.create(status, seat, request);

        eventRepository.save(ticketEvent);

        return TicketEventResponseDto.from(ticketEvent, request.performanceId());
    }

    @Override
    @Transactional
    public TicketApplyResponseDto applyTicketEvent(Long eventId) {

        Member member = new Member();  //[임의 값] 유저 개발 완료 시 삭제 코드
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));

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
                    getStatusOrThrow("RESERVATION", (short) 100),
                    "100",  // [임의 값]예매코드 생성 로직 따로 필요 -> 예매코드 로직 생성 시 변경
                    event.getGoalPrice(),
                    true);

            reservation.assignSeat(seat);

            reservationRepository.save(reservation);
        }

        return TicketApplyResponseDto.from(eventId, member.getId(), isWinner);
    }

    private Status getStatusOrThrow(String type, short code) {
        return statusRepository.findByTypeAndCode(type, code)
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
