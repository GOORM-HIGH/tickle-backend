package com.profect.tickle.domain.performance.controller;

import com.profect.tickle.domain.performance.dto.request.PerformanceRequestDto;
import com.profect.tickle.domain.performance.dto.request.UpdatePerformanceRequestDto;
import com.profect.tickle.domain.performance.dto.response.*;
import com.profect.tickle.domain.performance.mapper.PerformanceMapper;
import com.profect.tickle.domain.performance.service.PerformanceService;
import com.profect.tickle.global.paging.PagingResponse;
import com.profect.tickle.global.response.ResultCode;
import com.profect.tickle.global.response.ResultResponse;
import com.profect.tickle.global.security.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "공연", description = "공연 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/performance")
public class PerformanceController {

    private final PerformanceService performanceService;
    private final PerformanceMapper performanceMapper;

    @Operation(summary = "장르 목록 조회", description = "모든 공연 장르를 조회합니다.")
    @GetMapping("/genre")
    public ResultResponse<List<GenreDto>> getGenres() {
        List<GenreDto> genres = performanceService.getAllGenre();
        return  ResultResponse.of(ResultCode.GENRE_LIST_SUCCESS,genres);
    }

    @Operation(summary = "장르별 공연 목록 조회", description = "장르별로 공연 목록을 8개씩 페이징해 조회합니다.")
    @GetMapping("/genre/{genreId}")
    public ResultResponse<PagingResponse<PerformanceDto>> getPerformancesByGenre(
            @PathVariable Long genreId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size
    ) {
        PagingResponse<PerformanceDto> response = performanceService.getPerformancesByGenre(genreId, page, size);
        return ResultResponse.of(ResultCode.PERFORMANCE_LIST_SUCCESS, response);
    }

    @Operation(summary = "장르별 공연 랭킹", description = "장르별 공연 랭킹 TOP10을 조회합니다.")
    @GetMapping("/genre/{genreId}/ranking")
    public ResultResponse<List<PerformanceDto>> getTop10ByGenre(@PathVariable Long genreId) {
        List<PerformanceDto> result = performanceService.getTop10ByGenre(genreId);
        return ResultResponse.of(ResultCode.PERFORMANCE_GENRE_RANK_SUCCESS, result);
    }

    @Operation(summary = "전체 공연 랭킹", description = "전체 공연 랭킹 TOP10을 조회합니다.")
    @GetMapping("/ranking")
    public ResultResponse<List<PerformanceDto>> getTop10Performances() {
        List<PerformanceDto> performances = performanceService.getTop10Performances();
        return ResultResponse.of(ResultCode.PERFORMANCE_TOP100_SUCCESS, performances);
    }

    @Operation(summary = "공연 상세보기", description = "공연 정보를 상세 조회합니다.")
    @GetMapping("/{performanceId}")
    public ResultResponse<PerformanceDetailDto> getPerformanceDetail(@PathVariable Long performanceId) {
        PerformanceDetailDto detail = performanceService.getPerformanceDetail(performanceId);
        return ResultResponse.of(ResultCode.PERFORMANCE_DETAIL_SUCCESS,detail);
    }

    @Operation(summary = "오픈예정 공연 랭킹", description = "오픈 예정인 공연 4개를 조회합니다.")
    @GetMapping("/open")
    public ResultResponse<List<PerformanceDto>> getOpenPerformances() {
        List<PerformanceDto> popular = performanceService.getTop4UpcomingPerformances();
        return ResultResponse.of(ResultCode.PERFORMANCE_POPULAR_SUCCESS,popular);
    }

    @Operation(summary = "공연 검색", description = "공연이름으로 공연을 검색합니다.")
    @GetMapping("/search/{keyword}")
    public ResultResponse<PagingResponse<PerformanceDto>> searchPerformances(
            @PathVariable String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size
    ) {
        PagingResponse<PerformanceDto> response = performanceService.searchPerformances(keyword, page, size);
        return ResultResponse.of(ResultCode.PERFORMANCE_SEARCH_SUCCESS, response);
    }

    @Operation(summary = "공연 추천", description = "해당 공연과 관련있는 공연을 추천정보를 조회합니다.")
    @GetMapping("/{performanceId}/recommend")
    public ResultResponse<List<PerformanceDto>> getRelatedPerformances(
            @PathVariable Long performanceId
    ) {
        List<PerformanceDto> recommend = performanceService.getRelatedPerformances(performanceId);
        return ResultResponse.of(ResultCode.PERFORMANCE_RECOMMEND_LIST_SUCCESS, recommend);
    }

    @Operation(summary = "공연 등록", description = "HOST 권한으로 공연을 생성합니다.")
    @PostMapping
    @PreAuthorize("hasRole('HOST')")
    public ResultResponse<PerformanceResponseDto> createPerformance(@RequestBody @Valid PerformanceRequestDto request) {
        PerformanceResponseDto response = performanceService.createPerformance(request);
        return ResultResponse.of(ResultCode.PERFORMANCE_CREATE_SUCCESS, response);
    }

    @Operation(summary = "공연 수정", description = "HOST 권한으로 공연을 수정합니다.")
    @PatchMapping("/{performanceId}")
    @PreAuthorize("hasRole('HOST')")
    public ResultResponse<PerformanceResponseDto> updatePerformance(
            @PathVariable Long performanceId,
            @RequestBody UpdatePerformanceRequestDto dto
    ) {
        Long currentMemberId = SecurityUtil.getSignInMemberId();
        PerformanceResponseDto updated = performanceService.updatePerformance(performanceId, dto);
        return ResultResponse.of(ResultCode.PERFORMANCE_UPDATE_SUCCESS, updated);
    }

    @Operation(summary = "공연 삭제", description = "HOST 권한으로 공연을 삭제합니다 (soft delete).")
    @DeleteMapping("/{performanceId}")
    @PreAuthorize("hasRole('HOST')")
    public ResultResponse<Void> deletePerformance(@PathVariable Long performanceId) {
        Long memberId = SecurityUtil.getSignInMemberId();
        performanceService.deletePerformance(performanceId, memberId);
        return ResultResponse.ok(ResultCode.PERFORMANCE_DELETE_SUCCESS);
    }

    @Operation(summary = "생성한 공연 조회", description = "HOST 권한으로 본인이 작성한 공연 목록을 조회합니다.")
    @GetMapping("/host")
    public ResultResponse<List<PerformanceHostDto>> getPerformanceHost() {
        Long memberId = SecurityUtil.getSignInMemberId();
        List<PerformanceHostDto> hostP = performanceService.getMyPerformances(memberId);
        return ResultResponse.of(ResultCode.PERFORMANCE_HOST_SUCCESS,hostP);
    }


}
