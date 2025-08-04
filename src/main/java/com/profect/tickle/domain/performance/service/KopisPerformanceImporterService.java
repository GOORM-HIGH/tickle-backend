package com.profect.tickle.domain.performance.service;

import com.profect.tickle.domain.performance.entity.*;
import com.profect.tickle.domain.performance.repository.*;
import com.profect.tickle.domain.reservation.repository.SeatTemplateRepository;
import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.domain.performance.dto.KopisPerformanceDto;
import com.profect.tickle.domain.performance.parser.KopisXmlParser;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    public void importPerformances() {
        String baseUrl = "http://www.kopis.or.kr/openApi/restful/pblprfr?service=" + serviceKey;
        String startDate = "20000101";
        String endDate = "20250803";
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
                System.out.println("KOPIS 파싱 결과 개수: " + dtoList.size());
                if (dtoList.isEmpty()) break;

                savePerformances(dtoList);
                page++;
                Thread.sleep(200);

            } catch (Exception e) {
                System.out.println("❌ Page " + page + " 처리 실패");
                e.printStackTrace();
                page++;
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void savePerformances(List<KopisPerformanceDto> dtoList) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");

        for (KopisPerformanceDto dto : dtoList) {
            Performance performance = null;

            try {
                LocalDate endDate = LocalDate.parse(dto.getPrfpdto(), formatter);
                LocalDateTime performanceDate = endDate.atTime(20, 0);
                LocalDateTime startDate = performanceDate.minusDays(21);
                LocalDateTime endDateTime = performanceDate.minusDays(7);
                LocalDateTime createdAt = startDate.minusDays(7);

                if (performanceRepository.existsByTitleAndDate(dto.getPrfnm(), performanceDate)) {
                    System.out.println("🚫 중복 공연: " + dto.getPrfnm());
                    continue;
                }

                Genre genre = genreRepository.findByTitle(dto.getGenrenm())
                        .orElseGet(() -> genreRepository.save(Genre.builder().title(dto.getGenrenm()).build()));

                HallType hallType = Math.random() < 0.5 ? HallType.A : HallType.B;
                String hallAddress = dto.getFcltynm().trim();

                Hall hall = hallRepository.findByTypeAndAddress(hallType, hallAddress)
                        .orElseGet(() -> hallRepository.save(
                                Hall.builder()
                                        .type(hallType)
                                        .address(hallAddress)
                                        .build()
                        ));

                Integer minPrice = seatTemplateRepository.findMinPriceByHallType(hallType.name());
                Integer maxPrice = seatTemplateRepository.findMaxPriceByHallType(hallType.name());

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
                        .startDate(startDate)
                        .endDate(endDateTime)
                        .date(performanceDate)
                        .createdAt(createdAt)
                        .updatedAt(createdAt)
                        .deletedAt(null)
                        .member(member)
                        .genre(genre)
                        .hall(hall)
                        .status(status)
                        .build();

                performanceRepository.save(performance);
                System.out.println("✅ 저장 성공: " + dto.getPrfnm());

            } catch (Exception e) {
                if (performance != null) {
                    entityManager.detach(performance);
                }
                System.out.println("❌ 저장 실패: " + dto.getPrfnm());
                e.printStackTrace();
            }
        }
    }

    private Status getStatusByDate(LocalDateTime performanceDate) {
        LocalDateTime now = LocalDateTime.now();
        short code = (performanceDate.isAfter(now)) ? (short) 100
                : (performanceDate.toLocalDate().isEqual(now.toLocalDate())) ? (short) 101 : (short) 102;

        return statusRepository.findByTypeAndCode("공연", code)
                .orElseThrow(() -> new IllegalStateException("Status not found for code: " + code));
    }
}