package com.profect.tickle.domain.event.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.profect.tickle.domain.chat.config.ChatJwtAuthenticationInterceptor;
import com.profect.tickle.domain.event.dto.request.CouponCreateRequestDto;
import com.profect.tickle.domain.event.dto.response.*;
import com.profect.tickle.domain.event.entity.EventType;
import com.profect.tickle.domain.event.service.CouponService;
import com.profect.tickle.domain.event.service.EventService;
import com.profect.tickle.domain.reservation.entity.SeatGrade;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.exception.GlobalExceptionHandler;
import com.profect.tickle.global.paging.PagingResponse;
import com.profect.tickle.global.response.ResultCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EventController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisabledInAotMode
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    EventService eventService;

    @MockBean
    CouponService couponService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean com.profect.tickle.global.security.util.JwtUtil jwtUtil;

    @MockBean
    private ChatJwtAuthenticationInterceptor chatJwtAuthenticationInterceptor;


    @Nested
    @DisplayName("쿠폰 이벤트 생성(ADMIN)")
    class CreateCouponEvent {

        @Test
        @DisplayName("관리자 권한일 시 쿠폰 이벤트를 생성할 수 있다.")
        @WithMockUser(username = "admin@test.com", authorities = {"ADMIN"})
        void createCoupon_admin_ok() throws Exception {
            CouponCreateRequestDto req = new CouponCreateRequestDto("여름10", "여름 10%", (short) 100, (short) 10, Instant.now().plus(Duration.ofDays(5)));
            CouponResponseDto resp = new CouponResponseDto(1L, "여름10", (short) 20, Instant.now());

            given(eventService.createCouponEvent(any(CouponCreateRequestDto.class)))
                    .willReturn(resp);

            mockMvc.perform(post("/api/v1/event/coupon")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.couponId").value(1L))
                    .andExpect(jsonPath("$.data.couponName").value("여름10"));
        }

/*       @Test
        @DisplayName("권한 없으면 403")
        @WithMockUser(username = "user@test.com", authorities = {"MEMBER"})
        void createCoupon_forbidden() throws Exception {
            CouponCreateRequestDto req = new CouponCreateRequestDto("여름10", "여름 10%", (short)100, (short)10, Instant.now().plus(Duration.ofDays(5)));

            mockMvc.perform(post("/api/v1/event/coupon")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }*/
    }


    @Nested
    @DisplayName("사용자는 보유 포인트로 티켓 이벤트에 응모할 수 있다.")
    class ApplyTicketFail {

        @Test
        @DisplayName("티켓 이벤트 응모에 성공한다.")
        void applyTicketEvent_success() throws Exception {
            var dto = new TicketApplyResponseDto(6L, 1L, false, "아쉽네요. 다음 기회에...");
            when(eventService.applyTicketEvent(6L)).thenReturn(dto);

            mockMvc.perform(post("/api/v1/event/ticket/{eventId}", 6L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(ResultCode.EVENT_APPLY_SUCCESS.getMessage()))
                    .andExpect(jsonPath("$.data.eventId").value(6))
                    .andExpect(jsonPath("$.data.memberId").value(1))
                    .andExpect(jsonPath("$.data.isWinner").value(false))
                    .andExpect(jsonPath("$.data.message").value("아쉽네요. 다음 기회에..."))
            ;
        }

        @Test
        @DisplayName("포인트가 부족하면 이벤트에 응모할 수 없다")
        void apply_pointNotEnough_409() throws Exception {
            doThrow(new BusinessException(ErrorCode.INSUFFICIENT_POINT))
                    .when(eventService).applyTicketEvent(anyLong());

            mockMvc.perform(post("/api/v1/event/ticket/{eventId}", 6L))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409));
        }

        @Test
        @DisplayName("이벤트가 없으면 이벤트를 조회할 수 없다.")
        void apply_eventNotFound_404() throws Exception {
            doThrow(new BusinessException(ErrorCode.EVENT_NOT_FOUND))
                    .when(eventService).applyTicketEvent(anyLong());

            mockMvc.perform(post("/api/v1/event/ticket/{eventId}", 9999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    @Nested
    @DisplayName("사용자는 쿠폰 이벤트에 참여해 쿠폰을 발급받을 수 있다.")
    class IssueCoupon {
        @Test
        @DisplayName("쿠폰을 발급받을 수 있다.")
        void issueCoupon_success() throws Exception {
            doNothing().when(eventService).issueCoupon(100L);

            mockMvc.perform(post("/api/v1/event/coupon/{eventId}", 100L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(ResultCode.COUPON_ISSUE_SUCCESS.getMessage()));
        }
        @Test
        @DisplayName("쿠폰 소진이면 쿠폰을 발급받을 수 없다.")
        @WithMockUser(username = "user@test.com", authorities = {"MEMBER"})
        void issueCoupon_soldOut_409() throws Exception {
            doThrow(new BusinessException(ErrorCode.COUPON_SOLD_OUT))
                    .when(eventService).issueCoupon(anyLong());

            mockMvc.perform(post("/api/v1/event/coupon/{eventId}", 1L))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409));
            /*.andExpect(jsonPath(("$.message"))*/
        }

        @Test
        @DisplayName("이벤트가 없으면 이벤트를 조회할 수 없다.")
        @WithMockUser(username = "user@test.com", authorities = {"MEMBER"})
        void issueCoupon_notFound_404() throws Exception {
            doThrow(new BusinessException(ErrorCode.EVENT_NOT_FOUND))
                    .when(eventService).issueCoupon(anyLong());

            mockMvc.perform(post("/api/v1/event/coupon/{eventId}", 9999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
        @Test
        @DisplayName("쿠폰 이름이 비어있을 수 없다.")
        void blankName() throws Exception {
            CouponCreateRequestDto invalid =
                    new CouponCreateRequestDto("", "봄맞이", (short)100, (short)20, Instant.now().plus(Duration.ofDays(5)));

            mockMvc.perform(post("/api/v1/event/coupon")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("name"));
        }

        @DisplayName("쿠폰 유효기간이 과거거나 당일이여서는 안된다.")
        @Test
        void createCouponEvent_Fail_DayValid() throws Exception {
            // Given
            CouponCreateRequestDto invalidRequest = new CouponCreateRequestDto(
                    "여름 쿠폰","시원한 여름 맞이 쿠폰", (short) 100, (short) 20,Instant.now().minusNanos(3L));

            mockMvc.perform(post("/api/v1/event/coupon")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("잘못된 입력 값입니다."))
                    .andExpect(jsonPath("$.errors[0].field").value("validDate"))
                    .andExpect(jsonPath("$.errors[0].reason").value("과거, 당일은 유효기간으로 지정될 수 없습니다."));
        }
    }

    @Test
    @DisplayName("사용자는 이벤트 목록을 조회할 수 있다.")
    void getEventList_ok() throws Exception {
        EventListResponseDto p1 = new TestEventListProjection(10L, "레미제라블 스페셜");
        EventListResponseDto p2 = new TestEventListProjection(11L, "위키드 초대권");

        PagingResponse<EventListResponseDto> page =
                PagingResponse.from(List.of(p1, p2), 0, 2, 10);

        given(eventService.getEventList(eq(EventType.TICKET), eq(0), eq(2)))
                .willReturn(page);

        mockMvc.perform(get("/api/v1/event")
                        .param("type", "TICKET")
                        .param("page", "0")
                        .param("size", "2")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(ResultCode.EVENT_INFO_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.content[0].eventId").value(10))
                .andExpect(jsonPath("$.data.content[0].name").value("레미제라블 스페셜"))
                .andExpect(jsonPath("$.data.content[1].eventId").value(11))
                .andExpect(jsonPath("$.data.content[1].name").value("위키드 초대권"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(2));

        @Nested
        @DisplayName("이벤트 목록 조회 실패")
        class GetEventListFail {

            @Test
            @DisplayName("잘못된 이벤트 타입이면 400 (enum 변환 실패)")
            void list_badType_400() throws Exception {
                mockMvc.perform(get("/api/v1/event")
                                .param("type", "NOT_A_TYPE")
                                .param("page", "0")
                                .param("size", "10"))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.status").value(400));
            }

            @Test
            @DisplayName("잘못된 페이징이면 400 (서비스 유효성 실패)")
            void list_badPaging_400() throws Exception {
                doThrow(new BusinessException(ErrorCode.INVALID_INPUT_VALUE))
                        .when(eventService).getEventList(any(), anyInt(), anyInt());

                mockMvc.perform(get("/api/v1/event")
                                .param("type", "COUPON") // 존재하는 enum 값
                                .param("page", "-1")
                                .param("size", "0"))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.status").value(400));
            }
        }
    }

    @Test
    @DisplayName("이벤트 선택 시, 티켓 이벤트의 상세 정보를 조회할 수 있다.")
    void getTicketEventDetail_ok() throws Exception {
        TicketEventDetailResponseDto detail =
                new TicketEventDetailResponseDto(
                        6L,
                        "레미제라블",
                        "서울예술의전당",
                        (short)180,
                        Instant.parse("2025-08-02T12:00:00Z"),
                        "A12",
                        SeatGrade.VIP,
                        (short)1000,
                        "lesmis.jpg",
                        "진행중"
                );

        given(eventService.getTicketEventDetail(6L)).willReturn(detail);

        mockMvc.perform(get("/api/v1/event/ticket/{eventId}", 6L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(ResultCode.EVENT_INFO_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.id").value(6L))
                .andExpect(jsonPath("$.data.performanceTitle").value("레미제라블"))
                .andExpect(jsonPath("$.data.seatNumber").value("A12"))
                .andExpect(jsonPath("$.data.seatGrade").value("VIP"));

        @Nested
        @DisplayName("티켓 이벤트 상세 조회 실패")
        class GetTicketEventDetailFail {

            @Test
            @DisplayName("이벤트가 없으면 404")
            void detail_notFound_404() throws Exception {
                // given
                doThrow(new BusinessException(ErrorCode.EVENT_NOT_FOUND))
                        .when(eventService).getTicketEventDetail(anyLong());

                // when & then
                mockMvc.perform(get("/api/v1/event/ticket/{eventId}", 9999L))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.status").value(404));
            }
        }
    }

    @Nested
    @DisplayName("티켓 이벤트 키워드 검색에 성공한다.")
    class SearchTicketEventsFail {

        @Test
        @DisplayName("사용자는 티켓 이벤트를 특정 키워드로 검색할 수 있다.")
        void searchTicketEvents_ok() throws Exception {
            PagingResponse<TicketListResponseDto> page = getTicketListResponseDtoPagingResponse();

            given(eventService.searchTicketEvents(eq("레미"), eq(0), eq(2)))
                    .willReturn(page);

            mockMvc.perform(get("/api/v1/event/ticket/search")
                            .param("keyword", "레미")
                            .param("page", "0")
                            .param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(ResultCode.EVENT_INFO_SUCCESS.getMessage()))
                    .andExpect(jsonPath("$.data.content[0].eventId").value(21L))
                    .andExpect(jsonPath("$.data.content[0].name").value("레미제라블"))
                    .andExpect(jsonPath("$.data.content[1].eventId").value(22L))
                    .andExpect(jsonPath("$.data.content[1].name").value("위키드"))
                    .andExpect(jsonPath("$.data.totalElements").value(2));
        }

        @Test
        @DisplayName("잘못된 페이징이면 이벤트 키워드를 검색할 수 없다.")
        void search_badPaging_400() throws Exception {
            doThrow(new BusinessException(ErrorCode.INVALID_INPUT_VALUE))
                    .when(eventService).searchTicketEvents(anyString(), anyInt(), anyInt());

            mockMvc.perform(get("/api/v1/event/ticket/search")
                            .param("keyword", "레미제")
                            .param("page", "-1")
                            .param("size", "0"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }
    }

    private static PagingResponse<TicketListResponseDto> getTicketListResponseDtoPagingResponse() {
        TicketListResponseDto r1 = new TicketListResponseDto(
                21L, "레미제라블", (short)10, "img1.jpg", 5L,
                Instant.parse("2025-08-01T00:00:00Z"),
                Instant.parse("2025-08-31T23:59:59Z")
        );
        TicketListResponseDto r2 = new TicketListResponseDto(
                22L, "위키드", (short)15, "img2.jpg", 6L,
                Instant.parse("2025-09-01T00:00:00Z"),
                Instant.parse("2025-09-30T23:59:59Z")
        );

        PagingResponse<TicketListResponseDto> page =
                PagingResponse.from(List.of(r1, r2), 0, 2, 2);
        return page;
    }

    // --- 4) 랜덤 이벤트 5개 조회 ---
    @Test
    @DisplayName("랜덤 진행중인 이벤트 5개를 조회할 수 있다.")
    void getRandomEvents_ok() throws Exception {
        TicketEventResponseDto e1 = new TicketEventResponseDto(
                31L, 100L, "랜덤1", "A열 1번",
                Instant.parse("2025-08-01T00:00:00Z"),
                Instant.parse("2025-08-31T23:59:59Z")
        );
        TicketEventResponseDto e2 = new TicketEventResponseDto(
                32L, 101L, "랜덤2", "A열 2번",
                Instant.parse("2025-08-01T00:00:00Z"),
                Instant.parse("2025-08-31T23:59:59Z")
        );

        PagingResponse<TicketEventResponseDto> page =
                PagingResponse.from(List.of(e1, e2), 0, 2, 2);

        given(eventService.findRandomOngoingEvents()).willReturn(page);

        mockMvc.perform(get("/api/v1/event/random"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(ResultCode.EVENT_INFO_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.content[0].eventId").value(31))
                .andExpect(jsonPath("$.data.content[0].seatNumber").value("A열 1번"))
                .andExpect(jsonPath("$.data.content[1].eventId").value(32))
                .andExpect(jsonPath("$.data.content[1].seatNumber").value("A열 2번"));
    }



    // --- 테스트 전용 Projection 구현체 ---
    private static class TestEventListProjection implements EventListResponseDto {
        private final Long eventId;
        private final String name;

        private TestEventListProjection(Long eventId, String name) {
            this.eventId = eventId;
            this.name = name;
        }
        @Override public Long getEventId() { return eventId; }
        @Override public String getName() { return name; }
    }




    // ---------- 공통 더미 ----------
    private TicketApplyResponseDto dummyApplyResp(long eventId, long memberId, boolean winner) {
        return new TicketApplyResponseDto(eventId, memberId, winner,
                winner ? "축하합니다! 티켓에 당첨되었습니다. \n 예매권은 마이페이지에서 확인하세요."
                        : "아쉽네요. 다음 기회에...");
    }

    private TicketEventDetailResponseDto dummyTicketDetail(long eventId) {
        return new TicketEventDetailResponseDto(
                eventId,
                "레미제라블",
                "서울예술의전당",
                (short) 180,
                Instant.now(),
                "A12",
                SeatGrade.VIP,
                (short) 1000,
                "lesmis.jpg",
                "진행중"
        );
    }

    private TicketEventResponseDto dummyTicketEventResponse(long eventId, long perfId) {
        return new TicketEventResponseDto(
                eventId,
                perfId,
                "기프트 티켓 이벤트",
                "A열 3번",
                Instant.parse("2025-08-01T00:00:00Z"),
                Instant.parse("2025-08-31T23:59:59Z")
        );
    }

/*
    private PagingResponse<TicketListResponseDto> dummyTicketSearch() {
        List<TicketListResponseDto> items = List.of(
                new TicketListResponseDto(10L, "레미제라블", (short) 100, "image", 5L, Instant.now(), Instant.now()),
                new TicketListResponseDto(11L, "위키드", (short) 100, "image", 5L, Instant.now(), Instant.now())
        );

        return new PagingResponse<>(items, 0, 2, 5, 2);
    }

    private PagingResponse<TicketEventResponseDto> dummyRandomTickets() {
        List<TicketEventResponseDto> items = List.of(
                new TicketEventResponseDto(6L, 1L,"레미제라블", "image",  Instant.now(), Instant.now()),
                new TicketEventResponseDto(7L, 2L,"위키드","image", Instant.now(), Instant.now())
        );
        return new PagingResponse<>(items, 0, 2, 5, 2);
    }
*/

}
