package com.profect.tickle.domain.member.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;  // 사용자 고유번호

    @Column(name = "member_email", nullable = false, unique = true, length = 30)
    private String email;  // 이메일

    @Column(name = "member_pw", nullable = false, length = 255)
    private String password;  // 비밀번호

    @Column(name = "member_nickname", nullable = false, length = 10)
    private String nickname;  // 닉네임

    @Column(name = "member_birthday", nullable = false)
    private LocalDate birthday;  // 생년월일

    @Column(name = "member_img", nullable = false, length = 255)
    private String img;  // 프로필 이미지 URL (외부 저장소)

    @Enumerated(EnumType.STRING)
    @Column(name = "member_role", nullable = false)
    private MemberRole memberRole;  // 0 = 사용자, 1 = 주최자, 2 = 관리자

    @Column(name = "member_number", nullable = false, length = 10)
    private String phoneNumber;  // 전화번호

    @Builder.Default
    @ColumnDefault("0")
    @Column(name = "member_point_balance", nullable = false)
    private Integer pointBalance = 0;  // 포인트 잔액

    @Column(name = "member_created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;  // 생성일시

    @Column(name = "member_updated_at", nullable = false)
    private LocalDateTime updatedAt;  // 수정일시

    @Column(name = "member_deleted_at")
    private LocalDateTime deletedAt;  // 삭제일시 (논리 삭제)

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

    @Column(name = "host_biz_depositor", length = 10)
    private String hostBizDepositor;  // 예금주

    @Column(name = "host_biz_bank_number", length = 25)
    private String hostBizBankNumber;  // 계좌번호

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.memberRole == null) {
            this.memberRole = MemberRole.MEMBER;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
