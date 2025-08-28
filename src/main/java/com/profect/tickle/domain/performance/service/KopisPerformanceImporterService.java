package com.profect.tickle.domain.performance.service;

import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.domain.performance.dto.KopisPerformanceDto;
import com.profect.tickle.domain.performance.entity.Genre;
import com.profect.tickle.domain.performance.entity.Hall;
import com.profect.tickle.domain.performance.entity.HallType;
import com.profect.tickle.domain.performance.entity.Performance;
import com.profect.tickle.domain.performance.parser.KopisXmlParser;
import com.profect.tickle.domain.performance.repository.GenreRepository;
import com.profect.tickle.domain.performance.repository.HallRepository;
import com.profect.tickle.domain.performance.repository.PerformanceRepository;
import com.profect.tickle.domain.reservation.repository.SeatTemplateRepository;
import com.profect.tickle.global.status.Status;
import com.profect.tickle.global.status.repository.StatusRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KopisPerformanceImporterService {

    private final GenreRepository genreRepository;
    private final HallRepository hallRepository;
    private final SeatTemplateRepository seatTemplateRepository;
    private final PerformanceRepository performanceRepository;
    private final MemberRepository memberRepository;
    private final KopisXmlParser kopisXmlParser;
    private final StatusRepository statusRepository;

    @Value("${kopis.service-key}")
    private String serviceKey;

    @Qualifier("kopisRestTemplate")
    private final RestTemplate kopisRestTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * KST 고정(비즈니스 규칙)
     */
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /**
     * LDT(오프셋 없음, KST로 해석) -> Instant(UTC)
     */
    private static Instant kstLdtToInstant(LocalDateTime ldt) {
        return ldt == null ? null : ldt.atZone(KST).toInstant();
    }

    /**
     * 날짜만 들어올 때 특정 시각(KST) 붙여서 Instant로 변환
     */
    private static Instant kstDateAt(LocalDate date, int hour, int minute) {
        return date.atTime(hour, minute).atZone(KST).toInstant();
    }

    /**
     * 상태코드: KST ‘일자’ 기준
     */
    private Status getStatusByDate(Instant performanceDate) {
        LocalDate perf = performanceDate.atZone(KST).toLocalDate();
        LocalDate today = Instant.now().atZone(KST).toLocalDate();
        short code = (short) (perf.isAfter(today) ? 100 : (perf.isEqual(today) ? 101 : 102));
        return statusRepository.findByTypeAndCode("공연", code)
                .orElseThrow(() -> new IllegalStateException("Status not found for code: " + code));
    }

    public void importPerformances() {
        String baseUrl = "http://www.kopis.or.kr/openApi/restful/pblprfr?service=" + serviceKey;

        // 🔥 날짜 범위 수정: 최근 1년치 데이터만 받아오기
        String startDate = "20240101";  // 2024년 1월부터
        String endDate = "20251231";    // 2025년 12월까지

        int page = 1;
        int rows = 100;

        while (true) {
            try {
                String url = baseUrl +
                        "&stdate=" + startDate +
                        "&eddate=" + endDate +
                        "&cpage=" + page +
                        "&rows=" + rows;

                ResponseEntity<String> response = kopisRestTemplate.getForEntity(url, String.class);
                List<KopisPerformanceDto> dtoList = kopisXmlParser.parse(response.getBody());

                System.out.println("📋 KOPIS Page " + page + " 파싱 결과: " + dtoList.size() + "건");

                if (dtoList.isEmpty()) break;

                savePerformances(dtoList);
                page++;

                // API 호출 제한 고려
                Thread.sleep(200);

            } catch (Exception e) {
                System.out.println("❌ Page " + page + " 처리 실패: " + e.getMessage());
                page++;

                // 연속 실패 시 중단
                if (page > 1000) break;
            }
        }

        System.out.println("✅ KOPIS 데이터 import 완료!");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void savePerformances(List<KopisPerformanceDto> dtoList) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");

        for (KopisPerformanceDto dto : dtoList) {
            Performance performance = null;

            try {
                // KOPIS 종료일(날짜만) 파싱
                LocalDate endDate = LocalDate.parse(dto.getPrfpdto(), formatter);

                // 기준 공연 시각: 종료일 20:00 (KST)
                LocalDateTime perfLdt = endDate.atTime(18, 0);

                // KST ‘일자’ 기준으로 날짜 연산(LDT에서 처리) 후 Instant로 변환
                Instant performanceDate = kstLdtToInstant(perfLdt);
                Instant startDate = kstLdtToInstant(perfLdt.minusDays(21));
                Instant endDateTime = kstDateAt(perfLdt.toLocalDate().minusDays(7), 23, 59).plusSeconds(59); // ✅ 23:59:59 (KST)
                Instant createdAt = kstLdtToInstant(perfLdt.minusDays(28));

                // 중복 검사(엔티티가 Instant이므로 리포지토리 파라미터도 Instant)
                if (performanceRepository.existsByTitleAndDate(dto.getPrfnm(), performanceDate)) {
                    System.out.println("🚫 중복 공연: " + dto.getPrfnm());
                    continue;
                }

                // 장르
                Genre genre = genreRepository.findByTitle(dto.getGenrenm())
                        .orElseGet(() -> genreRepository.save(Genre.builder().title(dto.getGenrenm()).build()));

                // 홀(무작위 타입)
                HallType hallType = Math.random() < 0.5 ? HallType.A : HallType.B;
                String hallAddress = dto.getFcltynm().trim();

                Hall hall = hallRepository.findByTypeAndAddress(hallType, hallAddress)
                        .orElseGet(() -> hallRepository.save(
                                Hall.builder().type(hallType).address(hallAddress).build()
                        ));

                // 가격(좌석템플릿에서 최소/최대)
                Integer minPrice = seatTemplateRepository.findMinPriceByHallType(hallType); // enum 그대로
                Integer maxPrice = seatTemplateRepository.findMaxPriceByHallType(hallType);
                if (minPrice == null || maxPrice == null) {
                    System.out.println("   ⚠️ 가격 정보 없음 → 저장 스킵됨");
                    continue;
                }
                String price = minPrice + "~" + maxPrice;

                Status status = getStatusByDate(performanceDate);
                Member member = memberRepository.getReferenceById(1L);

                performance = Performance.builder()
                        .title(dto.getPrfnm())
                        .img(dto.getPoster())
                        .price(price)
                        .runtime((short) 120)
                        .isEvent(false)
                        .lookCount((short) 0)
                        .startDate(startDate)        // Instant(UTC)
                        .endDate(endDateTime)        // Instant(UTC)
                        .date(performanceDate)       // Instant(UTC)
                        .createdAt(createdAt)        // Instant(UTC)
                        .updatedAt(createdAt)        // Instant(UTC)
                        .deletedAt(null)
                        .member(member)
                        .genre(genre)
                        .hall(hall)
                        .status(status)
                        .build();

                performanceRepository.save(performance);
                System.out.println("✅ 저장 성공: " + dto.getPrfnm());

            } catch (Exception e) {
                if (performance != null) entityManager.detach(performance);
                System.out.println("❌ 저장 실패: " + dto.getPrfnm());
                e.printStackTrace();
            }
        }
    }
}
