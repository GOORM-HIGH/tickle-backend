package com.profect.tickle.domain.user.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.profect.tickle.domain.user.entity.User;
import com.profect.tickle.domain.user.entity.UserRole;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponseDTO {

    private Long id;                     // 사용자 고유번호
    private String email;                // 이메일
    private String nickname;             // 닉네임
    private LocalDate birthday;          // 생년월일
    private String img;                  // 프로필 이미지 URL
    private Integer pointBalance;        // 포인트 잔액
    private UserRole userRole;           // 사용자 역할 (USER, HOST, ADMIN)

    // 주최자(Host) 정보
    private String hostBizNumber;                       // 사업자 등록번호
    private String hostBizCeo;                          // 대표자명
    private String hostBizName;                         // 상호명
    private String hostBizAddress;                      // 사업장 주소
    private String hostBizEcommerceRegistrationNumber;  // 통신판매업 신고번호
    private String hostBizBank;                         // 정산 계좌 은행명
    private String hostBizDepositor;                    // 정산 계좌 예금주
    private String hostBizBankNumber;                   // 정산 계좌 번호

    private LocalDateTime createdAt;  // 계정 생성일
    private LocalDateTime updatedAt;  // 계정 정보 수정일
    private LocalDateTime deletedAt;  // 계정 삭제일(논리 삭제)

    /** Entity → DTO 변환 */
    public static UserResponseDTO fromEntity(User userEntity) {
        return UserResponseDTO.builder()
                .id(userEntity.getId())
                .email(userEntity.getEmail())
                .nickname(userEntity.getNickname())
                .birthday(userEntity.getBirthday())
                .img(userEntity.getImg())
                .pointBalance(userEntity.getPointBalance())
                .userRole(userEntity.getUserRole())
                .hostBizNumber(userEntity.getHostBizNumber())
                .hostBizCeo(userEntity.getHostBizCeo())
                .hostBizName(userEntity.getHostBizName())
                .hostBizAddress(userEntity.getHostBizAddress())
                .hostBizEcommerceRegistrationNumber(userEntity.getHostBizEcommerceRegistrationNumber())
                .hostBizBank(userEntity.getHostBizBank())
                .hostBizDepositor(userEntity.getHostBizDepositor())
                .hostBizBankNumber(userEntity.getHostBizBankNumber())
                .createdAt(userEntity.getCreatedAt())
                .updatedAt(userEntity.getUpdatedAt())
                .deletedAt(userEntity.getDeletedAt())
                .build();
    }
}
