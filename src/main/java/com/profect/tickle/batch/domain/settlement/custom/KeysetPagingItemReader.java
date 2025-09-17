package com.profect.tickle.batch.domain.settlement.custom;

import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;

import java.io.Serializable;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

public class KeysetPagingItemReader<T>
        implements ItemStreamReader<T> {

    private final SqlSessionTemplate sqlSessionTemplate;
    private final String queryId;
    private final Instant contextKeyStartTime;
    private final Instant contextKeyLastTime;
    private final Long contextKeyLastId;
    private final int pageSize;
    private final Function<T,Long> idExtractor;

    private SqlSession session;
    private List<T> buffer;
    private Iterator<T> bufferIt;
    private Instant now;
    private Instant lastTime;
    private long lastProcessedId;

    public KeysetPagingItemReader(
//            SqlSessionFactory sqlSessionFactory,
            SqlSessionTemplate sqlSessionTemplate,
            String queryId,
            Instant contextKeyStartTime,
            Instant contextKeyLastTime,
            Long contextKeyLastId,
            int pageSize,
            Function<T,Long> idExtractor) {
//        this.sqlSessionFactory = sqlSessionFactory;
        this.sqlSessionTemplate = sqlSessionTemplate;
        this.queryId = queryId;
        this.contextKeyStartTime = contextKeyStartTime;
        this.contextKeyLastTime = contextKeyLastTime;
        this.contextKeyLastId = contextKeyLastId;
        this.pageSize = pageSize;
        this.idExtractor = idExtractor;
    }

    @Override
    public T read() throws Exception {
        // buffer 가 비었으면 새 페이지 로딩
        if (bufferIt == null || !bufferIt.hasNext()) {
            loadPage();
        }
        if (bufferIt == null || !bufferIt.hasNext()) {
            return null; // 더 이상 읽을 게 없으면 null 리턴
        }
        T item = bufferIt.next();

        this.lastProcessedId = idExtractor.apply(item);
        // DTO 에서 ID 를 추출해서 lastProcessedId 를 갱신
//        if (item instanceof SettlementDetailFindTargetDto dto) {
//            lastProcessedId = dto.getReservationId();
//        }

        return item;
    }

    private void loadPage() {
        Map<String, Object> params = new HashMap<>();
        params.put("now", now);
//        params.put("lastTimeSeconds", lastTime);
        params.put("lastProcessedId", lastProcessedId);
        params.put("pageSize", pageSize);

        System.out.println("loadPage 파라미터 시작시간 / 마지막 배치 시간 / 마지막 배치 아이디 :::::::::: " + now + " / " + lastTime + " / " + lastProcessedId);

        @SuppressWarnings("unchecked")
        List<T> page = (List<T>) sqlSessionTemplate.selectList(queryId, params);
        System.out.println("loadPage 리스트 사이즈 체크 ::::::::::: " + page.size());
        if (page == null || page.isEmpty()) {
            buffer = Collections.emptyList();
            System.out.println("loadPage page ::::::::::::::::::::::::: null");
        } else {
            buffer = page;
            System.out.println("loadPage page ::::::::::::::::::::::::: not null");
        }
        bufferIt = buffer.iterator();
    }

    // 스텝이 시작될 때 메타테이블에서 lastTime, lastId 를 읽어서 초기화
    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        Instant now = contextKeyStartTime;
        Instant lastTime = contextKeyLastTime;
        Long lastId = contextKeyLastId;

        System.out.println("open 파라미터 시작시간 / 마지막 배치 시간 / 마지막 배치 아이디 :::::::::: " + now + " / " + lastTime + " / " + lastId);

        this.now = now;
        this.lastTime = lastTime;
        this.lastProcessedId = lastId;
    }

    // 스텝 재시작 시나 checkpoint 시점에 lastId,lastTime 를 보존
    @Override
    public void update(ExecutionContext executionContext)
            throws ItemStreamException {
        executionContext.put("settlementBatchStartedAt", now);
        executionContext.put("lastTimeSeconds", lastTime);
        executionContext.putLong("lastProcessedId", lastProcessedId);
    }

    @Override
    public void close() throws ItemStreamException {
        if (session != null) {
            session.close();
        }
    }
}
