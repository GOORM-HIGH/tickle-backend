
package com.profect.tickle.domain.event.service.impl;

import com.profect.tickle.domain.event.dto.request.TicketEventCreateRequestDto;
import com.profect.tickle.domain.event.dto.response.TicketApplyResponseDto;
import com.profect.tickle.domain.event.dto.response.TicketEventDetailResponseDto;
import com.profect.tickle.domain.event.dto.response.TicketEventResponseDto;
import com.profect.tickle.domain.event.dto.response.TicketListResponseDto;
import com.profect.tickle.domain.event.entity.Coupon;
import com.profect.tickle.domain.event.entity.Event;
import com.profect.tickle.domain.event.repository.CouponRepository;
import com.profect.tickle.domain.event.repository.EventRepository;
import com.profect.tickle.domain.event.service.EventService;
import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.repository.CouponReceivedRepository;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.domain.member.service.MemberService;
import com.profect.tickle.domain.point.repository.PointRepository;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.paging.PagingResponse;
import com.profect.tickle.global.security.util.principal.CustomUserDetails;
import com.profect.tickle.global.status.StatusIds;
import com.profect.tickle.testsecurity.WithMockMember;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.query.sqm.tree.SqmNode.log;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Sql(scripts = "classpath:sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:sql/schema.sql",  executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:sql/data.sql",    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class EventServiceImplTest{

    @Autowired
    EventService eventService;

    @Autowired
    PointRepository pointRepository;

    @Autowired
    EventRepository eventRepository;

    @Autowired
    MemberRepository memberRepository;


    @Autowired
    Clock clock;

    @Autowired
    CouponRepository couponRepository;

    @Autowired
    MemberService memberService;

    @Autowired
    CouponReceivedRepository couponReceivedRepository;

    Member member;


    @BeforeEach
    void setUp() {
        member = memberRepository.findById(1L).orElseThrow();
        member.addPoint(2000);
        memberRepository.save(member);
    }

    @Nested
    @DisplayName("사용자가 진행중인 티켓 이벤트에 참여할 수 있다.")
    class ApplyTicketEvent {
        @WithMockMember(id = 1L, email = "user1@test.com", roles = {"MEMBER"})
        @DisplayName("사용자는 진행중인 티켓 이벤트에 참여한다.")
        @Test
        void applyTicketEvent(){
            // given
            Event event = eventRepository.findById(6L).orElseThrow();
            Long eventId = event.getId();

            // when
            TicketApplyResponseDto result = eventService.applyTicketEvent(eventId);
            Event updateEvent = eventRepository.findById(eventId).orElseThrow();
            // then
            assertThat(result).isNotNull();
            assertThat(result.eventId()).isEqualTo(eventId);
            assertThat(result.memberId()).isEqualTo(1L);
            assertThat(result.isWinner()).isFalse();
            assertThat(result.message()).isEqualTo("아쉽네요. 다음 기회에...");
            assertThat(updateEvent.getAccrued()).isEqualTo(1);
        }

        @WithMockMember(id = 1L, email = "user1@test.com", roles = {"MEMBER"})
        @DisplayName("사용자가 진행중인 티켓 이벤트에 참여해 당첨된다.")
        @Test
        void applyTicketEventThenWin(){
            // given
            // 같은 트랜잭션 내에서 조회하면 Managed 상태
            Event event = eventRepository.findById(7L).orElseThrow();
            Long eventId = event.getId();

            // when
            TicketApplyResponseDto result = eventService.applyTicketEvent(eventId);
            Event updated = eventRepository.findById(eventId).orElseThrow();

            // then
            assertThat(updated.getStatus().getId()).isEqualTo(6L);
            assertThat(result).isNotNull();
            assertThat(result.eventId()).isEqualTo(eventId);
            assertThat(result.memberId()).isEqualTo(1L);
            assertThat(result.isWinner()).isTrue();
            assertThat(result.message()).isEqualTo("축하합니다! 티켓에 당첨되었습니다. \n 예매권은 마이페이지에서 확인하세요.");
            assertThat(updated.getStatus().getId()).isEqualTo(6L);
        }

        @WithMockMember(id = 1L, email = "user1@test.com", roles = {"MEMBER"})
        @DisplayName("티켓 이벤트 응모 시, 사용자의 보유 포인트가 부족한 경우 응모할 수 없다.")
        @Test
        void applyTicketEventThenFailByNoPoint(){
            // given
            Event event = eventRepository.findById(8L).orElseThrow();
            Long eventId = event.getId();

            // when & then
            assertThatThrownBy(() -> eventService.applyTicketEvent(eventId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.INSUFFICIENT_POINT.getMessage());
        }

        @WithMockMember(id = 1L, email = "user1@test.com", roles = {"MEMBER"})
        @DisplayName("사용자는 종료된 티켓 이벤트에 참여할 수 없다.")
        @Test
        void applyTicketEventThenFailByClosed(){
            // given
            Event event = eventRepository.findById(18L).orElseThrow();
            Long eventId = event.getId();

            // when & then
            assertThatThrownBy(() -> eventService.applyTicketEvent(eventId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.EVENT_NOT_IN_PROGRESS.getMessage());
        }
        @DisplayName("티켓 이벤트에 여러 사용자가 동시에 응모할 수 있다.")
        @Test
        void applyTicketEventWithMultipleMembers() throws InterruptedException {
            // given
            int memberCount = 100;
            final long eventId = 6L;
            final int memberPerPrice = 5000;
            final int perPrice = 1;
            int eventAmount = memberCount * perPrice;

            Event event = eventRepository.findById(eventId).orElseThrow();

            ExecutorService pool = Executors.newFixedThreadPool(memberCount);
            CountDownLatch startGate = new CountDownLatch(1);
            CountDownLatch doneGate = new CountDownLatch(memberCount);

            AtomicInteger successCount = new AtomicInteger();
            AtomicInteger failCount = new AtomicInteger();

            for (long id = 1; id <= memberCount; id++) {
                Member m = memberRepository.findById(id).orElseThrow();
                m.addPoint(memberPerPrice);
                memberRepository.save(m);
            }

            // when
            for (long id = 1; id <= memberCount; id++) {
                final long memberId = id;
                pool.submit(() -> {
                    try {
                        startGate.await(); // 스레드 준비 완료
                        var authorities = List.of(new SimpleGrantedAuthority("MEMBER")); // 스레드 별 로그인 컨텍스트 세팅
                        var principal = new CustomUserDetails(
                                memberId,
                                "user" + memberId + "@test.com",
                                "pw" + memberId,
                                "유저" + memberId,
                                authorities
                        );
                        var authentication =
                                new UsernamePasswordAuthenticationToken(principal, principal.getPassword(), principal.getAuthorities());
                        TestSecurityContextHolder.setAuthentication(authentication);

                        eventService.applyTicketEvent(eventId);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                    } finally {
                        doneGate.countDown();
                        TestSecurityContextHolder.clearContext();
                    }
                });
            }
            startGate.countDown(); // 모든 작업자 준비 후 동시에 출발
            doneGate.await(); // 모두 끝날 때까지 대기
            pool.shutdown();
            // then
            log.info("successCount = " + successCount.get());
            log.info("failCount = " + failCount.get());

            Event updated = eventRepository.findById(event.getId()).orElseThrow();
            assertThat(updated.getAccrued()).isEqualTo(eventAmount);
        }
    }


    @Nested
    @DisplayName("사용자는 진행중인 쿠폰 이벤트의 쿠폰을 발급받을 수 있다.")
    class CreateCouponEvent {

        @WithMockMember(id = 1L, email = "user1@test.com", roles = {"MEMBER"})
        @DisplayName("사용자가 진행중인 쿠폰 이벤트의 잔여 수량이 남아있는 쿠폰을 발급받는다.")
        @Test
        void issueCoupon() {
            // given
            Long eventId = 1L;
            Event event = eventRepository.findById(1L).orElseThrow();
            Long couponId = event.getCoupon().getId();

            Coupon coupon = couponRepository.findById(couponId).orElseThrow();
            short before = coupon.getCount();

            // when
            eventService.issueCoupon(eventId);

            Coupon updateCoupon = couponRepository.findById(couponId).orElseThrow();
            Event updateEvent = eventRepository.findById(eventId).orElseThrow();

            // then
            assertThat(updateCoupon.getCount()).isEqualTo((short) (before - 1));
            assertThat(couponReceivedRepository.existsByMemberIdAndCouponId(1L, couponId)).isTrue();
            assertThat(updateEvent.getStatus().getId()).isEqualTo(StatusIds.Event.IN_PROGRESS);
        }


        @WithMockMember(id = 1L, email = "user1@test.com", roles = {"MEMBER"})
        @DisplayName("쿠폰의 잔여 수량이 0개가 될 시, 쿠폰 이벤트가 종료된다.")
        @Test
        void issueCouponThenEndEvent() {
            // given
            Long eventId = 3L;
            Event event = eventRepository.findById(eventId).orElseThrow();
            Long couponId = event.getCoupon().getId();

            // when
            eventService.issueCoupon(eventId);
            Coupon updatedCoupon = couponRepository.findById(couponId).orElseThrow();
            Event updatedEvent = eventRepository.findById(eventId).orElseThrow();

            // then

            assertThat(updatedCoupon.getCount()).isEqualTo((short) 0);
            assertThat(updatedEvent.getStatus().getId()).isEqualTo(StatusIds.Event.COMPLETED);
        }

        @WithMockMember(id = 1L, email = "user1@test.com", roles = {"MEMBER"})
        @DisplayName("잔여 수량이 남지 않은(종료 된) 쿠폰 이벤트의 쿠폰을 발급받을 수 없다.")
        @Test
        void issueCouponThenFailBySoldOut() {
            // given
            Event event = eventRepository.findById(5L).orElseThrow();
            Long eventId = event.getId();

            // when & then
            assertThatThrownBy(() -> eventService.issueCoupon(eventId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.COUPON_SOLD_OUT.getMessage());
        }

        @WithMockMember(id = 1L, email = "user1@test.com", roles = {"MEMBER"})
        @DisplayName("이벤트 시작 전의 쿠폰 이벤트의 쿠폰을 발급받을 수 없다.")
        @Test
        void issueCouponThenFailByNotStarted() {
            // given
            Event event = eventRepository.findById(4L).orElseThrow();
            Long eventId = event.getId();

            // when & then
            assertThatThrownBy(() -> eventService.issueCoupon(eventId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.EVENT_NOT_IN_PROGRESS.getMessage());
        }

        @WithMockMember(id = 1L, email = "user1@test.com", roles = {"MEMBER"})
        @DisplayName("사용자는 같은 쿠폰을 중복적으로 발급할 수 없다.")
        @Test
        void issueCouponThenFailByDuplicate() {
            // given
            Long eventId = 1L;

            Long couponId = eventRepository.findById(eventId)
                    .orElseThrow()
                    .getCoupon()
                    .getId();

            short before = couponRepository.findById(couponId)
                    .orElseThrow()
                    .getCount();

            // 첫 발급
            eventService.issueCoupon(eventId);
            short afterFirst = couponRepository.findById(couponId)
                    .orElseThrow()
                    .getCount();
            assertThat(afterFirst).isEqualTo((short) (before - 1));
            assertThat(couponReceivedRepository.existsByMemberIdAndCouponId(1L, couponId)).isTrue();


            // when & then
            assertThatThrownBy(() -> eventService.issueCoupon(eventId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.ALREADY_ISSUED_COUPON.getMessage());

            // 실패 후에도 수량은 더 줄어들지 않았는지 확인
            short afterSecond = couponRepository.findById(couponId)
                    .orElseThrow()
                    .getCount();
            assertThat(afterSecond).isEqualTo(afterFirst);
        }

        @Test
        @DisplayName("쿠폰의 수량 M개면 동시에 K명이 발급해도 성공 횟수는 M개로 정확히 제한된다.")
        void issueCoupon_concurrent_exactlyStockSuccess() throws InterruptedException {
            // given
            long eventId = 1L;
            Event event = eventRepository.findById(eventId).orElseThrow();
            Long couponId = event.getCoupon().getId();
            short stock = couponRepository.findById(couponId).orElseThrow().getCount();

            int threadCount = stock;
            threadCount = Math.min(threadCount, 100);
            AtomicInteger success = new AtomicInteger();
            AtomicInteger fail = new AtomicInteger();

            ExecutorService pool = Executors.newFixedThreadPool(Math.min(threadCount, 64));
            CountDownLatch startGate = new CountDownLatch(1);
            CountDownLatch doneGate = new CountDownLatch(threadCount);

            for (long id = 1; id <= threadCount; id++) {
                final long memberId = id;
                pool.submit(() -> {
                    try {
                        startGate.await();
                        asMember(memberId); // ★ 보안컨텍스트 세팅
                        eventService.issueCoupon(eventId); // ★ 쿠폰 발급
                        success.incrementAndGet();
                    } catch (Exception e) {
                        fail.incrementAndGet();
                    } finally {
                        TestSecurityContextHolder.clearContext();
                        doneGate.countDown();
                    }
                });
            }

            // when
            startGate.countDown();
            doneGate.await();
            pool.shutdown();

            // then
            short left = couponRepository.findById(couponId).orElseThrow().getCount();
            Event updated = eventRepository.findById(eventId).orElseThrow();

            assertThat(success.get()).isEqualTo(stock);
            assertThat(fail.get()).isEqualTo(threadCount - stock);
            assertThat(left).isEqualTo((short) 0);
            assertThat(updated.getStatus().getId()).isEqualTo(StatusIds.Event.COMPLETED);
        }

        @Test
        @DisplayName("쿠폰의 수량 M보다 더 많은 N개의 발급 요청이 동시에 온다면, 정확히 M개 성공하고 N개 실패한다.")
        void issueCoupon_concurrent_limitToStock() throws InterruptedException {
            // given
            long eventId = 1L;
            Event event = eventRepository.findById(eventId).orElseThrow();
            Long couponId = event.getCoupon().getId();
            short stock = couponRepository.findById(couponId).orElseThrow().getCount();

            int threadCount = Math.min(stock + 1, 100);
            ExecutorService pool = Executors.newFixedThreadPool(Math.min(threadCount, 64));
            CountDownLatch startGate = new CountDownLatch(1);
            CountDownLatch doneGate = new CountDownLatch(threadCount);

            AtomicInteger success = new AtomicInteger();
            AtomicInteger fail = new AtomicInteger();

            // 누가 성공했는지 기록 (중복 방지용 Set)
            Set<Long> winners = Collections.newSetFromMap(new ConcurrentHashMap<>());

            for (long id = 1; id <= threadCount; id++) {
                final long memberId = id;
                pool.submit(() -> {
                    try {
                        startGate.await();
                        asMember(memberId); // 보안컨텍스트 세팅
                        eventService.issueCoupon(eventId);
                        winners.add(memberId);
                        success.incrementAndGet();
                    } catch (Exception e) {
                        fail.incrementAndGet();
                    } finally {
                        TestSecurityContextHolder.clearContext();
                        doneGate.countDown();
                    }
                });
            }

            // when
            startGate.countDown();
            doneGate.await();
            pool.shutdown();

            // then
            short left = couponRepository.findById(couponId).orElseThrow().getCount();
            long issuedRows = couponReceivedRepository.countByCouponId(couponId);
            Event updated = eventRepository.findById(eventId).orElseThrow();

            assertThat(success.get()).isEqualTo(stock);
            assertThat(fail.get()).isEqualTo(threadCount - stock);

            // DB에 실제 발급된 행 수도 정확히 M
            assertThat(issuedRows).isEqualTo(stock);

            // 성공 멤버 ID 집합 크기도 정확히 M (중복 체크)
            assertThat(winners).hasSize(stock);

            // 재고 0 & 이벤트 완료
            assertThat(left).isZero();
            assertThat(updated.getStatus().getId()).isEqualTo(StatusIds.Event.COMPLETED);
        }
    }

   @Nested
    @DisplayName("이벤트의 상세 정보를 조회할 수 있다.")
    class GetTicketEventDetail {

        @Test
        @DisplayName("사용자는 이벤트의 상세 정보를 조회할 수 있다.")
        void ok() {
            TicketEventDetailResponseDto d = eventService.getTicketEventDetail(6L);
            assertThat(d).isNotNull();
            assertThat(d.id()).isEqualTo(6L);
            assertThat(d.performanceTitle()).isNotBlank();
        }

        @Test
        @DisplayName("이벤트가 존재하지 않으면 조회할 수 없다.")
        void notFound() {
            assertThatThrownBy(() -> eventService.getTicketEventDetail(9999L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.EVENT_NOT_FOUND.getMessage());
        }
    }

    @Nested
    @DisplayName("티켓 이벤트를 조회할 수 있다.")
    class SearchTicketEvents {

        @Test
        @DisplayName("'레미'키워드를 검색하면 해당 제목을 가진 이벤트를 조회할 수 있다.")
        void SearchTicketEvent() {
            PagingResponse<TicketListResponseDto> page = eventService.searchTicketEvents("레미", 0, 10);
            assertThat(page.content()).isNotEmpty();
            assertThat(page.totalElements()).isEqualTo(1);
            assertThat(page.content().getFirst().getEventId()).isEqualTo(6L);
            assertThat(page.content().getFirst().getName()).isEqualTo("레미제라블 티켓 이벤트");
        }
    }

    @Nested
    @DisplayName("랜덤으로 이벤트를 조회한다. 이벤트가 5개 이하여도 조회가 가능하다.")
    class FindRandomOngoingEvents {

        @Test
        @DisplayName("랜덤으로 이벤트를 조회한다.")
        void ok() {
            PagingResponse<TicketEventResponseDto> page = eventService.findRandomOngoingEvents();
            assertThat(page.content()).isNotEmpty();
            assertThat(page.content().size()).isEqualTo(5);
        }
    }

  @Nested
    @DisplayName("호스트는 공연에 대한 이벤트를 생성할 수 있다.")
    class CreateTicketEvent {

        @Test
        @DisplayName("좌석과 공연이 존재하면 해당 공연에 대한 이벤트가 생성된다.")
        @Transactional
        void createTicketEvent() {
            // data.sql 의 performance_id=1, seat_id=1 사용 (seat는 event 6에 묶여있어도 서비스가 assignEvent로 덮어씀)
            var req = new TicketEventCreateRequestDto(16L, 16L, "기프트 티켓 이벤트", 50_000, (short) 500);

            TicketEventResponseDto dto = eventService.createTicketEvent(req);

            assertThat(dto).isNotNull();
            assertThat(dto.performanceId()).isEqualTo(16L);
            assertThat(dto.eventName()).isEqualTo("기프트 티켓 이벤트");
        }

        @Test
        @DisplayName("공연의 좌석이 없다면 공연에 대한 이벤트를 생성할 수 없다.")
        void seatNotFound() {
            var req = new TicketEventCreateRequestDto(1L, 9999L, "이벤트", 50_000, (short) 500);
            assertThatThrownBy(() -> eventService.createTicketEvent(req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.SEAT_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("공연이 없다면 공연에 대한 이벤트를 생성할 수 없다.")
        void performanceNotFound() {
            var req = new TicketEventCreateRequestDto(9999L, 1L, "이벤트", 50_000, (short) 500);
            assertThatThrownBy(() -> eventService.createTicketEvent(req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.PERFORMANCE_NOT_FOUND.getMessage());
        }
    }

    private void asMember(long memberId) {
        var authorities = List.of(new SimpleGrantedAuthority("MEMBER"));
        var principal = new CustomUserDetails(
                memberId,
                "user" + memberId + "@test.com", // data.sql 형식
                "pw" + memberId,
                "유저" + memberId,
                authorities
        );
        var auth = new UsernamePasswordAuthenticationToken(
                principal, principal.getPassword(), principal.getAuthorities()
        );
        TestSecurityContextHolder.setAuthentication(auth);
    }
}

