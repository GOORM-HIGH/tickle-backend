package com.profect.tickle.domain.member.service;

import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.entity.MemberRole;
import com.profect.tickle.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.Instant;

@ExtendWith(MockitoExtension.class)
class SmtpMailSenderTest {

    @MockBean
    private MemberRepository memberRepository;

//    @Test
//    @DisplayName("")
//    void TC_SIGNUP_001() {
//        // given
//        Instant birthday = Instant.parse("2000-08-04T00:00:00Z");
//
//        Member newMember = createMember(
//                1L,
//                "test@tickle.com",   // email
//                "pw1234!",           // password
//                birthday,            // birthday
//                "테스터",             // nickname
//                null,               // 프로필 사진
//                "01012345678",       // phoneNumber
//                MemberRole.MEMBER,   // role
//                null, null, null,    // hostBizNumber, hostBizCeoName, hostBizName
//                null, null, null,    // hostBizAddress, hostBizEcommerceRegistrationNumber, hostBizBankName
//                null, null           // hostBizDepositor, hostBizBankNumber
//        );
//
//        BDDMockito.given(memberRepository.save(BDDMockito.any())).willReturn(newMember);
//
//        // when
//
//
//        // then
//
//    }

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