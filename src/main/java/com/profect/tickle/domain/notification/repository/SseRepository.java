package com.profect.tickle.domain.notification.repository;

import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Repository
public class SseRepository {

    // emitterId → Emitter
    private final ConcurrentMap<String, SseEmitter> emittersById = new ConcurrentHashMap<>();

    // memberId → {emitterId set}
    private final ConcurrentMap<Long, CopyOnWriteArraySet<String>> emitterIdsByMember = new ConcurrentHashMap<>();

    // memberId → {eventId → json} (시간순 정렬)
    private final ConcurrentMap<Long, ConcurrentSkipListMap<Long, String>> eventsByMember = new ConcurrentHashMap<>();

    // 저장 (한 유저가 여러 탭을 열 수 있음)
    public void save(long memberId, String emitterId, SseEmitter emitter) {
        emittersById.put(emitterId, emitter);
        emitterIdsByMember.computeIfAbsent(memberId, k -> new CopyOnWriteArraySet<>()).add(emitterId);
    }

    // 개별 emitter 제거
    public void remove(long memberId, String emitterId) {
        emittersById.remove(emitterId);
        var set = emitterIdsByMember.get(memberId);
        if (set != null) {
            set.remove(emitterId);
            if (set.isEmpty()) emitterIdsByMember.remove(memberId);
        }
    }

    // 해당 유저의 모든 emitter 조회 (Emitter만 컬렉션으로)
    public Collection<SseEmitter> getAll(long memberId) {
        var ids = emitterIdsByMember.getOrDefault(memberId, new CopyOnWriteArraySet<>());
        List<SseEmitter> list = new ArrayList<>(ids.size());
        for (String id : ids) {
            SseEmitter e = emittersById.get(id);
            if (e != null) list.add(e);
        }
        return list;
    }

    // emitterId로 단일 조회
    public SseEmitter getByEmitterId(String emitterId) {
        return emittersById.get(emitterId);
    }

    // 사용자별 유실 이벤트 캐시 저장
    public void saveEvent(long memberId, long eventId, String json) {
        eventsByMember.computeIfAbsent(memberId, k -> new ConcurrentSkipListMap<>())
                .put(eventId, json);
    }

    // lastEventId 이후의 이벤트만 가져오기
    public NavigableMap<Long, String> eventsAfter(long memberId, long lastEventId) {
        return eventsByMember.getOrDefault(memberId, new ConcurrentSkipListMap<>())
                .tailMap(lastEventId, false);
    }

    // 너무 오래된 이벤트 정리 (남겨둠: 사용처가 있으면 그대로 사용)
    public void clearEventsBefore(long memberId, long thresholdEventId) {
        ConcurrentSkipListMap<Long, String> map = eventsByMember.get(memberId);
        if (map != null) map.headMap(thresholdEventId, false).clear();
    }

    public void removeAll(long memberId) {
        removeAll(memberId, true); // 기본: 캐시까지 제거
    }

    public void removeAll(long memberId, boolean clearEventCache) {
        // 1) emitterId 집합을 인덱스에서 제거하면서 스냅샷 확보
        var ids = emitterIdsByMember.remove(memberId);
        if (ids != null) {
            // 2) 각 emitterId에 대한 Emitter 레코드 제거
            for (String emitterId : ids) {
                emittersById.remove(emitterId);
            }
        }

        // 3) 이벤트 캐시 처리
        if (clearEventCache) {
            eventsByMember.remove(memberId);
        }
    }

    // memberId → (emitterId → emitter) 형태로 조회 (브로드캐스트용)
    public Map<Long, Map<String, SseEmitter>> getAllWithIdsGroupedByMember() {
        Map<Long, Map<String, SseEmitter>> result = new HashMap<>();
        emitterIdsByMember.forEach((memberId, ids) -> {
            Map<String, SseEmitter> inner = new HashMap<>();
            for (String id : ids) {
                SseEmitter e = emittersById.get(id);
                if (e != null) inner.put(id, e);
            }
            if (!inner.isEmpty()) result.put(memberId, inner);
        });
        return result;
    }

    // memberId → (emitterId → emitter) 형태로 조회 (개별 발송용)
    public Map<String, SseEmitter> getAllWithIds(long memberId) {
        CopyOnWriteArraySet<String> ids = emitterIdsByMember.getOrDefault(memberId, new CopyOnWriteArraySet<>());
        Map<String, SseEmitter> map = new HashMap<>(ids.size());
        for (String id : ids) {
            SseEmitter e = emittersById.get(id);
            if (e != null) map.put(id, e);
        }
        return map;
    }

    // 온라인 여부
    public boolean hasEmitters(long memberId) {
        CopyOnWriteArraySet<String> set = emitterIdsByMember.get(memberId);
        return set != null && !set.isEmpty();
    }

    // 시간+개수 기준으로 리플레이 캐시 정리
    public void trimEvents(long memberId, int maxEntries, long ttlCutoffExclusive) {
        ConcurrentSkipListMap<Long, String> map = eventsByMember.get(memberId);
        if (map == null) return;

        // TTL 정리
        map.headMap(ttlCutoffExclusive, false).clear();

        // 개수 상한
        while (map.size() > maxEntries) {
            map.pollFirstEntry();
        }
    }
}
