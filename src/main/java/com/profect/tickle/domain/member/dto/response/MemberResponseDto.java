package com.profect.tickle.domain.member.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.entity.MemberRole;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MemberResponseDto {

    private Long id;                     // 사용자 고유번호
    private String email;                // 이메일
    private String nickname;             // 닉네임
    private Instant birthday;          // 생년월일
    private String img;                  // 프로필 이미지 URL
    private Integer pointBalance;        // 포인트 잔액
    private MemberRole memberRole;           // 사용자 역할 (USER, HOST, ADMIN)

    // 주최자(Host) 정보
    private String hostBizNumber;                       // 사업자 등록번호
    private String hostBizCeo;                          // 대표자명
    private String hostBizName;                         // 상호명
    private String hostBizAddress;                      // 사업장 주소
    private String hostBizEcommerceRegistrationNumber;  // 통신판매업 신고번호
    private String hostBizBank;                         // 정산 계좌 은행명
    private String hostBizDepositor;                    // 정산 계좌 예금주
    private String hostBizBankNumber;                   // 정산 계좌 번호
    private BigDecimal contractCharge;                  // 정산 수수료

    private Instant createdAt;  // 계정 생성일
    private Instant updatedAt;  // 계정 정보 수정일
    private Instant deletedAt;  // 계정 삭제일(논리 삭제)

}
