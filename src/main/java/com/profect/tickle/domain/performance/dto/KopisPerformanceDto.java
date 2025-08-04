package com.profect.tickle.domain.performance.dto;

import lombok.Data;

@Data
public class KopisPerformanceDto {
    private String mt20id;     // KOPIS 공연 고유 ID (DB 저장 X)
    private String prfnm;      // 공연명
    private String poster;     // 포스터 이미지
    private String fcltynm;    // 공연 시설명
    private String genrenm;      // 장르명
    private String prfpdfrom;  // 공연 시작일 (yyyy.MM.dd)
    private String prfpdto;    // 공연 종료일 (yyyy.MM.dd)
}

