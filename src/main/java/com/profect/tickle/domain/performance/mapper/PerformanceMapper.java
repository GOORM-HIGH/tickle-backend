package com.profect.tickle.domain.performance.mapper;

import com.profect.tickle.domain.performance.dto.response.*;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface PerformanceMapper {

    List<GenreDto> findAllGenres();

    List<PerformanceDto> findPerformancesByGenre(
            @Param("genreId") Long genreId,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    int countPerformancesByGenre(@Param("genreId") Long genreId);

    PerformanceDetailDto findDetailById(@Param("performanceId") Long performanceId);

    void increaseLookCount(@Param("performanceId") Long performanceId);

    List<PerformanceDto> findTop10ByGenre(@Param("genreId") Long genreId);

    List<PerformanceDto> findTop10ByClickCount();

    List<PerformanceDto> findTop4UpcomingPerformances();

    List<PerformanceDto> searchPerformancesByKeyword(
            @Param("keyword") String keyword,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    long countPerformancesByKeyword(@Param("keyword") String keyword);

    List<PerformanceDto> findRelatedPerformances(
            @Param("genreId") Long genreId,
            @Param("performanceId") Long performanceId
    );

    Long findGenreIdByPerformanceId(@Param("performanceId") Long performanceId);

    List<PerformanceHostDto> findPerformancesByMemberId(Long memberId);

    List<PerformanceScrapDto> findScrappedPerformancesByMemberId(Long memberId);

    Boolean isScrapped(Long memberId, Long performanceId);

    PerformanceDto findByReservationId(Long reservationId);

    Optional<PerformanceServiceDto> findById(@Param("id") Long id);
}
