package com.profect.tickle.global.paging;

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

    public static <T> PagingResponse<T> from(List<T> content, int page, int size, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        boolean isLast = (page + 1) >= totalPages;
        return new PagingResponse<>(content, page, size, totalElements, totalPages, isLast);
    }
}