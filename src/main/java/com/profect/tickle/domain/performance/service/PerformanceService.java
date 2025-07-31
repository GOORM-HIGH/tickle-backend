package com.profect.tickle.domain.performance.service;

import com.profect.tickle.domain.performance.dto.response.GenreDto;
import com.profect.tickle.domain.performance.dto.response.PerformanceDetailDto;
import com.profect.tickle.domain.performance.dto.response.PerformanceDto;
import com.profect.tickle.domain.performance.entity.Performance;
import com.profect.tickle.domain.performance.mapper.PerformanceMapper;
import com.profect.tickle.domain.performance.repository.PerformanceRepository;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.paging.PagingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PerformanceService {

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

    public List<PerformanceDto> getTop100Performances() {
        return performanceMapper.findTop100ByClickCount();
    }

    public PerformanceDetailDto getPerformanceDetail(Long performanceId) {
        PerformanceDetailDto result = performanceMapper.findDetailById(performanceId);
        if (result == null) {
            throw new BusinessException(ErrorCode.PERFORMANCE_NOT_FOUND);
        }
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

}
