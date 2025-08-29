package com.profect.tickle.domain.performance.service;

import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.domain.performance.dto.response.PerformanceScrapDto;
import com.profect.tickle.domain.performance.entity.Performance;
import com.profect.tickle.domain.performance.entity.PerformanceFavorite;
import com.profect.tickle.domain.performance.mapper.PerformanceMapper;
import com.profect.tickle.domain.performance.repository.PerformanceFavoriteRepository;
import com.profect.tickle.domain.performance.repository.PerformanceRepository;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PerformanceFavoriteServiceTest {

    @Mock private PerformanceFavoriteRepository favoriteRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private PerformanceRepository performanceRepository;
    @Mock private PerformanceMapper performanceMapper;

    @InjectMocks
    private PerformanceFavoriteService service;

    private final Long memberId = 100L;
    private final Long performanceId = 10L;

    @Test
    @DisplayName("아직 스크랩하지 않은 공연을 스크랩하면 즐겨찾기 등록이 완료된다")
    void TC_FAVORITE_001() {
        // given
        when(favoriteRepository.existsByMemberIdAndPerformanceId(memberId, performanceId)).thenReturn(false);
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(mock(Member.class)));
        when(performanceRepository.findById(performanceId)).thenReturn(Optional.of(mock(Performance.class)));

        // when
        service.scrapPerformance(performanceId, memberId);

        // then
        verify(favoriteRepository).save(any(PerformanceFavorite.class));
    }

    @Test
    @DisplayName("이미 스크랩한 공연을 다시 스크랩하면 '이미 스크랩됨' 오류로 거절된다")
    void TC_FAVORITE_002() {
        // given
        when(favoriteRepository.existsByMemberIdAndPerformanceId(memberId, performanceId)).thenReturn(true);

        // when
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.scrapPerformance(performanceId, memberId));

        // then
        assertEquals(ErrorCode.ALREADY_SCRAPPED, ex.getErrorCode());
        verify(favoriteRepository, never()).save(any());
        verify(memberRepository, never()).findById(any());
        verify(performanceRepository, never()).findById(any());
    }

    @Test
    @DisplayName("회원이 존재하지 않으면 '회원을 찾을 수 없음' 오류로 거절된다")
    void TC_FAVORITE_003() {
        // given
        when(favoriteRepository.existsByMemberIdAndPerformanceId(memberId, performanceId)).thenReturn(false);
        when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

        // when
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.scrapPerformance(performanceId, memberId));

        // then
        assertEquals(ErrorCode.MEMBER_NOT_FOUND, ex.getErrorCode());
        verify(performanceRepository, never()).findById(any());
        verify(favoriteRepository, never()).save(any());
    }

    @Test
    @DisplayName("공연이 존재하지 않으면 '공연을 찾을 수 없음' 오류로 거절된다")
    void TC_FAVORITE_004() {
        // given
        when(favoriteRepository.existsByMemberIdAndPerformanceId(memberId, performanceId)).thenReturn(false);
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(mock(Member.class)));
        when(performanceRepository.findById(performanceId)).thenReturn(Optional.empty());

        // when
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.scrapPerformance(performanceId, memberId));

        // then
        assertEquals(ErrorCode.PERFORMANCE_NOT_FOUND, ex.getErrorCode());
        verify(favoriteRepository, never()).save(any());
    }

    @Test
    @DisplayName("즐겨찾기(스크랩) 취소 시 해당 즐겨찾기가 삭제되어 더 이상 스크랩 상태가 아니다")
    void TC_FAVORITE_005() {
        // given
        PerformanceFavorite favorite = mock(PerformanceFavorite.class);
        when(favoriteRepository.findByMemberIdAndPerformanceId(memberId, performanceId))
                .thenReturn(Optional.of(favorite));

        // when
        service.cancelScrap(performanceId, memberId);

        // then
        verify(favoriteRepository).delete(favorite);
    }

    @Test
    @DisplayName("즐겨찾기(스크랩) 취소 대상이 없으면 '즐겨찾기를 찾을 수 없음' 오류로 거절된다")
    void TC_FAVORITE_006() {
        // given
        when(favoriteRepository.findByMemberIdAndPerformanceId(memberId, performanceId))
                .thenReturn(Optional.empty());

        // when
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.cancelScrap(performanceId, memberId));

        // then
        assertEquals(ErrorCode.FAVORITE_NOT_FOUND, ex.getErrorCode());
        verify(favoriteRepository, never()).delete(any());
    }

    @Test
    @DisplayName("내 스크랩 목록은 매퍼가 준 최신순 결과를 그대로 반환한다")
    void TC_FAVORITE_007() {
        // given
        var dtoNewest = new PerformanceScrapDto(20L, "최신공연","img-new", Instant.now(), null,null);
        var dtoOld    = new PerformanceScrapDto(11L, "이전공연", "img-old", Instant.now(), null,null);
        when(performanceMapper.findScrappedPerformancesByMemberId(memberId))
                .thenReturn(List.of(dtoNewest, dtoOld)); // 최신 먼저라고 가정

        // when
        List<PerformanceScrapDto> result = service.getScrappedPerformances(memberId);

        // then
        assertEquals(2, result.size());
        assertEquals("최신공연", result.get(0).getTitle());
        assertEquals("이전공연", result.get(1).getTitle());
        verify(performanceMapper).findScrappedPerformancesByMemberId(memberId);
    }

    @Test
    @DisplayName("조합이 존재하면 '스크랩한 상태'로 확인된다")
    void TC_FAVORITE_008() {
        // given
        when(performanceMapper.isScrapped(memberId, performanceId)).thenReturn(Boolean.TRUE);

        // when
        boolean scrapped = service.isScrapped(memberId, performanceId);

        // then
        assertTrue(scrapped);
        verify(performanceMapper).isScrapped(memberId, performanceId);
    }

    @Test
    @DisplayName("조합이 없으면 '스크랩하지 않음'으로 확인된다")
    void TC_FAVORITE_009() {
        // given: false
        when(performanceMapper.isScrapped(memberId, performanceId)).thenReturn(Boolean.FALSE);
        assertFalse(service.isScrapped(memberId, performanceId));

        // given: null 방어
        when(performanceMapper.isScrapped(memberId, performanceId)).thenReturn(null);
        assertFalse(service.isScrapped(memberId, performanceId));
    }
}
