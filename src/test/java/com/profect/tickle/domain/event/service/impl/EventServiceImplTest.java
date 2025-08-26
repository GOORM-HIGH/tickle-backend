
package com.profect.tickle.domain.event.service.impl;

import com.profect.tickle.domain.event.dto.response.TicketApplyResponseDto;
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
import com.profect.tickle.global.security.util.principal.CustomUserDetails;
import com.profect.tickle.global.status.StatusIds;
import com.profect.tickle.testsecurity.WithMockMember;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import java.util.List;
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



    @WithMockMember(id = 1L, email = "user1@test.com", roles = {"MEMBER"})
    @DisplayName("사용자가 진행중인 티켓 이벤트에 참여한다.")
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
        assertThat(updateEvent.getAccrued()).isEqualTo(1000);
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


    @Transactional
    @WithMockMember(id = 1L, email = "user1@test.com", roles = {"MEMBER"})
    @DisplayName("사용자가 진행중인 쿠폰 이벤트의 잔여 수량이 남아있는 쿠폰을 발급받는다.")
    @Test
    void issueCoupon(){
        // given
        Event event = eventRepository.findById(1L).orElseThrow();
        Coupon coupon = event.getCoupon();
        Long eventId = event.getId();
        Long couponId = coupon.getId();
        short before = coupon.getCount();

        // when
        eventService.issueCoupon(eventId);

        // then
        assertThat(coupon.getCount()).isEqualTo((short)(before - 1));
        assertThat(couponReceivedRepository.existsByMemberIdAndCouponId(1L, couponId)).isTrue();
        assertThat(event.getStatus().getId()).isEqualTo(StatusIds.Event.IN_PROGRESS);
    }

    @Transactional
    @WithMockMember(id = 1L, email = "user1@test.com", roles = {"MEMBER"})
    @DisplayName("쿠폰의 잔여 수량이 0개가 될 시, 쿠폰 이벤트가 종료된다.")
    @Test
    void issueCouponThenEndEvent(){
        // given
        Event event = eventRepository.findById(3L).orElseThrow();
        Coupon coupon = event.getCoupon();
        Long eventId = event.getId();

        // when
        eventService.issueCoupon(eventId);

        // then
        assertThat(coupon.getCount()).isEqualTo((short)0);
        assertThat(event.getStatus().getId()).isEqualTo(StatusIds.Event.COMPLETED);
    }

    @Transactional
    @WithMockMember(id = 1L, email = "user1@test.com", roles = {"MEMBER"})
    @DisplayName("잔여 수량이 남지 않은(종료 된) 쿠폰 이벤트의 쿠폰을 발급받을 수 없다.")
    @Test
    void issueCouponThenFailBySoldOut(){
        // given
        Event event = eventRepository.findById(5L).orElseThrow();
        Long eventId = event.getId();

        // when & then
        assertThatThrownBy(() -> eventService.issueCoupon(eventId))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.COUPON_SOLD_OUT.getMessage());
    }

    @Transactional
    @WithMockMember(id = 1L, email = "user1@test.com", roles = {"MEMBER"})
    @DisplayName("이벤트 시작 전의 쿠폰 이벤트의 쿠폰을 발급받을 수 없다.")
    @Test
    void issueCouponThenFailByNotStarted(){
        // given
        Event event = eventRepository.findById(4L).orElseThrow();
        Long eventId = event.getId();

        // when & then
        assertThatThrownBy(() -> eventService.issueCoupon(eventId))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.EVENT_NOT_IN_PROGRESS.getMessage());
    }

    @Transactional
    @WithMockMember(id = 1L, email = "user1@test.com", roles = {"MEMBER"})
    @DisplayName("사용자는 같은 쿠폰을 중복적으로 발급할 수 없다.")
    @Test
    void issueCouponThenFailByDuplicate() {
        // given
        Event event = eventRepository.findById(1L).orElseThrow();
        Long eventId = event.getId();
        Short before = event.getCoupon().getCount();
        Long couponId = eventRepository.findById(eventId).orElseThrow().getCoupon().getId();
        eventService.issueCoupon(eventId);

        // when & then
        assertThatThrownBy(() -> eventService.issueCoupon(eventId))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.ALREADY_ISSUED_COUPON.getMessage());
        assertThat(before).isEqualTo(before);
        assertThat(couponReceivedRepository.existsByMemberIdAndCouponId(1L, couponId)).isTrue();
    }

    @DisplayName("티켓 이벤트에 여러 사용자가 동시에 응모할 수 있다.")
    @Test
    void applyTicketEventWithMultipleMembers() throws InterruptedException {
        // given
        int memberCount = 100;
        final long eventId = 6L;
        final int perPrice = 10000;
        int eventAmount = 100000;

        Event event = eventRepository.findById(eventId).orElseThrow();

        ExecutorService pool = Executors.newFixedThreadPool(memberCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(memberCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        for (long id = 1; id <= memberCount; id++) {
            Member m = memberRepository.findById(id).orElseThrow();
            m.addPoint(perPrice);
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
                    log.error("apply failed for member {}: {}", memberId, new String[]{e.getMessage()}, e);
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

