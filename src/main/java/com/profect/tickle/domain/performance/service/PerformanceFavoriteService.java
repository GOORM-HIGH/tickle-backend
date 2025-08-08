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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PerformanceFavoriteService {

    private final PerformanceFavoriteRepository favoriteRepository;
    private final MemberRepository memberRepository;
    private final PerformanceRepository performanceRepository;
    private final PerformanceMapper performanceMapper;

    public void scrapPerformance(Long performanceId, Long memberId) {
        if (favoriteRepository.existsByMemberIdAndPerformanceId(memberId, performanceId)) {
            throw new BusinessException(ErrorCode.ALREADY_SCRAPPED);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Performance performance = performanceRepository.findById(performanceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PERFORMANCE_NOT_FOUND));

        favoriteRepository.save(PerformanceFavorite.from(member, performance));
    }

    public void cancelScrap(Long performanceId, Long memberId) {
        PerformanceFavorite favorite = favoriteRepository.findByMemberIdAndPerformanceId(memberId, performanceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FAVORITE_NOT_FOUND));

        favoriteRepository.delete(favorite);
    }

    public List<PerformanceScrapDto> getScrappedPerformances(Long memberId) {
        return performanceMapper.findScrappedPerformancesByMemberId(memberId);
    }

    public boolean isScrapped(Long memberId, Long performanceId) {
        return Boolean.TRUE.equals(performanceMapper.isScrapped(memberId, performanceId));
    }

}

