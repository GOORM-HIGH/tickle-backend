package com.profect.tickle.domain.chat.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginationDto {

    private Integer currentPage;
    private Integer pageSize;
    private Long totalElements;
    private Integer totalPages;
    private Boolean hasNext;

    // 페이징 정보 생성 헬퍼 메서드
    public static PaginationDto of(int currentPage, int pageSize, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        boolean hasNext = currentPage < (totalPages - 1);

        return PaginationDto.builder()
                .currentPage(currentPage)
                .pageSize(pageSize)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasNext(hasNext)
                .build();
    }
}
