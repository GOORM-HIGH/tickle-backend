package com.profect.tickle.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;  // 사용자 고유번호

    @Column(name = "user_email", nullable = false, unique = true, length = 30)
    private String userEmail;  // 이메일

    @Column(name = "user_pw", nullable = false, length = 255)
    private String userPw;  // 비밀번호

    @Column(name = "user_nickname", nullable = false, length = 10)
    private String userNickname;  // 닉네임

    @Column(name = "user_birthday", nullable = false)
    private LocalDate userBirthday;  // 생년월일

    @Column(name = "user_img", nullable = false, length = 255)
    private String userImg;  // 프로필 이미지 URL (외부 저장소)

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", nullable = false)
    private UserRole userRole;  // 0 = 사용자, 1 = 주최자, 2 = 관리자

    @Column(name = "user_number", nullable = false, length = 10)
    private String userNumber;  // 전화번호

    @Column(name = "user_point_balance", nullable = false)
    private Integer userPointBalance = 0;  // 포인트 잔액

    @Column(name = "user_created_at", nullable = false, updatable = false)
    private LocalDateTime userCreatedAt;  // 생성일시

    @Column(name = "user_updated_at", nullable = false)
    private LocalDateTime userUpdatedAt;  // 수정일시

    @Column(name = "user_deleted_at")
    private LocalDateTime userDeletedAt;  // 삭제일시 (논리 삭제)

    // 주최자(Host) 전용 필드
    @Column(name = "host_biz_number", length = 15)
    private String hostBizNumber;  // 사업자 등록번호

    @Column(name = "host_biz_ceo", length = 10)
    private String hostBizCeo;  // 대표자명

    @Column(name = "host_biz_name", length = 15)
    private String hostBizName;  // 상호명

    @Column(name = "host_biz_address", length = 50)
    private String hostBizAddress;  // 주소

    @Column(name = "host_biz_ecommerce_registration_number", length = 30)
    private String hostBizEcommerceRegistrationNumber;  // 통신판매업 신고번호

    @Column(name = "host_biz_bank", length = 10)
    private String hostBizBank;  // 은행명

    @Column(name = "host_biz_ depositor", length = 10)
    private String hostBizDepositor;  // 예금주

    @Column(name = "host_biz_bank_number", length = 25)
    private String hostBizBankNumber;  // 계좌번호

    @PrePersist
    public void prePersist() {
        this.userCreatedAt = LocalDateTime.now();
        this.userUpdatedAt = LocalDateTime.now();
        if (this.userRole == null) {
            this.userRole = UserRole.USER;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.userUpdatedAt = LocalDateTime.now();
    }
}
