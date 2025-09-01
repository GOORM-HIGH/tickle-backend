package com.profect.tickle.domain.event.mapper;

import com.profect.tickle.domain.event.dto.response.TicketListResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

@ActiveProfiles("test")
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY) // <- 반드시 추가
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:tickle;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@Sql(scripts = "classpath:sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:sql/schema.sql",  executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:sql/data.sql",    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class EventMapperTest {

    @Autowired
    private EventMapper eventMapper;


    @Test
    @DisplayName("티켓 이벤트 리스트 중, 예매 종료일이 지나지않은 티켓 이벤트만 최신순으로 조회한다.")
    void findTicketEventList_filtersOngoing_andOrders_andPaginates() {
        // given
        int size = 5;
        int offset = 0;

        // when
        List<TicketListResponseDto> result = eventMapper.findTicketEventList(size, offset);

        // then
        assertThat(result).hasSize(5)
                .extracting("eventId", "name", "perPrice", "img", "statusId")
                .containsExactlyInAnyOrder(
                        tuple(6L, "레미제라블 티켓 이벤트", (short) 1, "lesmis.jpg", 5L),
                        tuple(7L, "햄릿 티켓 이벤트",(short) 1600, "hamlet.jpg", 5L),
                        tuple (8L, "캣츠 티켓 이벤트",(short) 2500, "cats.jpg", 5L),
                        tuple(9L, "오페라의 유령 티켓 이벤트",(short) 2500, "phantom.jpg", 5L),
                        tuple (10L, "라이온킹 티켓 이벤트",(short) 1200, "lionking.jpg", 5L)
                );
    }


    @Test
    @DisplayName("티켓 이벤트 리스트 중, 예매 종료일이 지난 이벤트는 조회되지 않는다.")
    void findTicketEventList_excludesExpired() {
        // given
        int size = 30;
        int offset = 0;

        // when
        List<TicketListResponseDto> result = eventMapper.findTicketEventList(size, offset);

        // then
        assertThat(result).extracting(TicketListResponseDto::eventId)
                .doesNotContain(18L, 19L);

        Instant now = Instant.now();
        assertThat(result).allSatisfy(dto ->
                assertThat(dto.endDate()).isAfterOrEqualTo(now)
        );
    }
}