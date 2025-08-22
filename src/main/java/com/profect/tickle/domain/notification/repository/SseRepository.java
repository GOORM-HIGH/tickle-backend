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

    // ------------- 저장 구조 설명 -------------
//
// 1) emittersById
//    - 목적: 개별 SSE 연결(탭/창 한 개)을 식별자(emitterId)로 바로 찾아서
//            메시지 전송/종료(disconnect) 등에 사용.
//    - Key  : emitterId (예: "<memberId>_<epochMillis>_<uuid>")
//    - Value: SseEmitter (해당 브라우저 탭과의 SSE 스트림 핸들)
//    - 라이프사이클: connect() 시 put → onCompletion/onTimeout/onError 시 remove
//    - 동시성: ConcurrentMap으로 멀티스레드 안전. 개별 emitter에 대한 O(1) 조회/삭제.
//
// 2) emitterIdsByMember
//    - 목적: 한 사용자(memberId)가 여러 탭을 열 수 있으므로,
//            "그 사용자에게 열린 모든 emitterId"를 빠르게 브로드캐스트하기 위한 인덱스.
//    - Key  : memberId (사용자 단위 식별자)
//    - Value: 그 사용자의 모든 emitterId 집합(Set). CopyOnWriteArraySet으로
//             반복 중 수정 충돌을 피하고, 브로드캐스트 시 안전하게 순회 가능.
//    - 라이프사이클: connect() 시 해당 memberId의 Set에 emitterId 추가 →
//                   해당 emitter 종료 시 Set에서 emitterId 제거(비면 키 삭제).
//    - 사용 예: send(memberId, payload)에서 Set을 순회하며 emittersById로 실제 Emitter 조회.
//
// 3) eventsByMember
//    - 목적: 사용자별 "유실 이벤트 복원(replay)"을 위해 최근 전송 이벤트를 캐시.
//            Last-Event-ID 이후의 이벤트만 정렬 순서대로 재전송.
//    - Key  : memberId (사용자 단위)
//    - Value: (ConcurrentSkipListMap<Long,String>)
//             └ Key   = eventId (단조 증가하는 long; 보통 System.currentTimeMillis())
//             └ Value = 직렬화된 JSON 페이로드(문자열)
//    - 특징: SkipList 기반 정렬 맵 → tailMap(lastEventId, false)로
//            "지정 ID 이후" 구간을 효율적으로 얻어 순서 보장 재전송 가능.
//    - 메모리 관리: 일정 기간/개수 초과 시 headMap(...)으로 오래된 이벤트 정리 권장.
// ------------------------------------------

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

    //개별 emitter 제거
    public void remove(long memberId, String emitterId) {
        emittersById.remove(emitterId);
        var set = emitterIdsByMember.get(memberId);
        if (set != null) {
            set.remove(emitterId);
            if (set.isEmpty()) emitterIdsByMember.remove(memberId);
        }
    }

    // 해당 유저의 모든 emitter 조회
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

    // 너무 오래된 이벤트 정리
    public void clearEventsBefore(long memberId, long thresholdEventId) {
        var map = eventsByMember.get(memberId);
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

    public Map<String, SseEmitter> getAllWithIds(long memberId) {
        CopyOnWriteArraySet<String> ids = emitterIdsByMember.getOrDefault(memberId, new CopyOnWriteArraySet<>());
        Map<String, SseEmitter> map = new HashMap<>(ids.size());
        for (String id : ids) {
            SseEmitter e = emittersById.get(id);
            if (e != null) map.put(id, e);
        }
        return map;
    }
}
