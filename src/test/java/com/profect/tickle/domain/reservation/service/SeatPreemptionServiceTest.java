package com.profect.tickle.domain.reservation.service;

import static com.profect.tickle.domain.reservation.service.SeatPreemptionTestFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.domain.reservation.dto.request.SeatPreemptionRequestDto;
import com.profect.tickle.domain.reservation.dto.response.preemption.SeatPreemptionResponseDto;
import com.profect.tickle.domain.reservation.entity.Seat;
import com.profect.tickle.domain.reservation.repository.SeatRepository;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.status.Status;
import com.profect.tickle.global.status.StatusIds;
import com.profect.tickle.global.status.service.StatusProvider;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@DisplayName("SeatPreemptionService 단위 테스트")
class SeatPreemptionServiceTest {

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private StatusProvider statusProvider;

    @InjectMocks
    private SeatPreemptionService seatPreemptionService;

    @Test
    @DisplayName("TC-PREEMPTION-001: 단일 좌석 선점 성공")
    void shouldPreemptSingleSeatSuccessfully() {
        // given
        List<Long> seatIds = List.of(1L);
        Long performanceId = 1L;
        Long userId = 1L;

        List<Seat> availableSeats = List.of(createAvailableSeat(1L, performanceId));
        Member testMember = mockMember();

        Status mockPreemptedStatus = mockPreemptedStatus();
        given(statusProvider.provide(StatusIds.Seat.PREEMPTED)).willReturn(mockPreemptedStatus);

        given(seatRepository.findAllByIdWithLock(seatIds)).willReturn(availableSeats);
        given(memberRepository.findById(userId)).willReturn(Optional.of(testMember));
        given(seatRepository.saveAll(availableSeats)).willReturn(availableSeats);

        SeatPreemptionRequestDto request = new SeatPreemptionRequestDto(performanceId, seatIds);

        // when
        SeatPreemptionResponseDto response = seatPreemptionService.preemptSeats(request, userId);

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getSeats()).hasSize(1);
        assertThat(response.getPreemptionToken()).isNotNull();
        assertThat(response.getPreemptedUntil()).isNotNull();
        assertThat(response.getMessage()).contains("1개 좌석을 선점했습니다");

        verify(seatRepository).saveAll(availableSeats);
    }

    @Test
    @DisplayName("TC-PREEMPTION-002: 복수 좌석 선점 성공")
    void shouldPreemptMultipleSeatsSuccessfully() {
        // given
        List<Long> seatIds = Arrays.asList(1L, 2L, 3L);
        Long performanceId = 1L;
        Long userId = 1L;

        List<Seat> availableSeats = Arrays.asList(
                createAvailableSeat(1L, performanceId),
                createAvailableSeat(2L, performanceId),
                createAvailableSeat(3L, performanceId)
        );
        Member testMember = mockMember();

        Status mockPreemptedStatus = mockPreemptedStatus();
        given(statusProvider.provide(StatusIds.Seat.PREEMPTED)).willReturn(mockPreemptedStatus);

        given(seatRepository.findAllByIdWithLock(seatIds)).willReturn(availableSeats);
        given(memberRepository.findById(userId)).willReturn(Optional.of(testMember));
        given(seatRepository.saveAll(availableSeats)).willReturn(availableSeats);

        SeatPreemptionRequestDto request = new SeatPreemptionRequestDto(performanceId, seatIds);

        // when
        SeatPreemptionResponseDto response = seatPreemptionService.preemptSeats(request, userId);

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getSeats()).hasSize(3);
        assertThat(response.getMessage()).contains("3개 좌석을 선점했습니다");

        assertThat(response.getPreemptionToken()).isNotNull();
        assertThat(response.getPreemptedUntil()).isNotNull();

        verify(seatRepository).saveAll(availableSeats);
    }

    @Test
    @DisplayName("TC-PREEMPTION-003: 선점 불가능한 좌석 포함시 전체 선점 실패")
    void shouldFailWhenUnavailableSeatsIncluded() {
        // given
        List<Long> seatIds = Arrays.asList(1L, 2L);
        Long performanceId = 1L;
        Long userId = 1L;

        List<Seat> mixedSeats = Arrays.asList(
                createAvailableSeat(1L, performanceId),
                createUnavailableSeat(2L, performanceId)
        );

        given(seatRepository.findAllByIdWithLock(seatIds)).willReturn(mixedSeats);

        SeatPreemptionRequestDto request = new SeatPreemptionRequestDto(performanceId, seatIds);

        // when
        SeatPreemptionResponseDto response = seatPreemptionService.preemptSeats(request, userId);

        // then
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("선점할 수 없는 좌석");
        assertThat(response.getUnavailableSeatIds()).contains(2L);

        verify(seatRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("TC-PREEMPTION-004: 다른 공연의 좌석 포함시 선점 실패")
    void shouldFailWhenSeatsFromDifferentPerformanceIncluded() {
        // given
        List<Long> seatIds = List.of(1L, 2L);
        Long performanceId = 1L;
        Long differentPerformanceId = 2L;
        Long userId = 1L;

        List<Seat> mixedSeats = Arrays.asList(
                createAvailableSeat(1L, performanceId),
                createSeatFromDifferentPerformance(2L, differentPerformanceId)
        );

        given(seatRepository.findAllByIdWithLock(seatIds)).willReturn(mixedSeats);

        SeatPreemptionRequestDto request = new SeatPreemptionRequestDto(performanceId, seatIds);

        // when
        SeatPreemptionResponseDto response = seatPreemptionService.preemptSeats(request, userId);

        // then
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getUnavailableSeatIds()).contains(2L);

        verify(seatRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("TC-PREEMPTION-005: 좌석이 조회되지 않으면 선점 실패")
    void shouldFailWhenNoSeatsFound() {
        // given
        List<Long> seatIds = List.of(999L);
        Long performanceId = 1L;
        Long userId = 1L;

        given(seatRepository.findAllByIdWithLock(seatIds)).willReturn(Collections.emptyList());

        SeatPreemptionRequestDto request = new SeatPreemptionRequestDto(performanceId, seatIds);

        // when
        SeatPreemptionResponseDto response = seatPreemptionService.preemptSeats(request, userId);

        // then
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("선점할 수 없는 좌석");

        verify(seatRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("TC-PREEMPTION-006: 존재하지 않는 사용자ID로 선점 시도시 예외 발생")
    void shouldThrowExceptionWhenMemberNotFound() {
        // given
        List<Long> seatIds = List.of(1L);
        Long performanceId = 1L;
        Long invalidUserId = 999L;

        List<Seat> availableSeats = List.of(createAvailableSeat(1L, performanceId));

        given(seatRepository.findAllByIdWithLock(seatIds)).willReturn(availableSeats);
        given(memberRepository.findById(invalidUserId)).willReturn(Optional.empty());

        SeatPreemptionRequestDto request = new SeatPreemptionRequestDto(performanceId, seatIds);

        // when & then
        assertThatThrownBy(() -> seatPreemptionService.preemptSeats(request, invalidUserId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);

        verify(seatRepository, never()).saveAll(any());
    }
}