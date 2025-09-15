package com.profect.tickle.domain.notification.repository;

import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Repository
public class SseRepository {

    // emitterId → Emitter
    private final Map<String, SseEmitter> emittersById = new ConcurrentHashMap<>();

    // memberId → {emitterId set}
    private final Map<Long, CopyOnWriteArraySet<String>> emitterIdsByMember = new ConcurrentHashMap<>();

    // SSE 연결 저장 (한 유저가 여러 탭을 열 수 있음)
    public void save(long memberId, String emitterId, SseEmitter emitter) {
        emittersById.put(emitterId, emitter);
        emitterIdsByMember.computeIfAbsent(memberId, k -> new CopyOnWriteArraySet<>()).add(emitterId);
    }

    // 개별 emitter 제거
    public void remove(long memberId, String emitterId) {
        emittersById.remove(emitterId);
        Set<String> set = emitterIdsByMember.get(memberId);
        if (set != null) {
            set.remove(emitterId);
            if (set.isEmpty()) emitterIdsByMember.remove(memberId);
        }
    }

    // emitterId로 단일 조회
    public SseEmitter getByEmitterId(String emitterId) {
        return emittersById.get(emitterId);
    }

    // 특정 회원의 모든 emitter 조회 (개별 발송용)
    public Map<String, SseEmitter> getAllWithIds(long memberId) {
        Set<String> ids = emitterIdsByMember.getOrDefault(memberId, new CopyOnWriteArraySet<>());
        Map<String, SseEmitter> map = new HashMap<>(ids.size());
        for (String id : ids) {
            SseEmitter e = emittersById.get(id);
            if (e != null) map.put(id, e);
        }
        return map;
    }

    // 모든 회원별 emitter 조회 (브로드캐스트용)
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

    // 특정 회원의 모든 emitter 제거
    public void removeAll(long memberId) {
        Set<String> ids = emitterIdsByMember.remove(memberId);
        if (ids != null) {
            for (String emitterId : ids) {
                emittersById.remove(emitterId);
            }
        }
    }

    // 전체 연결 수
    public int size() {
        return emittersById.size();
    }

    // 연결된 회원 수
    public int getMemberCount() {
        return emitterIdsByMember.size();
    }
}
