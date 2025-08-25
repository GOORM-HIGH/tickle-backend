package com.profect.tickle.domain.performance.service;

import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.entity.MemberRole;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.domain.performance.dto.response.PerformanceDetailDto;
import com.profect.tickle.domain.performance.dto.response.PerformanceDto;
import com.profect.tickle.domain.performance.dto.response.PerformanceHostDto;
import com.profect.tickle.domain.performance.mapper.PerformanceMapper;
import com.profect.tickle.domain.performance.repository.GenreRepository;
import com.profect.tickle.domain.performance.repository.HallRepository;
import com.profect.tickle.domain.performance.repository.PerformanceRepository;
import com.profect.tickle.domain.reservation.repository.SeatTemplateRepository;
import com.profect.tickle.domain.reservation.service.SeatService;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.paging.PagingResponse;
import com.profect.tickle.global.security.util.SecurityUtil;
import com.profect.tickle.global.status.repository.StatusRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.aot.DisabledInAotMode;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.Mockito.*;

@DisabledInAotMode
@ExtendWith(MockitoExtension.class)
class PerformanceServiceTest {

    @InjectMocks
    PerformanceService performanceService;

    @Mock
    SeatService seatService;

    @Mock
    PerformanceMapper performanceMapper;

    @Mock
    MemberRepository memberRepository;

    @Mock
    PerformanceRepository performanceRepository;

    @Mock
    GenreRepository genreRepository;

    @Mock
    HallRepository hallRepository;

    @Mock
    StatusRepository statusRepository;

    @Mock
    SeatTemplateRepository seatTemplateRepository;

    @Mock
    Clock clock;

    @Test
    @DisplayName("삭제되지 않은 공연정보 상세 조회에 성공한다. 상세정보 반환과 함께 조회수 컬럼이 1 증가한다.")
    void TC_PERFORMANCE_001() {
        //given
        Long performanceId = 1L;
        PerformanceDetailDto stub = mock(PerformanceDetailDto.class);
        when(performanceMapper.findDetailById(performanceId)).thenReturn(stub);

        //when
        PerformanceDetailDto result = performanceService.getPerformanceDetail(performanceId);

        //then
        assertThat(result).isSameAs(stub);

        InOrder inOrder = inOrder(performanceMapper);
        inOrder.verify(performanceMapper).findDetailById(performanceId);
        inOrder.verify(performanceMapper).increaseLookCount(performanceId);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    @DisplayName("공연이 없거나 삭제되었다면 예외 처리되고 조회되지 않는. 또한 조회수 증가되지 않는다.")
    void TC_PERFORMANCE_002() {
        // given
        Long performanceId = 999L;
        when(performanceMapper.findDetailById(performanceId)).thenReturn(null);

        // when
        Throwable thrown = catchThrowable(() -> performanceService.getPerformanceDetail(performanceId));

        // then
        assertThat(thrown)
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.PERFORMANCE_NOT_FOUND.getMessage());

        verify(performanceMapper).findDetailById(performanceId);
        verify(performanceMapper, never()).increaseLookCount(anyLong());
        verifyNoMoreInteractions(performanceMapper);
    }

    @Test
    @DisplayName("장르별 공연 목록을 페이징해 조회에 성공한다.")
    void TC_PERFORMANCE_003() {
        // given
        Long genreId = 1L;
        int page = 2, size = 10, total = 23;
        int offset = page * size;
        when(performanceMapper.countPerformancesByGenre(genreId)).thenReturn(total);

        var items = List.of(
                PerformanceDto.builder().performanceId(21L).title("A").build(),
                PerformanceDto.builder().performanceId(22L).title("B").build(),
                PerformanceDto.builder().performanceId(23L).title("C").build()
        );
        when(performanceMapper.findPerformancesByGenre(genreId, offset, size)).thenReturn(items);

        // when
        PagingResponse<PerformanceDto> result = performanceService.getPerformancesByGenre(genreId, page, size);

        // then
        assertThat(result.content()).hasSize(3);
        assertThat(result.totalElements()).isEqualTo(23);
        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.isLast()).isTrue();
        assertThat(result.page()).isEqualTo(2);
        assertThat(result.size()).isEqualTo(10);

        InOrder io = inOrder(performanceMapper);
        io.verify(performanceMapper).countPerformancesByGenre(genreId);  // 먼저 호출
        io.verify(performanceMapper).findPerformancesByGenre(genreId, offset, size); // 그 다음
        io.verifyNoMoreInteractions();
    }

    @Test
    @DisplayName("잘못된 genreId면 INVALID_INPUT_VALUE 예외가 발생한다.")
    void TC_PERFORMANCE_003_invalid_genreId() {
        assertThatThrownBy(() -> performanceService.getPerformancesByGenre(0L, 0, 10))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(performanceMapper);
    }

    @Test
    @DisplayName("장르별 인기 공연 상위 10개 공연을 조회 성공한다.")
    void TC_PERFORMANCE_004() {
        // Given
        Long genreId = 1L;
        List<PerformanceDto> top10 = new ArrayList<>();

        IntStream.rangeClosed(1, 10).forEach(i ->
                top10.add(PerformanceDto.builder()
                        .performanceId(100L - i)
                        .title("TOP-" + i)
                        .date(Instant.parse("2025-09-" + String.format("%02d", i) + "T00:00:00Z"))
                        .build()));
        when(performanceMapper.findTop10ByGenre(genreId)).thenReturn(top10);

        // When
        List<PerformanceDto> result = performanceService.getTop10ByGenre(genreId);

        // Then
        assertThat(result).hasSize(10)
                .containsExactlyElementsOf(top10);
        verify(performanceMapper).findTop10ByGenre(genreId);
    }

    @Test
    @DisplayName("전체 공연중 인기 상위 공연이 10개 조회 성공한다.")
    void TC_PERFORMANCE_005() {
        // Given
        List<PerformanceDto> top10 = IntStream.rangeClosed(1, 10)
                .mapToObj(i -> PerformanceDto.builder()
                        .performanceId((long) i)
                        .title("ALL-" + i)
                        .date(Instant.parse("2025-10-" + String.format("%02d", i) + "T00:00:00Z"))
                        .build())
                .toList();
        when(performanceMapper.findTop10ByClickCount()).thenReturn(top10);

        // When
        List<PerformanceDto> result = performanceService.getTop10Performances();

        // Then
        assertThat(result).hasSize(10).containsExactlyElementsOf(top10);
        verify(performanceMapper).findTop10ByClickCount();
    }

    @Test
    void TC_PERFORMANCE_006() {
        // Given - any() 매처 사용
        List<PerformanceDto> mockPerformances = createMockPerformances();
        when(performanceMapper.findTop4UpcomingPerformances(any(LocalDateTime.class)))
                .thenReturn(mockPerformances);

        // When
        List<PerformanceDto> result = performanceService.getTop4UpcomingPerformances();

        // Then
        assertThat(result).hasSize(4);
        verify(performanceMapper).findTop4UpcomingPerformances(any(LocalDateTime.class));
    }

    private List<PerformanceDto> createMockPerformances() {
        return List.of(
                new PerformanceDto(1L, "공연1", Instant.now().plusNanos(1), "img1.jpg"),
                new PerformanceDto(2L, "공연2", Instant.now().plusNanos(2), "img2.jpg"),
                new PerformanceDto(3L, "공연3", Instant.now().plusNanos(3), "img3.jpg"),
                new PerformanceDto(4L, "공연4", Instant.now().plusNanos(4), "img4.jpg")
        );
    }

    @Test
    @DisplayName("키워드 '뮤지'로 검색하면 2번째 페이지가 반환되고 전체 개수·페이지 수·마지막 여부가 올바르게 계산된다.")
    void TC_PERFORMANCE_007() {
        // Given
        String keyword = "뮤지";
        int page = 1, size = 10;
        long total = 25L; // (참고) count는 종료 포함일 수 있음 - 현재 쿼리 기준
        int expectedOffset = page * size; // 10

        when(performanceMapper.countPerformancesByKeyword(keyword)).thenReturn(total);

        List<PerformanceDto> items = IntStream.range(0, 10)
                .mapToObj(i -> PerformanceDto.builder()
                        .performanceId(100L + i)
                        .title("뮤지컬-" + i)
                        .date(Instant.parse("2025-09-" + String.format("%02d", (i % 28) + 1) + "T00:00:00Z"))
                        .build())
                .toList();
        when(performanceMapper.searchPerformancesByKeyword(keyword, expectedOffset, size))
                .thenReturn(items);

        // When
        PagingResponse<PerformanceDto> result = performanceService.searchPerformances(keyword, page, size);

        // Then
        assertThat(result.content()).hasSize(10);
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalElements()).isEqualTo(25);
        assertThat(result.totalPages()).isEqualTo(3); // ceil(25/10)=3
        assertThat(result.isLast()).isFalse();

        verify(performanceMapper).countPerformancesByKeyword(keyword);
        verify(performanceMapper).searchPerformancesByKeyword(keyword, expectedOffset, size);
        verifyNoMoreInteractions(performanceMapper);
    }

    @Test
    @DisplayName("키워드 'zzxy'과 같이 관련 없는 키워드를 검색하면 결과가 없어 빈 목록과 total=0,totalPages=0, 마지막 페이지가 반환된다.")
    void TC_PERFORMANCE_008() {
        // Given
        String keyword = "zzxy";
        int page = 0, size = 10;

        when(performanceMapper.countPerformancesByKeyword(keyword)).thenReturn(0L);

        // When
        PagingResponse<PerformanceDto> result = performanceService.searchPerformances(keyword, page, size);

        // Then
        assertThat(result.content()).isEmpty();
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalElements()).isZero();
        assertThat(result.totalPages()).isZero();
        assertThat(result.isLast()).isTrue();

        verify(performanceMapper).countPerformancesByKeyword(keyword);
        verify(performanceMapper, never()).searchPerformancesByKeyword(anyString(), anyInt(), anyInt());
        verifyNoMoreInteractions(performanceMapper);
    }

    @Test
    @DisplayName("같은 장르의 다른 공연을 조회하면 자기 자신과 종료 공연을 제외한 목록이 반환된다.")
    void TC_PERFORMANCE_009() {
        // Given
        Long performanceId = 10L;
        Long genreId = 7L;

        when(performanceMapper.findGenreIdByPerformanceId(performanceId)).thenReturn(genreId);

        List<PerformanceDto> related = List.of(
                PerformanceDto.builder().performanceId(101L).title("동장르-공연1")
                        .date(Instant.parse("2025-09-01T00:00:00Z")).build(),
                PerformanceDto.builder().performanceId(102L).title("동장르-공연2")
                        .date(Instant.parse("2025-09-10T00:00:00Z")).build()
        );
        when(performanceMapper.findRelatedPerformances(genreId, performanceId)).thenReturn(related);

        // When
        List<PerformanceDto> result = performanceService.getRelatedPerformances(performanceId);

        // Then
        assertThat(result).hasSize(2).containsExactlyElementsOf(related);

        verify(performanceMapper).findGenreIdByPerformanceId(performanceId);
        verify(performanceMapper).findRelatedPerformances(genreId, performanceId);
        verifyNoMoreInteractions(performanceMapper);
    }

    @Test
    @DisplayName("대상 공연의 장르를 찾지 못하면 '공연을 찾을 수 없음' 오류가 발생한다.")
    void TC_PERFORMANCE_010() {
        // Given
        Long performanceId = 10L;
        when(performanceMapper.findGenreIdByPerformanceId(performanceId)).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> performanceService.getRelatedPerformances(performanceId))
                .isInstanceOf(BusinessException.class);

        verify(performanceMapper).findGenreIdByPerformanceId(performanceId);
        verify(performanceMapper, never()).findRelatedPerformances(anyLong(), anyLong());
        verifyNoMoreInteractions(performanceMapper);
    }

    @Test
    @DisplayName("HOST 본인이 자신의 공연 목록을 조회하면 최신 생성일 순으로 반환된다.")
    void TC_PERFORMANCE_011_HOST_OK() {
        // Given
        Long memberId = 100L;
        Member me = stubMember(MemberRole.HOST);
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(me));

        var now = Instant.parse("2025-08-24T12:00:00Z");
        List<PerformanceHostDto> expected = List.of(
                PerformanceHostDto.builder().performanceId(3L).title("최신").createdDate(now.plusSeconds(120)).build(),
                PerformanceHostDto.builder().performanceId(2L).title("이전").createdDate(now.plusSeconds(60)).build(),
                PerformanceHostDto.builder().performanceId(1L).title("더이전").createdDate(now).build()
        );
        when(performanceMapper.findPerformancesByMemberId(memberId)).thenReturn(expected);

        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getSignInMemberId).thenReturn(memberId);

            // When
            List<PerformanceHostDto> result = performanceService.getMyPerformances(memberId);

            // Then
            assertThat(result).containsExactlyElementsOf(expected);
            assertThat(result.get(0).getCreatedDate()).isAfter(result.get(1).getCreatedDate());
            assertThat(result.get(1).getCreatedDate()).isAfter(result.get(2).getCreatedDate());

            verify(memberRepository).findById(memberId);
            verify(performanceMapper).findPerformancesByMemberId(memberId);
            verifyNoMoreInteractions(performanceMapper);
        }
    }

    @Test
    @DisplayName("HOST가 아니면 내 공연 목록 조회가 NO_PERMISSION으로 거절된다.")
    void TC_PERFORMANCE_011_FORBIDDEN_NON_HOST() {
        // Given
        Long signInId = 100L;
        Long targetId = 100L;

        Member me = stubMember(MemberRole.MEMBER); // HOST 아님
        when(memberRepository.findById(signInId)).thenReturn(Optional.of(me));

        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getSignInMemberId).thenReturn(signInId);

            // When & Then
            assertThatThrownBy(() -> performanceService.getMyPerformances(targetId))
                    .isInstanceOf(BusinessException.class);

            verify(memberRepository).findById(signInId);
            verify(performanceMapper, never()).findPerformancesByMemberId(anyLong());
        }
    }

    @Test
    @DisplayName("HOST라도 본인이 아닌 memberId로 요청하면 NO_PERMISSION으로 거절된다.")
    void TC_PERFORMANCE_011_FORBIDDEN_NOT_SELF() {
        // Given
        Long signInId = 100L;
        Long otherId = 200L;

        Member me = stubMember(MemberRole.HOST);
        when(memberRepository.findById(signInId)).thenReturn(Optional.of(me));

        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getSignInMemberId).thenReturn(signInId);

            // When & Then
            assertThatThrownBy(() -> performanceService.getMyPerformances(otherId))
                    .isInstanceOf(BusinessException.class);

            verify(memberRepository).findById(signInId);
            verify(performanceMapper, never()).findPerformancesByMemberId(anyLong());
        }
    }

    @Test
    @DisplayName("로그인 사용자를 찾지 못하면 MEMBER_NOT_FOUND 오류가 발생한다.")
    void TC_PERFORMANCE_011_MEMBER_NOT_FOUND() {
        // Given
        Long signInId = 100L;
        when(memberRepository.findById(signInId)).thenReturn(Optional.empty());

        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getSignInMemberId).thenReturn(signInId);

            // When & Then
            assertThatThrownBy(() -> performanceService.getMyPerformances(signInId))
                    .isInstanceOf(BusinessException.class);

            verify(memberRepository).findById(signInId);
            verify(performanceMapper, never()).findPerformancesByMemberId(anyLong());
        }
    }

    private Member stubMember(MemberRole role) {
        Member m = mock(Member.class);
        when(m.getMemberRole()).thenReturn(role);
        return m;
    }

}