package com.profect.tickle.domain.member.entity;

import com.profect.tickle.domain.member.dto.request.CreateMemberRequestDto;
import com.profect.tickle.domain.point.entity.Point;
import com.profect.tickle.domain.point.entity.PointTarget;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
    private Instant birthday;  // 생년월일

    @Column(name = "member_img", nullable = false, length = 255)
    private String img;  // 프로필 이미지 URL (외부 저장소)

    @Enumerated(EnumType.STRING)
    @Column(name = "member_role", nullable = false)
    private MemberRole memberRole;  // 0 = 사용자, 1 = 주최자, 2 = 관리자

    @Column(name = "member_number", nullable = false, length = 11)
    private String phoneNumber;  // 전화번호

    @Builder.Default
    @ColumnDefault("0")
    @Column(name = "member_point_balance", nullable = false)
    private Integer pointBalance = 0;  // 포인트 잔액

    @Column(name = "member_created_at", nullable = false, updatable = false)
    private Instant createdAt;  // 생성일시

    @Column(name = "member_updated_at", nullable = false)
    private Instant updatedAt;  // 수정일시

    @Column(name = "member_deleted_at")
    private Instant deletedAt;  // 삭제일시 (논리 삭제)

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

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Point> points = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CouponReceived> receivedCoupons = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.memberRole == null) {
            this.memberRole = MemberRole.MEMBER;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public static Member createMember(CreateMemberRequestDto dto) {
        return Member.builder()
                .email(dto.getEmail())
                .password(dto.getPassword())  // 서비스에서 BCrypt로 암호화
                .nickname(dto.getNickname())
                .birthday(dto.getBirthday())
                .img(dto.getImg())
                .phoneNumber(dto.getPhoneNumber())
                .memberRole(dto.getRole() != null ? dto.getRole() : MemberRole.MEMBER) // 기본값 MEMBER
                .hostBizNumber(dto.getHostBizNumber())
                .hostBizCeo(dto.getHostBizCeoName())
                .hostBizName(dto.getHostBizName())
                .hostBizAddress(dto.getHostBizAddress())
                .hostBizEcommerceRegistrationNumber(dto.getHostBizEcommerceRegistrationNumber())
                .hostBizBank(dto.getHostBizBankName())
                .hostBizDepositor(dto.getHostBizDepositor())
                .hostBizBankNumber(dto.getHostBizBankNumber())
                .build();
    }

    public void encryptPassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public Point deductPoint(Short price, PointTarget target) {
        if (this.getPointBalance() < price) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_POINT);
        }
        this.usePoint(price);

        return Point.deduct(this, price, target);
    }

    public Point deductPoint(Integer price, PointTarget target) {
        if (this.getPointBalance() < price) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_POINT);
        }

        pointBalance -= price;

        return Point.deduct(this, price, target);
    }

    public void usePoint(Short perPrice) {
        pointBalance -= perPrice;
    }

    public void addPoint(int point) {
        pointBalance += point;
    }
}
