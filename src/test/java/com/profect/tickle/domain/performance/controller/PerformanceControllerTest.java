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
    @DisplayName("장르 목록을 조회한다")
    void TC_PERFORMANCE_201() throws Exception {
        given(performanceService.getAllGenre()).willReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/performance/genre"))
                .andExpect(status().isOk());

        verify(performanceService).getAllGenre();
    }

    @Test
    @DisplayName("장르별 공연 목록을 페이징 조회한다")
    void TC_PERFORMANCE_202() throws Exception {
        PagingResponse<PerformanceDto> paging =
                new PagingResponse<>(Collections.emptyList(), 0, 8, 0L, 0, false);
        given(performanceService.getPerformancesByGenre(eq(10L), eq(0), eq(8))).willReturn(paging);

        mockMvc.perform(get("/api/v1/performance/genre/{genreId}", 10L)
                        .param("page", "0").param("size", "8"))
                .andExpect(status().isOk());

        verify(performanceService).getPerformancesByGenre(10L, 0, 8);
    }

    @Test
    @DisplayName("장르별 TOP10 공연을 조회한다")
    void TC_PERFORMANCE_203() throws Exception {
        given(performanceService.getTop10ByGenre(1L)).willReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/performance/genre/{genreId}/ranking", 1L))
                .andExpect(status().isOk());

        verify(performanceService).getTop10ByGenre(1L);
    }

    @Test
    @DisplayName("전체 공연 TOP10을 조회한다")
    void TC_PERFORMANCE_204() throws Exception {
        given(performanceService.getTop10Performances()).willReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/performance/ranking"))
                .andExpect(status().isOk());

        verify(performanceService).getTop10Performances();
    }

    @Test
    @DisplayName("공연 상세 정보를 조회한다")
    void TC_PERFORMANCE_205() throws Exception {
        given(performanceService.getPerformanceDetail(123L)).willReturn(new PerformanceDetailDto());

        mockMvc.perform(get("/api/v1/performance/{id}", 123L))
                .andExpect(status().isOk());

        verify(performanceService).getPerformanceDetail(123L);
    }

    @Test
    @DisplayName("오픈 예정 공연 4개를 조회한다")
    void TC_PERFORMANCE_206() throws Exception {
        given(performanceService.getTop4UpcomingPerformances()).willReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/performance/open"))
                .andExpect(status().isOk());

        verify(performanceService).getTop4UpcomingPerformances();
    }

    @Test
    @DisplayName("공연을 키워드로 검색한다")
    void TC_PERFORMANCE_207() throws Exception {
        PagingResponse<PerformanceDto> paging =
                new PagingResponse<>(Collections.emptyList(), 1, 8, 0L, 0, false);
        given(performanceService.searchPerformances(eq("뮤지컬"), eq(1), eq(8))).willReturn(paging);

        mockMvc.perform(get("/api/v1/performance/search/{keyword}", "뮤지컬")
                        .param("page", "1").param("size", "8"))
                .andExpect(status().isOk());

        verify(performanceService).searchPerformances("뮤지컬", 1, 8);
    }

    @Test
    @DisplayName("특정 공연과 관련된 추천 공연을 조회한다")
    void TC_PERFORMANCE_208() throws Exception {
        given(performanceService.getRelatedPerformances(321L)).willReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/performance/{id}/recommend", 321L))
                .andExpect(status().isOk());

        verify(performanceService).getRelatedPerformances(321L);
    }

    // -------- 인증/권한 필요한 엔드포인트 --------

    @Nested
    class HostSecured {

        @Test
        @WithMockUser(roles = "HOST")
        @DisplayName("HOST가 공연을 생성한다")
        void TC_PERFORMANCE_209() throws Exception {
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

            given(performanceService.createPerformance(any(PerformanceRequestDto.class)))
                    .willReturn(new PerformanceResponseDto());

            mockMvc.perform(post("/api/v1/performance")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());

            verify(performanceService).createPerformance(any(PerformanceRequestDto.class));
        }

        @Test
        @WithMockUser(roles = "HOST")
        @DisplayName("HOST가 공연 정보를 수정한다")
        void TC_PERFORMANCE_210() throws Exception {
            UpdatePerformanceRequestDto dto = new UpdatePerformanceRequestDto();

            given(performanceService.updatePerformance(eq(777L), any(UpdatePerformanceRequestDto.class)))
                    .willReturn(new PerformanceResponseDto());

            try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
                mocked.when(SecurityUtil::getSignInMemberId).thenReturn(1L);

                mockMvc.perform(patch("/api/v1/performance/{id}", 777L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                        .andExpect(status().isOk());
            }

            verify(performanceService).updatePerformance(eq(777L), any(UpdatePerformanceRequestDto.class));
        }

        @Test
        @WithMockUser(roles = "HOST")
        @DisplayName("HOST가 공연을 삭제한다")
        void TC_PERFORMANCE_211() throws Exception {
            try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
                mocked.when(SecurityUtil::getSignInMemberId).thenReturn(99L);

                mockMvc.perform(delete("/api/v1/performance/{id}", 999L))
                        .andExpect(status().isOk());
            }

            verify(performanceService).deletePerformance(999L, 99L);
        }

        @Test
        @WithMockUser(roles = "HOST")
        @DisplayName("HOST가 본인이 등록한 공연 목록을 조회한다")
        void TC_PERFORMANCE_212() throws Exception {
            PagingResponse<PerformanceHostDto> paging =
                    new PagingResponse<>(Collections.emptyList(), 0, 20, 0L, 0, false);
            given(performanceService.getMyPerformances(eq(100L), eq(0), eq(20))).willReturn(paging);

            try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
                mocked.when(SecurityUtil::getSignInMemberId).thenReturn(100L);

                mockMvc.perform(get("/api/v1/performance/host")
                                .param("page", "0").param("size", "20"))
                        .andExpect(status().isOk());
            }

            verify(performanceService).getMyPerformances(100L, 0, 20);
        }
    }
}
