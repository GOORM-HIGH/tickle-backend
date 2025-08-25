package com.profect.tickle.domain.performance.mapper;

import com.profect.tickle.domain.performance.dto.response.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@ActiveProfiles("test")
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@Rollback
@Sql(scripts = "classpath:sql/schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:sql/data.sql",   executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:sql/cleanup.sql",executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD) // DROP/정리면 여기
class PerformanceMapperTest {

    @Autowired PerformanceMapper mapper;
    @Autowired NamedParameterJdbcTemplate jdbc;

    void markEnded(long performanceId) {
        jdbc.update("UPDATE performance SET status_id = 3 WHERE performance_id = :id",
                Map.of("id", performanceId));
    }

    void markDeleted(long performanceId) {
        jdbc.update("UPDATE performance SET performance_deleted_at = NOW() WHERE performance_id = :id",
                Map.of("id", performanceId));
    }

    void setLookCount(long performanceId, int look) {
        jdbc.update("UPDATE performance SET performance_look_count = :lc WHERE performance_id = :id",
                Map.of("lc", look, "id", performanceId));
    }

    long insertPerf(String title, Instant start, Instant end, int statusId, int look, boolean deleted, Long genreId, Long memberId) {
        Long nextId = jdbc.queryForObject("SELECT COALESCE(MAX(performance_id), 0) + 1 FROM performance", Map.of(), Long.class);

        var params = new MapSqlParameterSource()
                .addValue("id", nextId)  // 명시적으로 ID 지정
                .addValue("title", title)
                .addValue("img", title + ".jpg")
                .addValue("start", start)
                .addValue("date", start)
                .addValue("end", end)
                .addValue("runtime", 150)
                .addValue("price", "70000")
                .addValue("genreId", genreId == null ? 1L : genreId)
                .addValue("hallId", 1L)
                .addValue("memberId", memberId == null ? 2L : memberId)
                .addValue("statusId", statusId)
                .addValue("isEvent", false)
                .addValue("look", look)
                .addValue("createdAt", Instant.now())
                .addValue("updatedAt", Instant.now())
                .addValue("deletedAt", deleted ? Instant.now() : null);

        jdbc.update("""
        INSERT INTO performance (performance_id, performance_title, performance_img, performance_start_date, performance_date,
                                 performance_end_date, performance_runtime, performance_price,
                                 genre_id, hall_id, member_id, status_id, performance_is_event,
                                 performance_look_count, performance_created_at, performance_updated_at, performance_deleted_at)
        VALUES (:id, :title, :img, :start, :date, :end, :runtime, :price,
                :genreId, :hallId, :memberId, :statusId, :isEvent,
                :look, :createdAt, :updatedAt, :deletedAt)
    """, params);

        return nextId;
    }

    @Test
    @DisplayName("장르별 공연 목록 조회 - 종료된 공연 제외하고 시작일 순 정렬된 페이징 결과 반환")
    void TC_PERFORMANCE_101() {
        // given
        markEnded(13);
        markEnded(14);

        // when
        List<PerformanceDto> page = mapper.findPerformancesByGenre(1L, 20, 10);

        // then
        assertThat(page).hasSizeLessThanOrEqualTo(10);
        assertThat(page).isEmpty();
    }

    @Test
    @DisplayName("장르별 공연 총 개수 조회 - 종료된 공연을 제외한 전체 카운트 반환")
    void TC_PERFORMANCE_102() {
        // given
        markEnded(13);
        markEnded(14);

        // when
        int cnt = mapper.countPerformancesByGenre(1L);

        // then
        assertThat(cnt).isEqualTo(13); // 15개 중 2개 종료 → 13개
    }

    @Test
    @DisplayName("공연 상세 정보 조회 성공 - 삭제되지 않은 공연의 모든 상세 정보 반환")
    void TC_PERFORMANCE_103() {
        // when
        PerformanceDetailDto d = mapper.findDetailById(1L);

        // then
        assertThat(d).isNotNull();
        assertThat(d.getTitle()).isEqualTo("레미제라블");
        assertThat(d.getRuntime()).isNotNull();
        assertThat(d.getPrice()).isNotBlank();
        assertThat(d.getHallAddress()).isNotBlank();
        assertThat(d.getHostBizName()).isNotBlank();
        assertThat(d.getStartDate()).isNotNull();
        assertThat(d.getEndDate()).isNotNull();
        assertThat(d.getStatusDescription()).isNotBlank();
    }

    @Test
    @DisplayName("공연 상세 정보 조회 실패 - 삭제된 공연에 대해 null 반환")
    void TC_PERFORMANCE_104() {
        // given
        markDeleted(2L);

        // when
        PerformanceDetailDto d = mapper.findDetailById(2L);

        // then - 상세 결과가 나오지 않는다(보이지 않음).
        assertThat(d).isNull();
    }

    @Test
    @DisplayName("공연 조회수 증가 - 기존 조회수에서 정확히 1 증가")
    void TC_PERFORMANCE_105() {
        // given
        Integer before = jdbc.queryForObject(
                "SELECT performance_look_count FROM performance WHERE performance_id=:id",
                Map.of("id", 1L), Integer.class);
        assertThat(before).isNotNull();

        // when
        mapper.increaseLookCount(1L);

        // then
        Integer after = jdbc.queryForObject(
                "SELECT performance_look_count FROM performance WHERE performance_id=:id",
                Map.of("id", 1L), Integer.class);
        assertThat(after).isEqualTo(before + 1);
    }

    @Test
    @DisplayName("장르별 인기 공연 Top10 조회 - 종료 제외하고 조회수 높은 순으로 정렬")
    void TC_PERFORMANCE_106() {
        // given
        markEnded(13);
        markEnded(14);

        setLookCount(5L, 999);
        setLookCount(1L, 500);
        setLookCount(3L, 500);

        // when
        List<PerformanceDto> list = mapper.findTop10ByGenre(1L);

        // then
        assertThat(list).hasSizeLessThanOrEqualTo(10);
        assertThat(list).extracting(PerformanceDto::getPerformanceId)
                .contains(5L, 1L, 3L)
                .doesNotContain(13L, 14L); // 종료 제외
    }

    @Test
    @DisplayName("전체 인기 공연 Top10 조회 - 모든 장르에서 조회수 높은 순으로 정렬")
    void TC_PERFORMANCE_107() {
        // given
        markEnded(13);
        markEnded(14);

        setLookCount(4L, 777);
        setLookCount(6L, 777);
        setLookCount(2L, 100);

        // when
        List<PerformanceDto> list = mapper.findTop10ByClickCount();

        // then
        assertThat(list).hasSizeLessThanOrEqualTo(10);
        assertThat(list).extracting(PerformanceDto::getPerformanceId)
                .contains(4L, 6L)
                .doesNotContain(13L, 14L);
    }

    @Test
    @DisplayName("오픈 예정 공연 Top4 조회 - 미래 시작 공연만 시작일 빠른 순으로 정렬")
    void TC_PERFORMANCE_108() {
        // given
        Instant dbNow = jdbc.queryForObject("SELECT NOW()", Map.of(), java.sql.Timestamp.class).toInstant();

        insertPerf("미래A", dbNow.plusSeconds(86400 * 3), dbNow.plusSeconds(86400 * 5), 1, 0, false, 1L, 2L);
        insertPerf("미래B", dbNow.plusSeconds(86400 * 1), dbNow.plusSeconds(86400 * 2), 1, 0, false, 1L, 2L);
        insertPerf("미래C", dbNow.plusSeconds(86400 * 2), dbNow.plusSeconds(86400 * 3), 1, 0, false, 1L, 2L);
        insertPerf("미래D", dbNow.plusSeconds(86400 * 4), dbNow.plusSeconds(86400 * 6), 1, 0, false, 1L, 2L);

        // when
        List<PerformanceDto> list = mapper.findTop4UpcomingPerformances(LocalDateTime.now());

        // then
        assertThat(list).hasSizeLessThanOrEqualTo(4);
        assertThat(list).extracting(PerformanceDto::getTitle)
                .containsExactly("미래B", "미래C", "미래A", "미래D");
    }

    @Test
    @DisplayName("키워드로 공연 검색 - 제목에 키워드 포함하고 종료되지 않은 공연을 날짜순 정렬")
    void TC_PERFORMANCE_109() {
        // given
        long a = insertPerf("뮤지 갈라 1", Instant.now().plusSeconds(10000), Instant.now().plusSeconds(20000), 2, 0, false, 1L, 2L);
        long b = insertPerf("뮤지 갈라 2", Instant.now().plusSeconds(5000),  Instant.now().plusSeconds(15000), 2, 0, false, 1L, 2L);
        long c = insertPerf("뮤지 폐막",   Instant.now().plusSeconds(7000),  Instant.now().plusSeconds(17000), 3, 0, false, 1L, 2L); // 종료(제외)

        // when
        List<PerformanceDto> list = mapper.searchPerformancesByKeyword("뮤지", 10, 0);

        // then - 제목에 키워드를 포함하고 종료되지 않은 공연만 보인다. 날짜가 빠른 순으로 정렬된다.
        assertThat(list).extracting(PerformanceDto::getPerformanceId)
                .contains(a, b)
                .doesNotContain(c); // 종료 제외
        assertThat(list).extracting(PerformanceDto::getTitle)
                .containsSubsequence("뮤지 갈라 2", "뮤지 갈라 1");
    }

    @Test
    @DisplayName("키워드 검색 결과 총 개수 조회 - 종료 여부 무관하게 전체 매칭 개수 반환")
    void TC_PERFORMANCE_110() {
        // given
        insertPerf("뮤지 AAA", Instant.now().plusSeconds(10000), Instant.now().plusSeconds(20000), 2, 0, false, 1L, 2L);
        insertPerf("뮤지 BBB", Instant.now().plusSeconds(5000),  Instant.now().plusSeconds(15000), 2, 0, false, 1L, 2L);
        insertPerf("뮤지 종료", Instant.now().plusSeconds(7000),  Instant.now().plusSeconds(17000), 3, 0, false, 1L, 2L); // 종료

        // when
        long count = mapper.countPerformancesByKeyword("뮤지");
        List<PerformanceDto> list = mapper.searchPerformancesByKeyword("뮤지", 100, 0); // 종료 제외

        // then
        assertThat(count).isGreaterThan(list.size());
    }

    @Test
    @DisplayName("연관 공연 추천 목록 조회 - 같은 장르에서 자신 제외하고 인기순으로 정렬")
    void TC_PERFORMANCE_111() {
        // given
        setLookCount(3L, 100);
        setLookCount(4L, 100);
        setLookCount(5L, 50);
        setLookCount(6L, 10);
        setLookCount(7L, 5);

        markEnded(13);
        markEnded(14);

        // when
        List<PerformanceDto> list = mapper.findRelatedPerformances(1L, 1L);

        // then
        assertThat(list).hasSizeLessThanOrEqualTo(5);
        assertThat(list).extracting(PerformanceDto::getPerformanceId).doesNotContain(1L); // 종료 제외
    }

    @Test
    @DisplayName("사용자가 등록한 공연 목록 조회 - 삭제되지 않은 공연만 최신순으로 정렬")
    void TC_PERFORMANCE_112() {
        // given
        markDeleted(3L);

        // when
        List<PerformanceHostDto> list = mapper.findPerformancesByMemberId(2L);

        // then
        assertThat(list).isNotEmpty();
        assertThat(list).extracting(PerformanceHostDto::getPerformanceId).doesNotContain(3L);

        Instant firstCreated = list.getFirst().getCreatedDate();
        Instant lastCreated = list.get(list.size() - 1).getCreatedDate();
        assertThat(firstCreated).isAfterOrEqualTo(lastCreated);
    }
}