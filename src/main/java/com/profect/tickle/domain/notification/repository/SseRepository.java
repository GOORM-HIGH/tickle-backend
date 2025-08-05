package com.profect.tickle.domain.notification.repository;

import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class SseRepository {

    private final Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();
    private final Map<String, Object> eventCacheMap = new ConcurrentHashMap<>();

    public void save(String id, SseEmitter emitter) {
        emitterMap.put(id, emitter);
    }

    public void deleteById(String id) {
        emitterMap.remove(id);
        eventCacheMap.remove(id);
    }

    public SseEmitter get(String id) {
        return emitterMap.get(id);
    }

    // 이벤트 캐시 저장
    public void saveEvent(String eventId, Object eventData) {
        eventCacheMap.put(eventId, eventData);
    }

    // 캐시 조회
    public Map<String, Object> getEventCache() {
        return eventCacheMap;
    }
}
