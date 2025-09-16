package com.profect.tickle.domain.performance.mapper;

import com.profect.tickle.domain.performance.dto.response.*;
import com.profect.tickle.global.paging.Cursor;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Mapper
public interface PerformanceMapper {

    List<GenreDto> findAllGenres();

    List<PerformanceDto> findPerformancesByGenreCursor(
            @Param("genreId") Long genreId,
            @Param("cursor") Cursor cursor,
            @Param("limit") int limit
    );

    PerformanceDetailDto findDetailById(@Param("performanceId") Long performanceId);

    void increaseLookCount(@Param("performanceId") Long performanceId);

    List<PerformanceDto> findTop10ByGenre(@Param("genreId") Long genreId);

    List<PerformanceDto> findTop10ByClickCount();

    List<PerformanceDto> findTop4UpcomingPerformances(@Param("now") LocalDateTime now);

    List<PerformanceDto> searchPerformancesByKeyword(
            @Param("keyword") String keyword,
            @Param("limit") int limit,
            @Param("cursorDate") Instant cursorDate,
            @Param("cursorId") Long cursorId
    );

    Long countPerformancesByKeyword(@Param("keyword") String keyword);

    List<PerformanceDto> findRelatedPerformances(
            @Param("genreId") Long genreId,
            @Param("performanceId") Long performanceId
    );

    Long findGenreIdByPerformanceId(@Param("performanceId") Long performanceId);

    long countPerformancesByMemberId(@Param("memberId") Long memberId);

    List<PerformanceHostDto> findPerformancesByMemberIdPaged(
            @Param("memberId") Long memberId,
            @Param("offset") int offset,
            @Param("size") int size
    );

    List<PerformanceScrapDto> findScrappedPerformancesByMemberId(Long memberId);

    Boolean isScrapped(Long memberId, Long performanceId);

    PerformanceServiceDto findByReservationId(Long reservationId);

    Optional<PerformanceServiceDto> findById(@Param("id") Long id);
}
