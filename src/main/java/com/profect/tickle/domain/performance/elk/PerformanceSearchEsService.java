package com.profect.tickle.domain.performance.elk;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.DisMaxQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchPhrasePrefixQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import co.elastic.clients.json.JsonData;

import com.profect.tickle.domain.performance.dto.response.PerformanceDto;
import com.profect.tickle.global.paging.Cursor;
import com.profect.tickle.global.paging.CursorPageResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PerformanceSearchEsService {
    private final ElasticsearchClient elasticsearchClient;
    private static final String INDEX = "performances_v1";

    public CursorPageResponse<PerformanceDto> search(
            String keyword, int size, Instant cursorDate, Long cursorId
    ) throws IOException {
        String kw = keyword == null ? "" : keyword.trim();
        if (kw.isEmpty()) return new CursorPageResponse<>(List.of(), null, false);

        int pageSize = Math.min(Math.max(size, 1), 100);
        boolean shortKw = kw.length() <= 2;

        Query must = shortKw
                ? MatchQuery.of(m -> m
                .field("performance_title.ngram")
                .query(kw)
                .operator(Operator.And)
        )._toQuery()
                : DisMaxQuery.of(d -> d
                .queries(
                        MatchPhrasePrefixQuery.of(mp -> mp
                                .field("performance_title")
                                .query(kw)
                                .boost(2.0f)
                        )._toQuery(),
                        MatchQuery.of(m -> m
                                .field("performance_title.ngram")
                                .query(kw)
                                .operator(Operator.And)
                                .boost(1.0f)
                        )._toQuery()
                )
                .tieBreaker(0.1)
        )._toQuery();

        SearchRequest.Builder sb = new SearchRequest.Builder()
                .index(INDEX)
                .size(pageSize + 1)
                .trackTotalHits(th -> th.count(3001))
                .query(q -> q.bool(b -> b
                        .filter(f -> f.terms(t -> t.field("status_id")
                                .terms(tf -> tf.value(List.of(FieldValue.of(1), FieldValue.of(2))))))
                        .filter(f -> f.range(r -> r.field("performance_date")
                                .gte(JsonData.of("now/d"))
                                .lt(JsonData.of("now+12M/d"))))
                        .must(must)
                ))
                .sort(s -> s.field(f -> f.field("performance_date").order(SortOrder.Asc)))
                .sort(s -> s.field(f -> f.field("performance_id").order(SortOrder.Asc)));

        if (cursorDate != null && cursorId != null) {
            sb.searchAfter(List.of(
                    FieldValue.of(cursorDate.toEpochMilli()),
                    FieldValue.of(cursorId)
            ));
        }

        SearchResponse<Map> res = elasticsearchClient.search(sb.build(), Map.class);
        List<Hit<Map>> hits = res.hits().hits();
        boolean hasNext = hits.size() > pageSize;
        List<Hit<Map>> pageHits = hasNext ? hits.subList(0, pageSize) : hits;

        List<PerformanceDto> items = new ArrayList<>(pageHits.size());
        for (Hit<Map> h : pageHits) {
            Map s = h.source();
            if (s == null) continue;

            PerformanceDto dto = PerformanceDto.builder()
                    .performanceId(asLong(s.get("performance_id")))
                    .title(asString(s.get("performance_title")))
                    .date(asInstant(s.get("performance_date")))
                    .img(asString(s.get("performance_img")))
                    .build();

            items.add(dto);
        }

        Cursor next = null;
        if (hasNext && !pageHits.isEmpty()) {
            List<FieldValue> sortVals = pageHits.get(pageHits.size() - 1).sort();
            Instant nextDate;
            FieldValue d = sortVals.get(0);
            if (d.isLong()) nextDate = Instant.ofEpochMilli(d.longValue());
            else if (d.isString()) nextDate = asInstant(d.stringValue());
            else nextDate = Instant.ofEpochMilli((long) d.doubleValue());
            Long nextId = sortVals.get(1).longValue();
            next = new Cursor(nextDate, nextId);
        }

        return new CursorPageResponse<>(items, next, hasNext);
    }

    // 빠른 카운트
    public record FastCount(long value, boolean gte, String display) {}

    public FastCount fastCount(String keyword) throws IOException {
        String kw = keyword == null ? "" : keyword.trim();
        if (kw.isEmpty()) return new FastCount(0, false, "0");

        boolean shortKw = kw.length() <= 2;
        Query must = shortKw
                ? MatchQuery.of(m -> m.field("performance_title.ngram")
                .query(kw)
                .operator(Operator.And))._toQuery()
                : MatchPhrasePrefixQuery.of(mp -> mp
                .field("performance_title")
                .query(kw)
        )._toQuery();

        SearchResponse<Void> res = elasticsearchClient.search(s -> s
                        .index(INDEX)
                        .size(0)
                        .trackTotalHits(t -> t.count(3001))
                        .query(q -> q.bool(b -> b
                                .filter(f -> f.terms(t -> t.field("status_id")
                                        .terms(tf -> tf.value(List.of(FieldValue.of(1), FieldValue.of(2))))))
                                .filter(f -> f.range(r -> r.field("performance_date")
                                        .gte(JsonData.of("now/d"))
                                        .lt(JsonData.of("now+12M/d"))))
                                .must(must)
                        ))
                , Void.class);

        long val = res.hits().total() == null ? 0 : res.hits().total().value();
        boolean gte = res.hits().total() != null && res.hits().total().relation() == TotalHitsRelation.Gte;
        return new FastCount(val, gte, gte ? "3,000+" : String.valueOf(val));
    }

    // ===== Helpers =====
    private static String asString(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static Long asLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(String.valueOf(v)); } catch (Exception e) { return null; }
    }

    private static Instant asInstant(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return Instant.ofEpochMilli(n.longValue());

        String s = String.valueOf(v);

        // ISO-8601 포맷
        try {
            return Instant.parse(s);
        } catch (Exception ignore1) {}

        // "yyyy-MM-dd HH:mm:ssZ" 포맷
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssZ");
            return OffsetDateTime.parse(s, formatter).toInstant();
        } catch (Exception ignore2) {}

        // 밀리초(long) 포맷
        try {
            return Instant.ofEpochMilli(Long.parseLong(s));
        } catch (Exception ignore3) {}

        return null;
    }
}
