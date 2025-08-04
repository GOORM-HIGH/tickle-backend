package com.profect.tickle.domain.point.service.impl;

import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.domain.point.dto.response.BootpayConfigResponseDto;
import com.profect.tickle.domain.point.service.BootPayService;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.security.util.SecurityUtil;
import jakarta.transaction.Transactional;
import kr.co.bootpay.pg.Bootpay;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

@Service
@Transactional
public class BootPayServiceImpl implements BootPayService {

    private final String applicationId;
    private final String privateKey;
    private final MemberRepository memberRepository;

    public BootPayServiceImpl(@Value("${api.pay.application_id}") String applicationId,
                          @Value("${api.pay.private_key}") String privateKey, MemberRepository memberRepository) {
        this.applicationId = applicationId;
        this.privateKey = privateKey;
        this.memberRepository = memberRepository;
    }

    public BootpayConfigResponseDto getBootpayConfig() {
        Member member = getMemberOrThrow();
        return BootpayConfigResponseDto.from(applicationId, generateOrderId(member.getId()), member);
    }

    public String getAccessToken() {
        try {
            Bootpay bootpay = new Bootpay(applicationId, privateKey);
            HashMap<String, Object> res = bootpay.getAccessToken();

            if (res.get("error_code") == null) {
                System.out.println("AccessToken 발급 성공: " + res);
                return (String) ((HashMap<String, Object>) res.get("data")).get("access_token");
            } else {
                System.out.println("AccessToken 발급 실패: " + res);
                throw new RuntimeException("Bootpay 인증 실패: " + res.get("message"));
            }

        } catch (Exception e) {
            throw new RuntimeException("Bootpay 토큰 요청 중 예외 발생", e);
        }
    }
    private String generateOrderId(Long memberId) {
        return "order_" + memberId + "_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    private Member getMemberOrThrow() {
        Long memberId = SecurityUtil.getSignInMemberId();
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
