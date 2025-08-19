package com.profect.tickle.domain.member.repository;

import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.entity.MemberRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@DataJpaTest
@ActiveProfiles(value = "test")
class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("이메일값으로 DB에서 엔티티를 찾는다.")
    void findByEmail() {
        // given
        String targetEmail = "test@tickle.com";
        Instant birthday = Instant.parse("2000-08-04T00:00:00Z");

        Member member = createMember(
                targetEmail,   // email
                "pw1234!",           // password
                birthday,            // birthday
                "테스터",             // nickname
                null,               // 프로필 사진
                "01012345678",       // phoneNumber
                MemberRole.MEMBER,   // role
                null, null, null,    // hostBizNumber, hostBizCeoName, hostBizName
                null, null, null,    // hostBizAddress, hostBizEcommerceRegistrationNumber, hostBizBankName
                null, null           // hostBizDepositor, hostBizBankNumber
        );

        memberRepository.save(member);

        // when
        Member result = memberRepository.findByEmail(targetEmail).orElse(null);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull(); // 저장되어 식별자 발급 확인

        assertThat(result)
                .extracting(
                        Member::getEmail,
                        Member::getNickname,
                        Member::getPhoneNumber,
                        Member::getMemberRole,   // getRole 이라면 Member::getRole 로 변경
                        Member::getBirthday
                )
                .containsExactly(
                        targetEmail,
                        "테스터",
                        "01012345678",
                        MemberRole.MEMBER,
                        birthday
                );

        assertThat(result.getHostBizNumber()).isNull();
        assertThat(result.getHostBizCeo()).isNull();
        assertThat(result.getHostBizName()).isNull();
        assertThat(result.getHostBizAddress()).isNull();
        assertThat(result.getHostBizEcommerceRegistrationNumber()).isNull();
        assertThat(result.getHostBizBank()).isNull();
        assertThat(result.getHostBizDepositor()).isNull();
        assertThat(result.getHostBizBankNumber()).isNull();

    }

    private Member createMember(
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