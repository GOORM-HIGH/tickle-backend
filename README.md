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
- 🎫 **티켓 이벤트**: 사용자가 보유 포인트를 차감하여 이벤트에 응모. 목표 금액 달성 시 당첨자를 선정하고 **예매권을 발급**.
- 🎟 **쿠폰 이벤트**: 선착순 쿠폰을 발급. (1인 1매 제한)
- 💸 **포인트 결제**: Bootpay 연동을 통한 포인트 충전 및 결제 기능. 충전/차감 이력 관리 및 마이페이지에서 내역 조회 가능.

---

## 도메인별 화면 구성
| 도메인 | 페이지 | 화면 구성 |
|---------|---|------|
| 공연 | 메인 페이지 | 공연·이벤트 목록 / 배너 |
| 이벤트 | 쿠폰 이벤트 페이지  | <img src="https://github.com/user-attachments/assets/a8914294-bea4-44f7-a963-952c1126014d" width="400"/>
| 이벤트 | 티켓 이벤트 페이지 </br> 및 </br> 이벤트 조회 | <img src="https://github.com/user-attachments/assets/7af1f75d-74b3-435e-ada5-24c3f67416ab" width="400"/>
| 이벤트 | 티켓 이벤트 응모 기능 </br> (미당첨, 당첨) | <img src="https://github.com/user-attachments/assets/58040467-af34-426d-a7e9-70da9537b524"  width="400"/>
| 마이페이지 | 마이페이지 | 쿠폰/포인트 내역 조회 |
| 어드민 | 관리자 페이지 | 이벤트 생성/쿠폰 관리 |

🔗 [API 문서 (Swagger UI)](http://{server-url}/swagger-ui.html)  

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

## 🎥 시연 영상
👉 [시연 영상 보러가기](https://youtu.be/your-demo-video-link)  

---

## 📂 레포지토리
🔗 [GitHub Repository](https://github.com/GOORM-HIGH)  
