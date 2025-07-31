package com.profect.tickle.domain.performance.controller;

import com.profect.tickle.domain.performance.dto.response.GenreDto;
import com.profect.tickle.domain.performance.dto.response.PerformanceDetailDto;
import com.profect.tickle.domain.performance.dto.response.PerformanceDto;
import com.profect.tickle.domain.performance.service.PerformanceService;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.paging.PagingResponse;
import com.profect.tickle.global.response.ResultCode;
import com.profect.tickle.global.response.ResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/performance")
public class PerformanceController {

    private final PerformanceService performanceService;

    @GetMapping("/genre")
    public ResultResponse<List<GenreDto>> getGenres() {
        List<GenreDto> genres = performanceService.getAllGenre();
        return  ResultResponse.of(ResultCode.GENRE_LIST_SUCCESS,genres);
    }

    @GetMapping("/genre/{genreId}")
    public ResultResponse<PagingResponse<PerformanceDto>> getPerformancesByGenre(
            @PathVariable Long genreId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size
    ) {
        PagingResponse<PerformanceDto> response = performanceService.getPerformancesByGenre(genreId, page, size);
        return ResultResponse.of(ResultCode.PERFORMANCE_LIST_SUCCESS, response);
    }

    @GetMapping("/genre/{genreId}/ranking")
    public ResultResponse<List<PerformanceDto>> getTop10ByGenre(@PathVariable Long genreId) {
        List<PerformanceDto> result = performanceService.getTop10ByGenre(genreId);
        return ResultResponse.of(ResultCode.PERFORMANCE_GENRE_RANK_SUCCESS, result);
    }

    @GetMapping("/ranking")
    public ResultResponse<List<PerformanceDto>> getTop100Performances() {
        List<PerformanceDto> performances = performanceService.getTop100Performances();
        return ResultResponse.of(ResultCode.PERFORMANCE_TOP100_SUCCESS, performances);
    }

    @GetMapping("/{performanceId}")
    public ResultResponse<PerformanceDetailDto> getPerformanceDetail(@PathVariable Long performanceId) {
        PerformanceDetailDto detail = performanceService.getPerformanceDetail(performanceId);
        return ResultResponse.of(ResultCode.PERFORMANCE_DETAIL_SUCCESS,detail);
    }

    @GetMapping("/open")
    public ResultResponse<List<PerformanceDto>> getOpenPerformances() {
        List<PerformanceDto> popular = performanceService.getTop4UpcomingPerformances();
        return ResultResponse.of(ResultCode.PERFORMANCE_POPULAR_SUCCESS,popular);
    }

}
