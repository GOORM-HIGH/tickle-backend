package com.profect.tickle.domain.performance.mapper;

import com.profect.tickle.domain.performance.dto.response.GenreDto;
import com.profect.tickle.domain.performance.dto.response.PerformanceDetailDto;
import com.profect.tickle.domain.performance.dto.response.PerformanceDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.data.repository.query.Param;

import java.util.List;

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

    List<PerformanceDto> findTop10ByGenre(@Param("genreId") Long genreId);

    List<PerformanceDto> findTop100ByClickCount();

    List<PerformanceDto> findTop4UpcomingPerformances();

    List<PerformanceDto> searchPerformancesByKeyword(
            @Param("keyword") String keyword,
            @Param("limit") int limit,
            @Param("offset") int offset
    );
    long countPerformancesByKeyword(@Param("keyword") String keyword);



}
