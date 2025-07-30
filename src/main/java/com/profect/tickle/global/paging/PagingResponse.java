package com.profect.tickle.global.paging;

import com.profect.tickle.domain.event.dto.response.EventListResponseDto;
import org.springframework.data.domain.Page;

import java.util.List;

public record PagingResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean isLast
) {
    public static <T> PagingResponse<T> from(Page<T> page) {
        return new PagingResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}