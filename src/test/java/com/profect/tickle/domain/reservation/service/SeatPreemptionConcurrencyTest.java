package com.profect.tickle.domain.reservation.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.profect.tickle.domain.reservation.dto.request.SeatPreemptionRequestDto;
import com.profect.tickle.domain.reservation.dto.response.preemption.SeatPreemptionResponseDto;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ActiveProfiles("test")
@Sql(scripts = "classpath:sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:sql/schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:sql/seatPreemption/seatPreemptionData.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@SpringBootTest
class SeatPreemptionConcurrencyTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private SeatPreemptionService service;

    @Test
    @DisplayName("같은 좌석을 동시에 선점하려는 경우 한 명만 성공해야 한다")
    public void concurrentPreemptionTest() throws Exception {
        //given
        Long performanceId = 1L;
        Long targetSeatId = 1L;

        int requestCount = 10;
        AtomicInteger success = new AtomicInteger();
        AtomicInteger fail = new AtomicInteger();

        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(requestCount);

        ExecutorService executorService = Executors.newFixedThreadPool(requestCount);

        //when
        for (long i = 1; i <= requestCount; i++) {
            long memberId = i;
            executorService.submit(() -> {
                try {
                    startGate.await();
                    SeatPreemptionRequestDto request = SeatPreemptionRequestDto.builder()
                            .seatIds(List.of(targetSeatId))
                            .performanceId(performanceId)
                            .build();

                    SeatPreemptionResponseDto response = service.preemptSeats(
                            request, memberId);

                    if (response.isSuccess()) {
                        success.incrementAndGet();
                    } else {
                        fail.incrementAndGet();
                    }
                } catch (Exception e) {
                    fail.incrementAndGet();
                } finally {
                    doneGate.countDown();
                }
            });
        }
        startGate.countDown();
        doneGate.await();

        //then
        assertThat(success.get()).isEqualTo(1);
        assertThat(fail.get()).isEqualTo(9);

        executorService.shutdown();
    }

    @Test
    @DisplayName("서로 다른 좌석에 대한 동시 선점 요청 - 모두 성공해야 함")
    public void concurrentPreemptionTest2() throws Exception {
        Long performanceId = 1L;

        int requestCount = 10;
        AtomicInteger success = new AtomicInteger();
        AtomicInteger fail = new AtomicInteger();

        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(requestCount);

        ExecutorService executorService = Executors.newFixedThreadPool(requestCount);

        for (long i = 1; i <= requestCount; i++) {
            long memberId = i;
            long seatId = i;

            executorService.submit(() -> {
                try {
                    startGate.await();
                    SeatPreemptionRequestDto request = SeatPreemptionRequestDto.builder()
                            .seatIds(List.of(seatId))
                            .performanceId(performanceId)
                            .build();

                    SeatPreemptionResponseDto response = service.preemptSeats(
                            request, memberId);

                    if (response.isSuccess()) {
                        success.incrementAndGet();
                    } else {
                        fail.incrementAndGet();
                    }
                } catch (Exception e) {
                    fail.incrementAndGet();
                } finally {
                    doneGate.countDown();
                }
            });
        }

        startGate.countDown();
        doneGate.await();

        //then
        assertThat(success.get()).isEqualTo(10);
        assertThat(fail.get()).isEqualTo(0);

        executorService.shutdown();
    }
}
