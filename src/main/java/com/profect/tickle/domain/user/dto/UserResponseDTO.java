package com.profect.tickle.domain.user.dto;

import com.profect.tickle.domain.user.entity.User;
import com.profect.tickle.domain.user.entity.UserRole;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class UserResponseDTO {

    private Long userId;               // 사용자 고유번호
    private String userEmail;          // 이메일
    private String userNickname;       // 닉네임
    private LocalDate userBirthday;      // 생년월일
    private String userImg;            // 프로필 이미지 URL
    private Integer userPointBalance;  // 포인트 잔액
    private UserRole userRole;         // 사용자 역할 (USER, HOST, ADMIN)

    // 주최자(Host) 정보
    private String hostBizNumber;
    private String hostBizCeo;
    private String hostBizName;
    private String hostBizAddress;
    private String hostBizEcommerceRegistrationNumber;
    private String hostBizBank;
    private String hostBizDepositor;
    private String hostBizBankNumber;

    private LocalDateTime userCreatedAt;
    private LocalDateTime userUpdatedAt;

    /** Entity → DTO 변환 */
    public static UserResponseDTO fromEntity(User userEntity) {
        return UserResponseDTO.builder()
                .userId(userEntity.getUserId())
                .userEmail(userEntity.getUserEmail())
                .userNickname(userEntity.getUserNickname())
                .userBirthday(userEntity.getUserBirthday())
                .userImg(userEntity.getUserImg())
                .userPointBalance(userEntity.getUserPointBalance())
                .userRole(userEntity.getUserRole())
                .hostBizNumber(userEntity.getHostBizNumber())
                .hostBizCeo(userEntity.getHostBizCeo())
                .hostBizName(userEntity.getHostBizName())
                .hostBizAddress(userEntity.getHostBizAddress())
                .hostBizEcommerceRegistrationNumber(userEntity.getHostBizEcommerceRegistrationNumber())
                .hostBizBank(userEntity.getHostBizBank())
                .hostBizDepositor(userEntity.getHostBizDepositor())
                .hostBizBankNumber(userEntity.getHostBizBankNumber())
                .userCreatedAt(userEntity.getUserCreatedAt())
                .userUpdatedAt(userEntity.getUserUpdatedAt())
                .build();
    }
}
