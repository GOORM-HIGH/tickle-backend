package com.profect.tickle.domain.performance.controller;

import com.profect.tickle.domain.performance.dto.response.PerformanceScrapDto;
import com.profect.tickle.domain.performance.service.PerformanceFavoriteService;
import com.profect.tickle.global.response.ResultCode;
import com.profect.tickle.global.response.ResultResponse;
import com.profect.tickle.global.security.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "공연", description = "공연 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/performance")
public class PerformanceFavoriteController {

    private final PerformanceFavoriteService performanceFavoriteService;

    @PostMapping("/{performanceId}/scrap")
    @PreAuthorize("hasRole('MEMBER')")
    @Operation(summary = "공연 스크랩", description = "해당 공연을 회원이 스크랩합니다.")
    public ResultResponse<Void> scrapPerformance(@PathVariable Long performanceId) {
        Long memberId = SecurityUtil.getSignInMemberId();
        performanceFavoriteService.scrapPerformance(performanceId, memberId);
        return ResultResponse.ok(ResultCode.PERFORMANCE_SCRAP_SUCCESS);
    }

    @DeleteMapping("/{performanceId}/scrap")
    @PreAuthorize("hasRole('MEMBER')")
    @Operation(summary = "공연 스크랩 취소", description = "해당 공연에 대한 스크랩을 취소합니다.")
    public ResultResponse<Void> cancelScrap(@PathVariable Long performanceId) {
        Long memberId = SecurityUtil.getSignInMemberId();
        performanceFavoriteService.cancelScrap(performanceId, memberId);
        return ResultResponse.ok(ResultCode.PERFORMANCE_SCRAP_CANCEL_SUCCESS);
    }

    @GetMapping("scrap")
    public ResponseEntity<List<PerformanceScrapDto>> getMyScraps() {
        Long memberId = SecurityUtil.getSignInMemberId();
        List<PerformanceScrapDto> scrapList = performanceFavoriteService.getScrappedPerformances(memberId);
        return ResponseEntity.ok(scrapList);
    }

    @GetMapping("/{performanceId}/exists")
    public ResponseEntity<Boolean> isScrapped(@PathVariable Long performanceId) {
        Long memberId = SecurityUtil.getSignInMemberId();
        boolean result = performanceFavoriteService.isScrapped(memberId, performanceId);
        return ResponseEntity.ok(result);
    }

}
