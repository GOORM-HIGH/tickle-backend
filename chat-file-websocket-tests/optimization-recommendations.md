# 채팅/웹소켓/파일 기능 성능 최적화 방안

## 📊 현재 성능 지표 (20 VUs, 60초 테스트)

### 핵심 지표
- **TPS**: 107.6 requests/s
- **평균 응답시간**: 41.4ms
- **95th percentile**: 155.19ms ✅ (목표: < 1000ms)
- **채팅 API 성공률**: 100% ✅
- **전체 HTTP 성공률**: 85.72% ⚠️

### 문제점
- 읽지 않은 메시지 개수 API: 100% 실패
- HTTP 실패율: 14.28%

## 🚀 최적화 방안

### 1. 데이터베이스 최적화

#### 1.1 HikariCP 커넥션 풀 튜닝
```yaml
# application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 50  # 현재 1000 → 50으로 조정
      minimum-idle: 20       # 현재 10 → 20으로 증가
      connection-timeout: 10000   # 현재 3000000 → 10초로 단축
      idle-timeout: 300000        # 5분 유지
      max-lifetime: 1200000       # 20분 유지
      leak-detection-threshold: 60000  # 1분 누수 감지
```

#### 1.2 채팅 관련 인덱스 최적화
```sql
-- 채팅 메시지 조회 최적화
CREATE INDEX CONCURRENTLY idx_chat_room_created_at ON chat (chat_room_id, chat_created_at);

-- 읽지 않은 메시지 개수 최적화
CREATE INDEX CONCURRENTLY idx_chat_participants_last_read ON chat_participants (chat_room_id, member_id, last_read_message_id);

-- 채팅방 온라인 사용자 최적화
CREATE INDEX CONCURRENTLY idx_chat_participants_online ON chat_participants (chat_room_id, is_online, last_activity_at);
```

#### 1.3 읽기 전용 Replica 활용
```yaml
# 읽기 전용 쿼리를 위한 Replica DB 설정
spring:
  datasource:
    read:
      url: jdbc:postgresql://localhost:54322/postgres
      username: postgres
      password: tickle
```

### 2. 캐싱 전략

#### 2.1 Redis 캐시 도입
```yaml
# Redis 설정 추가
spring:
  redis:
    host: localhost
    port: 6379
    timeout: 3000ms
    lettuce:
      pool:
        max-active: 100
        max-idle: 20
        min-idle: 5
```

#### 2.2 캐싱 적용 대상
- 채팅방 정보: TTL 1시간
- 온라인 사용자 수: TTL 30초
- 사용자 권한 정보: TTL 10분
- 파일 메타데이터: TTL 24시간

### 3. WebSocket/STOMP 최적화

#### 3.1 메시지 브로커 최적화
```yaml
# STOMP 메시지 브로커 설정
spring:
  websocket:
    stomp:
      broker:
        relay:
          enabled: true
          host: localhost
          port: 61613
      heartbeat:
        incoming: 10000  # 10초
        outgoing: 10000  # 10초
```

#### 3.2 세션 관리 최적화
- 세션 타임아웃: 30분
- 하트비트 간격: 10초
- 최대 동시 연결: 10,000개

### 4. 네트워크 프로그래밍 최적화 (강사님 추천)

#### 4.1 Netty 기반 WebSocket 서버 구성
```java
@Configuration
public class NettyWebSocketConfig {
    
    @Bean
    public NettyReactiveWebServerFactory nettyReactiveWebServerFactory() {
        NettyReactiveWebServerFactory factory = new NettyReactiveWebServerFactory();
        
        // TCP 옵션 최적화
        factory.addServerCustomizers(httpServer -> 
            httpServer
                .option(ChannelOption.SO_BACKLOG, 1024)           // 백로그 큐 크기
                .option(ChannelOption.SO_REUSEADDR, true)         // 주소 재사용
                .childOption(ChannelOption.SO_KEEPALIVE, true)    // Keep-Alive
                .childOption(ChannelOption.TCP_NODELAY, true)     // Nagle 알고리즘 비활성화
                .childOption(ChannelOption.SO_RCVBUF, 32 * 1024)  // 수신 버퍼 32KB
                .childOption(ChannelOption.SO_SNDBUF, 32 * 1024)  // 송신 버퍼 32KB
        );
        
        return factory;
    }
}
```

#### 4.2 NIO 기반 파일 업로드 최적화
```java
@Service
public class OptimizedFileService {
    
    // Zero-copy 파일 전송
    public void uploadFileWithZeroCopy(MultipartFile file) {
        try (FileChannel sourceChannel = ((FileInputStream) file.getInputStream()).getChannel();
             FileChannel targetChannel = new FileOutputStream(targetFile).getChannel()) {
            
            // Zero-copy로 파일 전송
            sourceChannel.transferTo(0, sourceChannel.size(), targetChannel);
        }
    }
    
    // 비동기 파일 처리
    @Async("fileTaskExecutor")
    public CompletableFuture<String> processFileAsync(MultipartFile file) {
        // 비동기 파일 처리 로직
        return CompletableFuture.completedFuture(processedFileUrl);
    }
}
```

#### 4.3 Connection Pool 최적화
```java
@Configuration
public class ConnectionPoolConfig {
    
    @Bean
    public ThreadPoolTaskExecutor chatTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);          // 기본 스레드 수
        executor.setMaxPoolSize(100);          // 최대 스레드 수
        executor.setQueueCapacity(500);        // 큐 용량
        executor.setKeepAliveSeconds(60);      // 유휴 스레드 생존 시간
        executor.setThreadNamePrefix("Chat-"); // 스레드 이름 접두사
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }
}
```

### 5. 모니터링 및 알람 설정

#### 5.1 주요 메트릭 모니터링
```yaml
# Grafana 대시보드 주요 지표
- HTTP 요청 수 (TPS)
- 응답 시간 분포 (P50, P95, P99)
- 에러율
- 데이터베이스 연결 수
- WebSocket 연결 수
- JVM 힙 메모리 사용량
- GC 수행 시간
```

#### 5.2 알람 설정
- 응답 시간 P95 > 500ms 지속 5분
- 에러율 > 5% 지속 2분
- DB 연결 수 > 40개 지속 1분
- WebSocket 연결 수 > 8000개

### 6. 로드 밸런싱 및 스케일링

#### 6.1 Horizontal Scaling
```yaml
# 멀티 인스턴스 배포
- 애플리케이션 서버: 3대
- Redis 클러스터: 3 마스터 + 3 슬레이브
- PostgreSQL: 1 마스터 + 2 읽기 전용 슬레이브
```

#### 6.2 WebSocket Sticky Session
```nginx
# Nginx 설정
upstream websocket_backend {
    ip_hash;  # 세션 고정을 위한 IP 해시
    server localhost:8081;
    server localhost:8082;
    server localhost:8083;
}
```

## 🎯 성능 목표

### 단기 목표 (1개월)
- TPS: 107 → 300 requests/s
- 평균 응답시간: 41ms → 30ms 이하
- 에러율: 14.28% → 1% 이하
- 동시 WebSocket 연결: 1,000개

### 중기 목표 (3개월)
- TPS: 500 requests/s
- P95 응답시간: 100ms 이하
- 동시 WebSocket 연결: 5,000개
- 무중단 배포 환경 구축

### 장기 목표 (6개월)
- TPS: 1,000 requests/s
- P99 응답시간: 200ms 이하
- 동시 WebSocket 연결: 10,000개
- 멀티 리전 배포

## 🔧 즉시 적용 가능한 개선사항

1. **HikariCP 설정 조정** (5분)
2. **읽지 않은 메시지 API 버그 수정** (30분)
3. **채팅 관련 인덱스 추가** (1시간)
4. **Redis 캐시 도입** (1일)
5. **Netty 옵션 튜닝** (반나절)

## 📈 예상 성능 개선 효과

- **데이터베이스 최적화**: 응답시간 30% 개선
- **캐싱 도입**: TPS 50% 향상
- **네트워크 최적화**: WebSocket 처리량 100% 개선
- **인덱스 최적화**: 복잡한 쿼리 80% 성능 향상
