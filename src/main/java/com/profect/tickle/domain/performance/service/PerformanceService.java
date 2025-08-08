package com.profect.tickle.domain.performance.service;

import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.domain.performance.dto.request.PerformanceRequestDto;
import com.profect.tickle.domain.performance.dto.request.UpdatePerformanceRequestDto;
import com.profect.tickle.domain.performance.dto.response.*;
import com.profect.tickle.domain.performance.entity.Genre;
import com.profect.tickle.domain.performance.entity.Hall;
import com.profect.tickle.domain.performance.entity.Performance;
import com.profect.tickle.domain.performance.mapper.PerformanceMapper;
import com.profect.tickle.domain.performance.repository.GenreRepository;
import com.profect.tickle.domain.performance.repository.HallRepository;
import com.profect.tickle.domain.performance.repository.PerformanceRepository;
import com.profect.tickle.domain.reservation.repository.SeatTemplateRepository;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.paging.PagingResponse;
import com.profect.tickle.global.security.util.SecurityUtil;
import com.profect.tickle.global.status.Status;
import com.profect.tickle.global.status.repository.StatusRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PerformanceService {

    private final PerformanceRepository performanceRepository;
    private final MemberRepository memberRepository;
    private final GenreRepository genreRepository;
    private final HallRepository hallRepository;
    private final StatusRepository statusRepository;
    private final SeatTemplateRepository seatTemplateRepository;
    //    private final SeatService seatService;
    private final PerformanceMapper performanceMapper;

    public List<GenreDto> getAllGenre() {
        return performanceMapper.findAllGenres();
    }

    public PagingResponse<PerformanceDto> getPerformancesByGenre(Long genreId, int page, int size) {
        int offset = page * size;

        List<PerformanceDto> contents = performanceMapper.findPerformancesByGenre(genreId, offset, size);
        int totalCount = performanceMapper.countPerformancesByGenre(genreId);

        int totalPages = (int) Math.ceil((double) totalCount / size);
        boolean isLast = page + 1 >= totalPages;

        return new PagingResponse<>(
                contents,
                page,
                size,
                totalCount,
                totalPages,
                isLast
        );
    }

    public List<PerformanceDto> getTop10ByGenre(Long genreId) {
        return performanceMapper.findTop10ByGenre(genreId);
    }

    public List<PerformanceDto> getTop10Performances() {
        return performanceMapper.findTop10ByClickCount();
    }

    public PerformanceDetailDto getPerformanceDetail(Long performanceId) {
        PerformanceDetailDto result = performanceMapper.findDetailById(performanceId);
        if (result == null) {
            throw new BusinessException(ErrorCode.PERFORMANCE_NOT_FOUND);
        }

        performanceMapper.increaseLookCount(performanceId);

        return result;
    }

    public List<PerformanceDto> getTop4UpcomingPerformances() {
        return performanceMapper.findTop4UpcomingPerformances();
    }

    public PagingResponse<PerformanceDto> searchPerformances(String keyword, int page, int size) {
        int offset = page * size;

        List<PerformanceDto> searchResult = performanceMapper.searchPerformancesByKeyword(keyword, size, offset);
        long totalCount = performanceMapper.countPerformancesByKeyword(keyword);

        int totalPages = (int) Math.ceil((double) totalCount / size);
        boolean isLast = page + 1 >= totalPages;

        return new PagingResponse<>(
                searchResult,
                page,
                size,
                totalCount,
                totalPages,
                isLast
        );
    }

    public List<PerformanceDto> getRelatedPerformances(Long performanceId) {
        Long genreId = performanceMapper.findGenreIdByPerformanceId(performanceId);
        if (genreId == null) {
            throw new BusinessException(ErrorCode.PERFORMANCE_NOT_FOUND);
        }

        return performanceMapper.findRelatedPerformances(genreId, performanceId);
    }

    @Transactional
    public PerformanceResponseDto createPerformance(PerformanceRequestDto dto) {
        Long signInMemberId = SecurityUtil.getSignInMemberId();

        Member member = memberRepository.findById(signInMemberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Genre genre = genreRepository.findById(dto.getGenreId())
                .orElseThrow(() -> new BusinessException(ErrorCode.GENRE_NOT_FOUND));

        Hall hall = hallRepository.findByTypeAndAddress(dto.getHallType(), dto.getHallAddress())
                .orElseGet(() -> {
                    try {
                        return hallRepository.save(Hall.builder()
                                .type(dto.getHallType())
                                .address(dto.getHallAddress().trim())
                                .build());
                    } catch (DataIntegrityViolationException e) {
                        return hallRepository.findByTypeAndAddress(dto.getHallType(), dto.getHallAddress())
                                .orElseThrow(() -> e);
                    }
                });

        Status status = statusRepository.findById(1L)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEFAULT_STATUS_NOT_FOUND));

        Integer minPrice = seatTemplateRepository.findMinPriceByHallType(dto.getHallType());
        Integer maxPrice = seatTemplateRepository.findMaxPriceByHallType(dto.getHallType());
        if (minPrice == null || maxPrice == null) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
        String priceRange = minPrice + " ~ " + maxPrice;

        Performance performance = Performance.create(dto, member, genre, hall, status, priceRange);
        performanceRepository.save(performance);

        return PerformanceResponseDto.from(performance);
    }

    @Transactional
    public PerformanceResponseDto updatePerformance(Long performanceId, UpdatePerformanceRequestDto dto) {
        Performance performance = performanceRepository.findById(performanceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PERFORMANCE_NOT_FOUND));

        performance.updateFrom(dto);

        return PerformanceResponseDto.from(performance);
    }

    @Transactional
    public void deletePerformance(Long performanceId, Long memberId) {
        Performance performance = performanceRepository.findActiveById(performanceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PERFORMANCE_NOT_FOUND));

        if (!performance.getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        performance.markAsDeleted();
    }

    public List<PerformanceHostDto> getMyPerformances(Long memberId) {
        return performanceMapper.findPerformancesByMemberId(memberId);
    }



}
