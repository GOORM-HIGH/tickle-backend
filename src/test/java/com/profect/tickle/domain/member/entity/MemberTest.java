package com.profect.tickle.domain.member.entity;

import com.profect.tickle.domain.member.dto.request.CreateMemberServiceRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(MockitoExtension.class)
class MemberTest {

    @Test
    @DisplayName("입력한 회원정보로 일반 회원 엔티티 생성")
    void createMember() {
        // given
        Instant birthday = Instant.parse("1981-11-04T00:00:00Z");
        BigDecimal hostContractCharge = new BigDecimal("0.01");
        CreateMemberServiceRequestDto dto = createMemberServiceRequest(
                "user@example.com", "pw1234!", birthday,
                "닉네임", null, "01012345678", MemberRole.MEMBER,
                null, null, null, null,
                null,
                null, null, null,
                hostContractCharge
        );

        // when
        Member result = Member.createMember(dto);

        // then
        assertThat(result)
                .extracting(
                        Member::getEmail,
                        Member::getPassword,
                        Member::getNickname,
                        Member::getImg,
                        Member::getPhoneNumber,
                        Member::getMemberRole,
                        Member::getBirthday,
                        Member::getHostBizNumber,
                        Member::getHostBizCeo,
                        Member::getHostBizName,
                        Member::getHostBizAddress,
                        Member::getHostBizEcommerceRegistrationNumber,
                        Member::getHostBizBank,
                        Member::getHostBizDepositor,
                        Member::getHostBizBankNumber
                )
                .containsExactly(
                        "user@example.com", null, "닉네임", null, "01012345678",
                        MemberRole.MEMBER, birthday,
                        null, null, null, null,
                        null, null, null,
                        null
                );
    }

    @Test
    @DisplayName("입력한 회원정보로 판매자 회원 엔티티를 생성한다.")
    void createHostMember() {
        // given
        Instant birthday = Instant.parse("1981-11-04T00:00:00Z");
        BigDecimal hostContractCharge = new BigDecimal("0.01");
        CreateMemberServiceRequestDto dto = createMemberServiceRequest(
                "user@example.com", "pw1234!", birthday,
                "닉네임", null, "01012345678", MemberRole.HOST,
                "4981401407", "김판매", "공연단단해지기", "서울특별시 마포구 덕우빌딩 123",
                "12345-서울마포-123",
                "KB국민은행", "김판매", "12312312312",
                hostContractCharge
        );

        // when
        Member result = Member.createMember(dto);

        // then
        assertThat(result)
                .extracting(
                        Member::getEmail,
                        Member::getPassword,
                        Member::getNickname,
                        Member::getImg,
                        Member::getPhoneNumber,
                        Member::getMemberRole,
                        Member::getBirthday,
                        Member::getHostBizNumber,
                        Member::getHostBizCeo,
                        Member::getHostBizName,
                        Member::getHostBizAddress,
                        Member::getHostBizEcommerceRegistrationNumber,
                        Member::getHostBizBank,
                        Member::getHostBizDepositor,
                        Member::getHostBizBankNumber
                )
                .containsExactly(
                        "user@example.com", null, "닉네임", null, "01012345678",
                        MemberRole.HOST, birthday,
                        "4981401407", "김판매", "공연단단해지기", "서울특별시 마포구 덕우빌딩 123",
                        "12345-서울마포-123", "KB국민은행", "김판매",
                        "12312312312"
                );
    }

    private CreateMemberServiceRequestDto createMemberServiceRequest(
            String email,
            String password,
            Instant birthday,
            String nickname,
            String img,
            String phoneNumber,
            MemberRole role,
            String hostBizNumber,
            String hostBizCeoName,
            String hostBizName,
            String hostBizAddress,
            String hostBizEcommerceRegistrationNumber,
            String hostBizBankName,
            String hostBizDepositor,
            String hostBizBankNumber,
            BigDecimal hostContractCharge
    ) {
        return new CreateMemberServiceRequestDto(
                email, password, birthday, nickname, img, phoneNumber, role,
                hostBizNumber, hostBizCeoName, hostBizName, hostBizAddress,
                hostBizEcommerceRegistrationNumber, hostBizBankName,
                hostBizDepositor, hostBizBankNumber, hostContractCharge
        );
    }
}