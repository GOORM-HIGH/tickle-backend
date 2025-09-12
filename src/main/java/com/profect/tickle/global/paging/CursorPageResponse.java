package com.profect.tickle.global.paging;

import java.util.List;

public record CursorPageResponse<T>(
        List<T> items,   // 실제 데이터 (공연 목록)
        Cursor nextCursor,  // 다음 페이지 커서 (없으면 null)
        boolean hasNext    // 다음 페이지 존재 여부
){}