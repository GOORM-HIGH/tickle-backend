package com.profect.tickle.domain.performance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.profect.tickle.domain.performance.dto.request.PerformanceRequestDto;
import com.profect.tickle.domain.performance.dto.request.UpdatePerformanceRequestDto;
import com.profect.tickle.domain.performance.dto.response.*;
import com.profect.tickle.domain.performance.entity.HallType;
import com.profect.tickle.domain.performance.mapper.PerformanceMapper;
import com.profect.tickle.domain.performance.service.PerformanceService;
import com.profect.tickle.global.paging.PagingResponse;
import com.profect.tickle.global.security.util.SecurityUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PerformanceController.class)
@AutoConfigureMockMvc(addFilters = false)
class PerformanceControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    PerformanceService performanceService;

    @MockBean
    PerformanceMapper performanceMapper;

    @MockBean com.profect.tickle.domain.chat.config.ChatJwtAuthenticationInterceptor chatJwtAuthenticationInterceptor;
    @MockBean com.profect.tickle.global.security.util.JwtUtil jwtUtil;

    // -------- 공개 엔드포인트 --------

    @Test
    @DisplayName("GET /performance/genre - 장르 목록 조회 200")
    void getGenres_ok() throws Exception {
        // given
        List<GenreDto> genres = Collections.emptyList();
        given(performanceService.getAllGenre()).willReturn(genres);

        // when & then
        mockMvc.perform(get("/api/v1/performance/genre"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.data").isArray());

        verify(performanceService).getAllGenre();
    }

    @Test
    @DisplayName("GET /performance/genre/{id} - 장르별 공연 목록(페이징) 200")
    void getPerformancesByGenre_ok() throws Exception {
        // given
        PagingResponse<PerformanceDto> paging =
                new PagingResponse<>(Collections.emptyList(), 0, 8, 0L, 0, false);
        given(performanceService.getPerformancesByGenre(eq(10L), eq(0), eq(8))).willReturn(paging);

        // when & then
        mockMvc.perform(get("/api/v1/performance/genre/{genreId}", 10L)
                        .param("page", "0").param("size", "8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(8));

        verify(performanceService).getPerformancesByGenre(10L, 0, 8);
    }

    @Test
    @DisplayName("GET /performance/genre/{id}/ranking - 장르 TOP10 200")
    void getTop10ByGenre_ok() throws Exception {
        given(performanceService.getTop10ByGenre(1L)).willReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/performance/genre/{genreId}/ranking", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.data").isArray());

        verify(performanceService).getTop10ByGenre(1L);
    }

    @Test
    @DisplayName("GET /performance/ranking - 전체 랭킹 TOP10 200")
    void getTop10Performances_ok() throws Exception {
        given(performanceService.getTop10Performances()).willReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/performance/ranking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.data").isArray());

        verify(performanceService).getTop10Performances();
    }

    @Test
    @DisplayName("GET /performance/{id} - 공연 상세 200")
    void getPerformanceDetail_ok() throws Exception {
        PerformanceDetailDto detail = new PerformanceDetailDto();
        given(performanceService.getPerformanceDetail(123L)).willReturn(detail);

        mockMvc.perform(get("/api/v1/performance/{id}", 123L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.data").exists());

        verify(performanceService).getPerformanceDetail(123L);
    }

    @Test
    @DisplayName("GET /performance/open - 오픈예정 4개 200")
    void getOpenPerformances_ok() throws Exception {
        given(performanceService.getTop4UpcomingPerformances()).willReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/performance/open"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.data").isArray());

        verify(performanceService).getTop4UpcomingPerformances();
    }

    @Test
    @DisplayName("GET /performance/search/{keyword} - 검색(페이징) 200")
    void searchPerformances_ok() throws Exception {
        PagingResponse<PerformanceDto> paging =
                new PagingResponse<>(Collections.emptyList(), 1, 8, 0L, 0, false);
        given(performanceService.searchPerformances(eq("뮤지컬"), eq(1), eq(8))).willReturn(paging);

        mockMvc.perform(get("/api/v1/performance/search/{keyword}", "뮤지컬")
                        .param("page", "1").param("size", "8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(8));

        verify(performanceService).searchPerformances("뮤지컬", 1, 8);
    }

    @Test
    @DisplayName("GET /performance/{id}/recommend - 추천 200")
    void recommend_ok() throws Exception {
        given(performanceService.getRelatedPerformances(321L)).willReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/performance/{id}/recommend", 321L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.data").isArray());

        verify(performanceService).getRelatedPerformances(321L);
    }

    // -------- 인증/권한 필요한 엔드포인트 --------

    @Nested
    class HostSecured {

        @Test
        @WithMockUser(roles = "HOST")
        @DisplayName("POST /performance - 공연 생성(HOST) 201")
        void create_ok() throws Exception {
            PerformanceRequestDto req = PerformanceRequestDto.builder()
                    .title("테스트 공연")
                    .genreId(1L)
                    .date(Instant.now())
                    .runtime((short) 120)
                    .hallType(HallType.A)
                    .img("poster.png")
                    .startDate(Instant.now())
                    .endDate(Instant.now().plusSeconds(3600))
                    .build();
            PerformanceResponseDto resp = new PerformanceResponseDto();

            given(performanceService.createPerformance(any(PerformanceRequestDto.class))).willReturn(resp);

            mockMvc.perform(post("/api/v1/performance")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(201))
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.data").exists());

            verify(performanceService).createPerformance(any(PerformanceRequestDto.class));
        }

        @Test
        @WithMockUser(roles = "HOST")
        @DisplayName("PATCH /performance/{id} - 공연 수정(HOST) 200")
        void update_ok() throws Exception {
            UpdatePerformanceRequestDto dto = new UpdatePerformanceRequestDto();
            PerformanceResponseDto resp = new PerformanceResponseDto();

            given(performanceService.updatePerformance(eq(777L), any(UpdatePerformanceRequestDto.class)))
                    .willReturn(resp);

            try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
                mocked.when(SecurityUtil::getSignInMemberId).thenReturn(1L);

                mockMvc.perform(patch("/api/v1/performance/{id}", 777L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value(200))
                        .andExpect(jsonPath("$.message").exists())
                        .andExpect(jsonPath("$.data").exists());
            }

            verify(performanceService).updatePerformance(eq(777L), any(UpdatePerformanceRequestDto.class));
        }

        @WithMockUser(roles = "HOST")
        @Test
        @DisplayName("DELETE /performance/{id} - 공연 삭제(HOST) 200")
        void delete_ok() throws Exception {
            try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
                mocked.when(SecurityUtil::getSignInMemberId).thenReturn(99L);

                mockMvc.perform(delete("/api/v1/performance/{id}", 999L))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value(200))
                        .andExpect(jsonPath("$.message").exists())
                        // 정책: data=null이면 필드 생략 → doesNotExist()
                        .andExpect(jsonPath("$.data").doesNotExist());
            }

            verify(performanceService).deletePerformance(999L, 99L);
        }

        @Test
        @WithMockUser(roles = "HOST")
        @DisplayName("GET /performance/host - 본인 생성 공연(페이징) 200")
        void getHostPerformances_ok() throws Exception {
            PagingResponse<PerformanceHostDto> paging =
                    new PagingResponse<>(Collections.emptyList(), 0, 20, 0L, 0, false);
            given(performanceService.getMyPerformances(eq(100L), eq(0), eq(20))).willReturn(paging);

            try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
                mocked.when(SecurityUtil::getSignInMemberId).thenReturn(100L);

                mockMvc.perform(get("/api/v1/performance/host")
                                .param("page", "0").param("size", "20"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value(200))
                        .andExpect(jsonPath("$.message").exists())
                        .andExpect(jsonPath("$.data.page").value(0))
                        .andExpect(jsonPath("$.data.size").value(20));
            }

            verify(performanceService).getMyPerformances(100L, 0, 20);
        }
    }
}
