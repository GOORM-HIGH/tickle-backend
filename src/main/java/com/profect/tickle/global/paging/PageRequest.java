package com.profect.tickle.global.paging;

import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;

public record PageRequest(int page, int size) {
    public PageRequest {
        if(page <0 || size <=0){
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
    public int offset() { return page * size; }
    public static PageRequest of(int page, int size) {
        return new PageRequest(page, size);
    }
    public static int calcTotalPages(long total, int size) {
        return (total == 0) ? 0 : (int) ((total + size - 1) / size);
    }
}
