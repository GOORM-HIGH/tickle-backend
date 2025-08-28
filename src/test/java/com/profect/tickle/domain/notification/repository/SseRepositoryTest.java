package com.profect.tickle.domain.notification.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Collection;
import java.util.Map;
import java.util.NavigableMap;

import static org.junit.jupiter.api.Assertions.*;

class SseRepositoryTest {

    @Test
    @DisplayName("save 후 getAll, getByEmitterId 로 조회된다")
    void saveAndGet() {
        SseRepository repo = new SseRepository();
        long memberId = 1L;
        String emitterId = "emitter-1";
        SseEmitter emitter = new SseEmitter();

        repo.save(memberId, emitterId, emitter);

        Collection<SseEmitter> all = repo.getAll(memberId);
        assertEquals(1, all.size());
        assertTrue(all.contains(emitter));

        SseEmitter found = repo.getByEmitterId(emitterId);
        assertEquals(emitter, found);
    }

    @Test
    @DisplayName("remove 하면 getAll 결과에서 빠진다")
    void remove() {
        SseRepository repo = new SseRepository();
        long memberId = 1L;
        String emitterId = "emitter-1";
        SseEmitter emitter = new SseEmitter();
        repo.save(memberId, emitterId, emitter);

        repo.remove(memberId, emitterId);

        assertTrue(repo.getAll(memberId).isEmpty());
        assertNull(repo.getByEmitterId(emitterId));
    }

    @Test
    @DisplayName("saveEvent 후 eventsAfter 로 가져올 수 있다")
    void saveEventAndReplay() {
        SseRepository repo = new SseRepository();
        long memberId = 1L;
        repo.saveEvent(memberId, 100L, "event-100");
        repo.saveEvent(memberId, 200L, "event-200");

        NavigableMap<Long, String> after100 = repo.eventsAfter(memberId, 100L);
        assertEquals(1, after100.size());
        assertTrue(after100.containsKey(200L));
    }

    @Test
    @DisplayName("removeAll 하면 emitter 와 이벤트 캐시가 제거된다")
    void removeAll() {
        SseRepository repo = new SseRepository();
        long memberId = 1L;
        repo.save(memberId, "emitter-1", new SseEmitter());
        repo.saveEvent(memberId, 1L, "event-1");

        repo.removeAll(memberId);

        assertTrue(repo.getAll(memberId).isEmpty());
        assertTrue(repo.eventsAfter(memberId, 0).isEmpty());
    }

    @Test
    @DisplayName("getAllWithIdsGroupedByMember 는 emitterId → Emitter 맵을 반환한다")
    void getAllWithIdsGroupedByMember() {
        SseRepository repo = new SseRepository();
        long memberId = 1L;
        repo.save(memberId, "em1", new SseEmitter());
        repo.save(memberId, "em2", new SseEmitter());

        Map<Long, Map<String, SseEmitter>> grouped = repo.getAllWithIdsGroupedByMember();

        assertTrue(grouped.containsKey(memberId));
        assertEquals(2, grouped.get(memberId).size());
    }

    @Test
    @DisplayName("trimEvents 는 TTL 및 최대 개수 기준으로 정리한다")
    void trimEvents() {
        SseRepository repo = new SseRepository();
        long memberId = 1L;

        // TTL cutoff: 1000
        repo.saveEvent(memberId, 1L, "old");
        repo.saveEvent(memberId, 2L, "old2");
        repo.saveEvent(memberId, 1001L, "recent1");
        repo.saveEvent(memberId, 1002L, "recent2");
        repo.saveEvent(memberId, 1003L, "recent3");

        repo.trimEvents(memberId, 2, 1000L);

        NavigableMap<Long, String> events = repo.eventsAfter(memberId, 0);
        // TTL < 1000 은 제거됨 → 남은 건 1001, 1002, 1003 중 2개만 남음
        assertEquals(2, events.size());
        assertTrue(events.containsKey(1002L) || events.containsKey(1003L));
    }
}
