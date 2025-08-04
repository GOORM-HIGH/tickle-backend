package com.profect.tickle.domain.member.service;

import com.profect.tickle.domain.member.dto.request.CreateMemberRequestDto;
import com.profect.tickle.domain.member.entity.EmailValidationCode;
import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.mapper.MemberMapper;
import com.profect.tickle.domain.member.repository.EmailValidationCodeRepository;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.security.util.principal.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberService implements UserDetailsService {

    private final PasswordEncoder passwordEncoder;

    private final MemberMapper memberMapper;
    private final MemberRepository memberRepository;
    private final EmailValidationCodeRepository emailValidationCodeRepository;


    @Transactional
    public void createMember(CreateMemberRequestDto createUserRequest) {
        Member newMember = Member.createMember(createUserRequest);
        newMember.encryptPassword(passwordEncoder.encode(createUserRequest.getPassword()));
        memberRepository.save(newMember);
    }

    // 로그인 요청 시 AuthenticationManager를 통해서 호출 될 메서드
    @Override
    public UserDetails loadUserByUsername(String inputEmail) throws UsernameNotFoundException {
        // 인증 토큰에 담긴 email이 메서드로 넘어오므로 해당 값을 기준으로 DB에서 조회한다.
        Member signInMember = memberRepository.findByEmail(inputEmail)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 계정입니다."));

        List<GrantedAuthority> grantedAuthorityList = new ArrayList<>();
        grantedAuthorityList.add(new SimpleGrantedAuthority(signInMember.getMemberRole().name()));

        // 내부적으로 비밀번호가 일치하는 확인도 한다.
        return new CustomUserDetails(
                signInMember.getId(),
                signInMember.getEmail(),
                signInMember.getPassword(),
                signInMember.getNickname(),
                grantedAuthorityList
        );
    }

    @Transactional
    public void createEmailValidationCode(String email) {
        Member member = memberRepository.findByEmail(email).orElse(null);
        String newValidationCode = createAuthenticationCode();

        if (member == null) { // 신규 가입
            EmailValidationCode emailValidationCode = emailValidationCodeRepository.findByEmail(email);

            if (emailValidationCode == null) {
                // 처음 발급
                emailValidationCodeRepository.save(
                        EmailValidationCode.builder()
                                .email(email)
                                .validationCode(newValidationCode)
                                .build()
                );
            } else {
                Instant now = Instant.now();

                // 1) 아직 만료 안됨 → 쿨타임 체크 후 제한
                if (emailValidationCode.getExpiresAt().isAfter(now)) {
                    if (emailValidationCode.getCreatedAt().isAfter(now.minus(1, ChronoUnit.MINUTES))) { // 1분 이내엔 재발송 안됨.
                        throw new BusinessException(ErrorCode.VALIDATION_CODE_REQUEST_TOO_SOON); // 429
                    }
                }

                // 2) 만료 or 쿨타임 경과 → 재발급
                emailValidationCode.regenerateCode(newValidationCode);
            }

        } else if (member.getDeletedAt() != null) { // 재가입
            EmailValidationCode emailValidationCode = emailValidationCodeRepository.findByEmail(email);
            if (emailValidationCode == null) {
                emailValidationCodeRepository.save(
                        EmailValidationCode.builder()
                                .email(email)
                                .validationCode(newValidationCode)
                                .build()
                );
            } else {
                emailValidationCode.regenerateCode(newValidationCode);
            }

        } else { // 이미 가입된 유저
            throw new BusinessException(ErrorCode.MEMBER_ALREADY_REGISTERED);
        }
    }

    // 랜덤 인증번호 생성 함수
    public String createAuthenticationCode() {
        // 12자리, 문자, 숫자 포함 문자열 생성
        return RandomStringUtils.random(12, true, true);
    }

    // 인증코드 검증
    @Transactional(readOnly = true)
    public void verifyEmailCode(String email, String code) {
        // 1. 인증코드 조회
        EmailValidationCode emailValidationCode = emailValidationCodeRepository.findByEmail(email);

        // 2. 만료 여부 확인
        if (emailValidationCode.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException(ErrorCode.VALIDATION_CODE_EXPIRED); // 400
        }

        // 3. 코드 일치 여부 확인
        if (!emailValidationCode.getValidationCode().equals(code)) {
            throw new BusinessException(ErrorCode.VALIDATION_CODE_MISMATCH); // 404
        }

        // 4. 이미 가입된 유저 확인
        memberRepository.findByEmail(email)
                .filter(m -> m.getDeletedAt() == null)
                .ifPresent(m -> {
                    throw new BusinessException(ErrorCode.MEMBER_ALREADY_REGISTERED); // 409
                });

        // 5. (필요 시 인증 완료 처리 로직 추가 - 예: 상태 플래그 변경)
    }

    public Member getMemberByEmail(String email) {
        return memberMapper.findByEmail(email)
                .orElseThrow(() -> new BusinessException("가입된 유저가 압니다.", ErrorCode.MEMBER_NOT_FOUND));
    }
}
