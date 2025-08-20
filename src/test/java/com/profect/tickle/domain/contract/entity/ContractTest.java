package com.profect.tickle.domain.contract.entity;

import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.entity.MemberRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


@ExtendWith(MockitoExtension.class)
class ContractTest {

    @Test
    @DisplayName("새로운 계약 생성")
    void createContract() {
        // given
        Instant birthday = Instant.parse("2000-08-04T00:00:00Z");
        BigDecimal newContractBalance = new BigDecimal("0.01");
        Member newMember = createMember(
                1L,
                "user@example.com", "pw1234!", birthday,
                "닉네임", null, "01012345678", MemberRole.HOST,
                "4981401407", "김판매", "공연단단해지기", "서울특별시 마포구 덕우빌딩 123",
                "12345-서울마포-123",
                "KB국민은행", "김판매", "12312312312"
        );

        // when
        Contract newContract = Contract.createContract(newMember, newContractBalance);

        // then
        assertThat(newContract)
                .isNotNull()
                .extracting(Contract::getMember, Contract::getCharge)
                .containsExactly(newMember, newContractBalance);
    }

    private Member createMember(
            Long id,
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
            String hostBizBankNumber
    ) {
        return Member.builder()
                .id(id)
                .email(email)
                .password(password)
                .birthday(birthday)
                .nickname(nickname)
                .img(img)
                .phoneNumber(phoneNumber)
                .memberRole(role)
                .hostBizNumber(hostBizNumber)
                .hostBizCeo(hostBizCeoName)
                .hostBizName(hostBizName)
                .hostBizAddress(hostBizAddress)
                .hostBizEcommerceRegistrationNumber(hostBizEcommerceRegistrationNumber)
                .hostBizBank(hostBizBankName)
                .hostBizDepositor(hostBizDepositor)
                .hostBizBankNumber(hostBizBankNumber)
                .build();
    }
}