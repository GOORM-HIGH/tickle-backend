# 🎟️ TicketCloud

> **대규모 트래픽에도 안정적인 티켓 예매 서비스**  

Tickle은 공연·이벤트를 쉽고 빠르게 예매할 수 있는 티켓 예매 웹 서비스입니다.  
사용자는 원하는 공연을 검색하고 실시간으로 남은 좌석과 가격을 확인하며, 간편한 절차를 통해 예매를 완료할 수 있습니다.  

### 기술 스택
**Frontend**  
![React](https://img.shields.io/badge/React-61DAFB?logo=react&logoColor=white) ![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?logo=typescript&logoColor=white) ![Vite](https://img.shields.io/badge/Vite-646CFF?logo=vite&logoColor=white)

**Backend**  
![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk) ![Spring Boot](https://img.shields.io/badge/SpringBoot-6DB33F?logo=springboot&logoColor=white) ![Spring Security](https://img.shields.io/badge/Spring%20Security-6DB33F?logo=springsecurity&logoColor=white) ![JPA](https://img.shields.io/badge/JPA-59666C?logo=hibernate&logoColor=white) ![MyBatis](https://img.shields.io/badge/MyBatis-000000?logoColor=white)

**Infra & DevOps**  
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?logo=postgresql&logoColor=white) ![Redis](https://img.shields.io/badge/Redis-DC382D?logo=redis&logoColor=white) ![Docker](https://img.shields.io/badge/Docker-2496ED?logo=docker&logoColor=white) ![AWS EC2](https://img.shields.io/badge/AWS%20EC2-FF9900?logo=amazonec2&logoColor=white) ![AWS S3](https://img.shields.io/badge/AWS%20S3-569A31?logo=amazons3&logoColor=white) ![Nginx](https://img.shields.io/badge/Nginx-009639?logo=nginx&logoColor=white) ![Github Actions](https://img.shields.io/badge/GitHub%20Actions-2088FF?logo=githubactions&logoColor=white) ![Prometheus](https://img.shields.io/badge/Prometheus-E6522C?logo=prometheus&logoColor=white) ![Grafana](https://img.shields.io/badge/Grafana-F46800?logo=grafana&logoColor=white)
---

## 👥 팀 소개
| 이름   | 역할                | 담당 업무 | GitHub |
|--------|---------------------|--------|--------|
| 서 해윤  | Backend / 팀장 & 풀스택 | - 이벤트 기능 구현 </br> - 포인트 결제 기능 구현 |[GitHub](https://github.com/Haennni) |
| 나 용성  | Backend / 팀원 & 풀스택 | - 채팅 기능 구현 |[GitHub](https://github.com/BE-Member) |
| 봉 선호  | Backend / 팀원 & 풀스택 | - 정산 기능 구현 |[GitHub](https://github.com/BE-Member) |
| 심 석현  | Backend / 팀원 & 풀스택 | - 예매 기능 구현 |[GitHub](https://github.com/BE-Member) |
| 임 광택  | Backend / 팀원 & 풀스택 | - 로그인 및 회원가입 구현 </br> - 알림 기능 구현 |[GitHub](https://github.com/BE-Member) |
| 홍 주이  | Backend / 팀원 & 풀스택 | - 공연 조회 기능 구현 |[GitHub](https://github.com/BE-Member) |

---

## ✨ 주요 기능
- ✋ **회원가입**: 사용자의 개인정보를 입력하려 티켓클라우드에 새로운 회원으로 등록 (이메일당 1회)
- 👋 **로그인**: 이메일과 비밀번호를 입력하여 인증. 인가를 위해 JWT 토큰 발급
- 🎭 **조회수별 장르/통합 Top10 조회**: 조회수가 높은 순서대로 조회.
- 👀 **공연정보 상세 조회**: 제목,장소,일시 등등 공연 관련된 자세한 정보 조회.
- 🔗 **상세 공연 관련 공연 Top4 조회**: 상세 조회된 공연의 장르 중 조회수가 높은 순서대로 조회.
- 📅 **예매 예정 공연 Top4 조회**: 공연 예정이 가까운 순서대로 조회.
- 🔍 **공연 검색 조회**: 공연 제목을 키워드로 공연을 검색.
- 🔖 **공연 스크랩**: 관심있는 공연은 스크랩 할 수 있고, 스크랩한 목록 조회 가능.
- 💡 공연 생성, 수정, 삭제: 주최자는 원하는 공연 생성, 수정, 삭제 가능.
- 🎫 **티켓 이벤트**: 사용자가 보유 포인트를 차감하여 이벤트에 응모. 목표 금액 달성 시 당첨자를 선정하고 **예매권을 발급**.
- 🎟 **쿠폰 이벤트**: 선착순 쿠폰을 발급. (1인 1매 제한)
- 💸 **포인트 결제**: Bootpay 연동을 통한 포인트 충전 및 결제 기능. 충전/차감 이력 관리 및 마이페이지에서 내역 조회 가능.
- 🔔 **실시간 알림**: 아래 이벤트가 발생하면, 실시간 알림을 사용자게에 전송.
- 🔕 **알림 수신**: 사용자가 알림 목록을 조회. 알림 상세 조회에 성공하면 알림 수신표시.


## 도메인별 화면 구성
| 도메인 | 페이지 | 화면 구성 |
|---------|---|------|
| 공 연 | 메인 페이지 | <img src="https://github.com/user-attachments/assets/f2a31836-22a4-4c21-9434-4d97093a1db8" width="400"/>
| 공 연 | 장르별 페이지 | <img src="https://github.com/user-attachments/assets/21e66eaf-c9f5-48e1-9404-e1bdf4de7c5f" width="400"/>
| 공 연 | 공연 상세 조회 페이지 | <img src="https://github.com/user-attachments/assets/e7fda9c5-1ae1-4bfb-922f-89c8471246ec" width="400"/>
| 공 연 | 공연 검색 페이지 | <img src="https://github.com/user-attachments/assets/5d431082-65e2-42be-a349-2669ba05fd66" width="400"/>
| 사용자 | 개인 회원가입 페이지  | <img src="https://github.com/user-attachments/assets/6a50c6b3-22aa-4ac7-8190-79497c8e8215" width="400"/>
| 사용자 | 사업자 회원가입 페이지  | <img src="https://github.com/user-attachments/assets/ac1b73d9-d29c-4a9c-80c4-bba6946fbef8" width="400"/>
| 사용자 | 로그인 페이지  | <img src="https://github.com/user-attachments/assets/cd3f200d-a5b2-4fca-bc6a-40d338ef25ae" width="400"/>
| 이벤트 | 쿠폰 이벤트 페이지  | <img src="https://github.com/user-attachments/assets/a8914294-bea4-44f7-a963-952c1126014d" width="400"/>
| 이벤트 | 티켓 이벤트 페이지 </br> 및 </br> 이벤트 조회 | <img src="https://github.com/user-attachments/assets/7af1f75d-74b3-435e-ada5-24c3f67416ab" width="400"/>
| 이벤트 | 티켓 이벤트 응모 기능 </br> (미당첨, 당첨) | <img src="https://github.com/user-attachments/assets/58040467-af34-426d-a7e9-70da9537b524"  width="400"/>
| 마이페이지 | 마이페이지 | <img src="https://github.com/user-attachments/assets/cef61673-b268-44d4-9a78-e10a8a675876" width="400"/>
| 마이페이지 | 예매 / 취소 내역 페이지 | <img src="https://github.com/user-attachments/assets/561e9099-aeb1-45b7-a9a8-5f52482fad00" width="400"/>
| 마이페이지 | 스크랩한 공연 페이지 | <img src="https://github.com/user-attachments/assets/5d9096f1-fc86-4138-b46e-b5c2f7704161" width="400"/>
| 포인트 | 포인트 충전 | <img src="https://github.com/user-attachments/assets/0ba966a7-ae32-4309-b8ba-751b134c3986" width="400"/>
| 알 림 | 알림 팝업창 | <img src="https://github.com/user-attachments/assets/9c1c6511-f23e-4f13-a0e4-c93007d624ae" width="400"/>

---

## 🧪 단위 테스트 및 통합 테스트
- JUnit 기반 단위 테스트 작성 (테스트 커버리지 평균 **60% 이상**)  
- 통합 테스트 환경 구성: **H2 DB** 기반 테스트, Spring Security/JWT 인증 검증 포함

**이벤트 테스트**
</br>
<img width="451" height="41" alt="스크린샷 2025-09-30 오후 3 28 33" src="https://github.com/user-attachments/assets/eb24f30e-856d-47d9-a394-7bdd66e88f27"  width="400"/></br>
<img width="487" height="47" alt="스크린샷 2025-09-30 오후 3 28 49" src="https://github.com/user-attachments/assets/27c6851b-9059-4049-8828-9e9be31a834e"  width="400"/></br>
<img width="479" height="36" alt="스크린샷 2025-09-30 오후 3 29 05" src="https://github.com/user-attachments/assets/be6b88b0-843a-4a3c-bba0-887cb199bd97"  width="400"/></br>


---

# 🚀 기술적 도전 과제 및 개선 사항
본 프로젝트는 다음과 같은 주요 기술적 문제들을 해결하고 성능을 개선하는 데 집중했습니다. </br> 각 항목에 대한 자세한 내용은 링크된 Wiki 문서를 참고해주세요.

---
## 🎭 공연
**담당자 홍주이**
> 공연 기능을 구현하며 3000K 이상 대규모 데이터 처리를 안정적으로 처리하는 것을 중점적으로 도전했습니다. 사용자의 이탈률을 줄이기 위해 공연 검색 조회 속도를 최적화하고자 했습니다.
  
## 1. **검색 기능 성능 개선**
### **문제**
공연 데이터를 6K에서 3000K 까지 늘리다보니, 기존 검색 조회 속도가 20배가 느려지는 문제가 발생. 간단한 LIKE 문으로 키워드 포함 구현 방식과 인덱스를 적용 하지 않은 것을 문제로 생각함.
</br>
</br>
### **해결과정**
- **(1) Cursor 페이징 조회 적용**</br>
  - 문제: 기존 Offset 기반 페이징 조회는 페이지가 뒤로 갈수록 DB가 앞 행을 읽고 버리는 과정 때문에 대규모 데이터 처리에서 성능 저하가 발생.
  - 해결: Cursor 페이징을 적용하여 마지막 레코드 위치를 기준으로 다음 데이터를 바로 탐색하도록 변경, 불필요한 행 스캔을 제거.
  - 결과: 평균 응답 속도 40초~1분 → 3~4초로 개선 (성능 향상: 약 10~20배)
  - 효과: 대규모 데이터 처리 환경에서 안정적인 페이지 조회 가능 및 사용자 경험 개선 (응답 지연 최소화).
  
- **(2) Full Text 적용한 검색**</br>
  - 문제: RDB 기반 Full Text Search에서도 데이터 증가와 동시 요청이 많아질 경우 성능 한계가 예상.
  - 해결: PostgreSQL Full Text Search를 적용하여 tsvector 기반 인덱스로 공연 제목을 검색하도록 개선.
  - 결과: 검색 속도 3~4s→ 1s 이내로 대폭 개선.2글자 이상 검색에도 안정적인 결과 제공

- **(3) Elasticsearch 를 적용한 검색**</br>
  - 문제: 기존 LIKE, ILIKE, Trigram 기반 검색으로는 3000K 이상의 공연 데이터를 대상으로 한 검색 성능 한계.
  - 해결: Elasticsearch를 도입하여 단일 노드, 단일 샤드 환경에서 공연 데이터를 색인하고, 커서 기반 페이지네이션을 적용.
  - 결과: 검색 응답 속도 안정화 (대규모 동시 요청 0.7K 건 처리 가능), 복잡한 키워드 검색, 정렬, 필터링 기능 확장 용이


### **성과**
- 공연 검색 1초 이내 응답 속도.
- 3000K 의 대규모 데이터 안정적인 처리 가능.
</br>

### **개선 과정별 성능 비교**
<img width="1536" height="1024" alt="image" src="https://github.com/user-attachments/assets/2f492d72-e5df-45c8-923d-abd179fcf8b8" />

</br> 

## 2. **공연 관련 조회 속도 성능 개선**
### **문제**
공연 데이터가 3천만 건으로 증가하면서 실시간 인기 공연 조회, 예정 공연 등 대부분의 공연 조회에서 응답 속도가 크게 저하됨. (NGrinder로 300 vuser를 5분간 테스트 → 0.6 TPS)
</br>
</br>
### **해결과정**
- **(1) 쿼리문 최적화**</br>
  - 문제: 기존 쿼리는 status 테이블과 JOIN을 수행하여 상태 정보를 조회.
  - 해결: 이미 status_id를 FK로 가지고 있기 때문에 불필요한 JOIN을 제거
  - 결과: 기존 쿼리 4,798ms → 약 3,222ms로 단축. </br> 조회 성능 향상, 응답 지연 감소했지만 여전히 느림.
  
- **(2) 인덱스 적용**</br>
  - 개선내용: 장르별 공연 목록, 카운트, 관련 공연 조회를 위해 인덱스를 생성.
  - 인기 공연 조회용 컬럼(performance_look_count, performance_start_date)에도 B-Tree 인덱스를 적용.
  - 조회 조건에서 사용되는 상태값(status_id)과 삭제 여부(performance_deleted_at)를 인덱스 필터 조건으로 활용.

### **성과**
- 300 vuser 동시 요청 시 약 319 TPS로 안정화.
- 대규모 데이터(3000K 건)에서도 빠른 조회 가능.
- 
</br>

### **개선 과정별 성능 비교**
<img width="1454" height="1080" alt="image" src="https://github.com/user-attachments/assets/e205bb6c-5c8b-48b5-93df-eabef0560388" />

</br>

## 🚀 트러블슈팅
-
-

---



## 🎫 이벤트 
**담당자 서해윤**
> 이벤트 기능을 구현하며 이벤트 기능을 구현하며 대규모 트래픽 상황에서도 데이터 정합성을 유지하는 것을 중점적으로 도전했습니다. 또한 사용자 입장에서는 대기 없는 빠른 응답과 공정한 참여 경험이 중요하다고 생각해 이를 개선하고자 했습니다.
  
## 1. **티켓 이벤트 데이터 정합성 개선**
### **문제**
동시에 다수의 사용자가 티켓 이벤트에 응모할 때, 목표 금액이 정확하게 누적되지 않는 문제가 발생함. 이는 선착순 이벤트 특성상 동시에 많은 요청이 들어와 DB 갱신 시 충돌이 빈번하게 발생했기 때문임.
</br>
</br>
### **해결과정**
- [[Wiki] 락 적용 단계 과정: 동시성을 잠금으로 안전하게 처리해보자](https://dev-haen.tistory.com/112)
- **(1) 락 방식 비교 실험 (Optimistic Lock vs Pessimistic Lock)**</br>
  - 낙관적 락을 적용했으나, 동시에 요청이 몰리는 상황에서 예외 및 재시도가 급격히 증가하여 성능 저하 발생. 
  - 평균 응답 시간이 길어지고, 사용자 경험 측면에서도 불리한 결과 확인.
- **(2) 비관적 락 적용** </br>
  - 임계 구간에 대해 선점 잠금을 사용하여 충돌 자체를 사전에 차단.
  - 동시성 충돌이 제거되어 목표 금액이 안정적으로 누적되고, 데이터 정합성 보장.
- [[Wiki] 비관적 락을 유지한 채 성능 개선을 해보자](https://dev-haen.tistory.com/116)

  
### **성과**
- 데이터 불일치 문제를 근본적으로 해소하고, 사용자에게 공정하고 신뢰할 수 있는 이벤트 경험 제공.
- 낙관적 락 대비 비관적 락 적용 시 평균 응답 속도 4.34s → 1.43s로 개선.
</br>

## 2. **티켓 이벤트 성능 개선**
### **문제**
비관적 락 적용으로 데이터 정합성은 확보했으나, 락 경합·불필요한 DB 부하로 인해 여전히 응답 지연과 낮은 처리량 문제가 발생함. 특히 선착순 이벤트와 같이 짧은 시간에 수천 건의 요청이 몰릴 경우, 기존 구조만으로는 확장성 확보에 한계가 있었음.
</br>

### **해결과정**
- **(1) 락 범위 최소화**</br>
  - 문제: 포인트 차감, 목표 금액 누적, 내역 저장, 좌석 배정이 모두 한 트랜잭션에 묶여 있어 불필요하게 락 유지 시간이 길어짐.
  - 해결: **핵심 로직(포인트 차감·목표 금액 누적)** 만 트랜잭션에 포함시키고, 내역 저장·좌석 배정은 별도 후처리로 분리하여 락 경합 최소화.
- **(2) SQL 최적화** </br>
  - 문제: 좌석 정보가 항상 EAGER로 조회되어 필요하지 않은 경우에도 불필요한 쿼리가 실행되고, UPDATE 시 조건문 없이 전체를 갱신해 DB 부하 증가.
  - 해결: 좌석 매핑을 LAZY 로딩으로 전환하고, 조건부 원자적 업데이트를 적용해 최소한의 쿼리만 실행되도록 최적화.
- **(3) Pub/Sub 구조 도입** </br>
  - 문제: 트래픽 급증 시 락 기반 구조만으로는 확장성에 한계가 있었고, 요청 폭주 상황에서 처리 병목이 발생.
  - 해결: Redis Pub/Sub을 도입해 요청을 메시지 단위로 발행·구독 처리, 락 없이 병렬 분산 처리 가능하도록 구조 개선. (단, 메시지 순서 보장이 어려워 일부 데이터 일관성 문제가 존재함)
- **(4) 메시지 큐 구조 도입 (Redis Stream)** </br>
  - 문제: Pub/Sub은 메시지 순서 보장이 안되기때문에 선착순 이벤트와 맞지 않다고 생각했고, Ack(확인 응답)가 불가능해 데이터 정합성 깨질 위험 존재.
  - 해결: Redis Stream 기반 큐를 적용해 메시지를 안정적으로 저장·분배하고, Ack 기반 재처리 메커니즘을 도입해 메시지 손실 없이 순차성을 보장.
  
### **성과**
- 평균 응답 속도 **38s** → **5ms** , 최대 응답 속도 **75s** → **86ms** 로 단축.
- 처리량 **TPS 1.3** → **2,269** 로 대폭 향상, **동시 10,000명 이상** 요청 환경에서도 안정적으로 운영 가능.
- 사용자 입장에서 대기 없는 빠른 응답과 공정한 이벤트 참여 경험을 제공할 수 있었음.
</br>
 
## 🚀 트러블슈팅
- [[Wiki] 데이터 정합성 이슈: 커밋 시점으로 인해 발생한 동시성 문제](https://dev-haen.tistory.com/113)
- [[Wiki] 커넥션 풀 고갈 이슈: HikariPool-1 - Connection is not available, request timed out after…](https://dev-haen.tistory.com/115)
- 톰캣 스레드 위키 추가 예정

---

## 🔔 알림
**담당자 임광택**
> 피크 트래픽 20K 상황에서 실시간 통신연결을 2초이내에 모두 성공하고 브로드캐스트 알림 발생 시, 5초이내에 모두 발송하는 것을 목표로 성능개선을 진행했습니다.
  
## 1. **SSE 연결 성능 개선**
### **문제**
20K SSE 연결 요청이 수신되었지만, 8192개의 연결요청만이 성공하는 문제가 발생함. 네트워크 수신에 제한이 있기 때문임.
</br>
</br>
### **해결과정**
- **(1) Tomcat 웹서버 튜닝**</br>
  - 문제: 8192개의 연결요청만이 성공하는 문제가 발생.
  - 해결: Tomcat 웹서버의 네트워크 연결 값을 25K으로 증가.
  - 결과: 여전히 8192개의 요청만이 수신됨.
  
- **(2) OS 커널 튜닝**</br>
  - 다음 값들을 증가시킴.
    - 네트워크 큐(Accept Queue, somaxconn)들의 크기
    - 파일디스크립터의 크기
    - JVM에게 할당하는 메모리의 크기
    - 성능테스트를 진행하는 PC의 임시포트 크기 
  - 결과:20K 연결에 성공.

### **성과**
- 20K의 SSE 연결을 요청했을 때, 평균 응답속도 1.8초로 모든 연결에 성공.
</br>

### **개선 과정별 성능 비교**
<img width="1536" height="1024" alt="image" src="https://github.com/user-attachments/assets/2f492d72-e5df-45c8-923d-abd179fcf8b8" />

</br> 

## 2. **20K 브로드캐스트 기능 개선**
### **문제**
20K의 사용자를 SSE 연결을 후, 브로드캐스트 이벤트(제휴업체에서 공연을 생성)를 발생시킴. 약30초의 응답시간이 발생했고, 일부 유저에게만 실시간 알림이 도착함. 병목지점을 분석하니 DB에 알림을 저장하는 과정과 알림을 송신하는 과정에서 병목이 발생.
</br>
</br>

### **해결과정**
- **(1) DB 저장 - 청크방식으로 알림을 저장.**</br>
  - 해결: 청크방식으로 알림을 저장.
  - 결과: 5000개의 데이터로 나눠 DB에 저장. 약 8초 소요.
  
- **(2) DB 저장 - Postgresql의 COPY 삽입**</br>
  - 해결: Postgresql의 COPY 삽입
  - 결과: 저장해야하는 약 4M개의 데이터를 CSV 파일로 변환하여 한번에 저장. 약 1.2초 소요
  
- **(3) 실시간 알림 송신 - 스레드 개수 증가**</br>
  - 스레드풀의 개수를 650개로 증가. 메모리 부족문제 발생.
  - 이유: 기존의 스레드풀을 100%사용하고 있어, 스레드 개수를 증가시켜봄.
  
- **(4) 실시간 알림 송신 - Virtual Thread로 전환**</br>
  - 사용하는 스레드를 Virtual Thread로 전환. 모든 알림 전송에 성공. 약 0.2초 소요
  - 이유: 메모리를 적게 사용하는 스레드를 찾아본 결과. Virtual Thread가 적합하다고 판단.

### **성과**
- 20K명의 사용자가 접속한 상황에서 브로드캐스트 메시지를 발송하는데 성공.

</br>

### **개선 과정별 성능 비교**
<img width="2048" height="1018" alt="image" src="https://github.com/user-attachments/assets/6abb300a-8ad1-4d9e-b809-6b8856610d91" />
- 좌: 2OK SSE 연결 성공률 (0.07 → 100)
- 우: 브로드캐스트 알림 발송 시간 (31s → 3.4s)

</br>

## 🚀 트러블슈팅
-
-

---

## 🎥 시연 영상
👉 [시연 영상 보러가기](https://youtu.be/your-demo-video-link)  

---

## 📂 레포지토리
🔗 [GitHub Repository](https://github.com/GOORM-HIGH)  
