package com.profect.tickle.domain.performance.service;

import com.profect.tickle.domain.performance.dto.response.PerformanceDetailDto;
import com.profect.tickle.domain.performance.mapper.PerformanceMapper;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PerformanceServiceTest {

    @Mock
    PerformanceMapper performanceMapper;

    @InjectMocks
    PerformanceService performanceService;

    @Test
    @DisplayName("삭제되지 않은 공연정보 상세 조회에 성공한다. 상세정보 반환과 함께 조회수 컬럼이 1 증가한다.")
    void TC_PERFORMANCE_001() {
        //given
        Long performanceId = 1L;
        PerformanceDetailDto stub = mock(PerformanceDetailDto.class);
        when(performanceMapper.findDetailById(performanceId)).thenReturn(stub);

        //when
        PerformanceDetailDto result = performanceService.getPerformanceDetail(performanceId);

        //then
        assertThat(result).isSameAs(stub);

        InOrder inOrder = inOrder(performanceMapper);
        inOrder.verify(performanceMapper).findDetailById(performanceId);
        inOrder.verify(performanceMapper).increaseLookCount(performanceId);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    @DisplayName("공연이 없거나 삭제됨 → PERFORMANCE_NOT_FOUND 예외, 조회수 증가 호출 없음")
    void TC_PERFORMANCE_002() {
        // given
        Long performanceId = 999L;
        when(performanceMapper.findDetailById(performanceId)).thenReturn(null);

        // when
        Throwable thrown = catchThrowable(() -> performanceService.getPerformanceDetail(performanceId));

        // then
        assertThat(thrown)
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.PERFORMANCE_NOT_FOUND.getMessage());

        verify(performanceMapper).findDetailById(performanceId);
        verify(performanceMapper, never()).increaseLookCount(anyLong());
        verifyNoMoreInteractions(performanceMapper);
    }
  
}