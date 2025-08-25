package com.profect.tickle.domain.member.service;

import com.profect.tickle.domain.contract.policy.HostChargePolicy;
import com.profect.tickle.domain.contract.service.ContractService;
import com.profect.tickle.domain.member.dto.request.CreateMemberServiceRequestDto;
import com.profect.tickle.domain.member.dto.request.UpdateMemberRequestDto;
import com.profect.tickle.domain.member.dto.response.MemberResponseDto;
import com.profect.tickle.domain.member.entity.EmailAuthenticationCode;
import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.entity.MemberRole;
import com.profect.tickle.domain.member.mapper.MemberMapper;
import com.profect.tickle.domain.member.repository.EmailAuthenticationCodeRepository;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.domain.notification.dto.request.MailCreateServiceRequestDto;
import com.profect.tickle.domain.notification.entity.NotificationKind;
import com.profect.tickle.domain.notification.entity.NotificationTemplate;
import com.profect.tickle.domain.notification.event.member.event.EmailAuthenticationCodePublishEvent;
import com.profect.tickle.domain.notification.service.NotificationTemplateService;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.security.util.SecurityUtil;
import com.profect.tickle.global.security.util.principal.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MemberService implements UserDetailsService {

    // utils
    private final PasswordEncoder passwordEncoder;
    private final HostChargePolicy hostChargePolicy;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    // services
    private final NotificationTemplateService notificationTemplateService;
    private final ContractService contractService;

    // mappers & repositories
    private final MemberMapper memberMapper;
    private final MemberRepository memberRepository;
    private final EmailAuthenticationCodeRepository emailAuthenticationRepository;

    @Transactional
    public void createMember(CreateMemberServiceRequestDto request) {
        // 1) 이메일 중복
        if (memberRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.MEMBER_ALREADY_REGISTERED);
        }

        // 2) 역할/필드 일관성
        if (request.role() == MemberRole.HOST) {
            requireHostFields(request); // HOST면 필수 사업자 정보 모두 필요
        } else { // MEMBER or ADMIN
            if (anyHostFieldFilled(request)) {
                throw new BusinessException(
                        "MEMBER/ADMIN 권한에서는 HOST 전용 필드를 보낼 수 없습니다.",
                        ErrorCode.INVALID_INPUT_VALUE
                );
            }
        }

        // 3) 수수료 유효 범위(있는 경우만)
        if (request.hostContractCharge() != null
                && request.hostContractCharge().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.CONTRACT_CHARGE_INVALID);
        }

        // 4) 엔티티 생성 + 비밀번호 암호화
        Member member = Member.createMember(request);
        member.encryptPassword(passwordEncoder.encode(request.password()));

        // 5) 저장
        Member saved = memberRepository.save(member);

        // 6) HOST 계약 생성 (수수료가 0보다 클 때만 생성)
        if (request.role() == MemberRole.HOST
                && request.hostContractCharge() != null
                && request.hostContractCharge().compareTo(BigDecimal.ZERO) > 0) {
            contractService.createContract(saved, request.hostContractCharge());
        }
    }

    // 로그인 요청 시 AuthenticationManager를 통해 호출
    @Override
    public UserDetails loadUserByUsername(String inputEmail) throws UsernameNotFoundException {
        Member signInMember = memberRepository.findByEmail(inputEmail)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 계정입니다."));

        List<GrantedAuthority> grantedAuthorityList = new ArrayList<>();
        grantedAuthorityList.add(new SimpleGrantedAuthority(signInMember.getMemberRole().name()));

        return new CustomUserDetails(
                signInMember.getId(),
                signInMember.getEmail(),
                signInMember.getPassword(),
                signInMember.getNickname(),
                grantedAuthorityList
        );
    }

    @Transactional
    public void createEmailAuthenticationCode(String email) {
        // 1) 이미 가입된 회원인지 확인(soft-deleted 제외)
        assertNotActiveMember(email);

        // 2) 새 인증코드 생성
        String newCode = createAuthenticationCode();

        // 3) 기존 인증코드 존재 여부에 따라 처리 (쿨타임/시간 계산은 엔티티에 위임)
        EmailAuthenticationCode emailAuthenticationCode = emailAuthenticationRepository.findByEmail(email).orElse(null);

        if (emailAuthenticationCode != null) {
            // 엔티티에게 쿨타임 확인
            emailAuthenticationCode.assertResendAllowed(clock);
            emailAuthenticationCode.regenerateCode(newCode, clock);
            log.info("인증코드 갱신");
        } else {
            emailAuthenticationCode = EmailAuthenticationCode.issue(email, newCode, clock);
            log.info("새 인증코드 발급");
        }

        // 4) 저장
        emailAuthenticationRepository.save(emailAuthenticationCode);
        log.info("인증코드 DB 저장 완료");

        // 5) 메일 발송 이벤트
        MailCreateServiceRequestDto mailReq = buildAuthMail(email, newCode);
        eventPublisher.publishEvent(new EmailAuthenticationCodePublishEvent(mailReq));
        log.info("인증코드 메일 발송 이벤트 발행 완료: {}", email);
    }

    // 인증코드 검증
    public void verifyEmailCode(String email, String code) {
        // 1) 인증코드 조회
        EmailAuthenticationCode entity = emailAuthenticationRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.MEMBER_NOT_FOUND.getMessage(),
                        ErrorCode.MEMBER_NOT_FOUND
                ));

        // 2) 만료 여부
        if (entity.isExpired(clock)) {
            throw new BusinessException(ErrorCode.VALIDATION_CODE_EXPIRED);
        }

        // 3) 코드 일치 여부
        if (!entity.getValidationCode().equals(code)) {
            throw new BusinessException(ErrorCode.VALIDATION_CODE_MISMATCH);
        }

        // 4) 이미 가입된 유저는 중복 가입 불가
        memberRepository.findByEmail(email)
                .filter(m -> m.getDeletedAt() == null)
                .ifPresent(m -> {
                    throw new BusinessException(ErrorCode.MEMBER_ALREADY_REGISTERED);
                });
    }

    public Member getMemberByEmail(String email) {
        return memberMapper.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND.getMessage(), ErrorCode.MEMBER_NOT_FOUND));
    }

    // 로그인한 유저의 이메일로 유저를 조회하여 정보 데이터를 반환
    public MemberResponseDto getMemberDtoByEmail(String email) {
        MemberRole memberRole = memberMapper.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND.getMessage(), ErrorCode.MEMBER_NOT_FOUND))
                .getMemberRole();

        if (memberRole == MemberRole.HOST) {
            return memberMapper.getHostMemberDtoByEmail(email)
                    .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND.getMessage(), ErrorCode.MEMBER_NOT_FOUND));
        } else {
            return memberMapper.getMemberDtoByEmail(email)
                    .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND.getMessage(), ErrorCode.MEMBER_NOT_FOUND));
        }
    }

    @Transactional
    public void deleteUser(Long memberId, String signInMemberEmail) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND.getMessage(), ErrorCode.MEMBER_NOT_FOUND));

        if (member.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND.getMessage(), ErrorCode.MEMBER_NOT_FOUND);
        }

        if (member.getEmail().equals(signInMemberEmail)) {
            throw new BusinessException(ErrorCode.MEMBER_DELETE_FORBIDDEN.getMessage(), ErrorCode.MEMBER_DELETE_FORBIDDEN);
        }

        member.deleteMember();
    }

    // 맴버정보 업데이트 메서드
    @Transactional
    public void updateUser(String memberEmail, UpdateMemberRequestDto request) {
        String signInMemberEmail = SecurityUtil.getSignInMemberEmail();

        if (!memberEmail.equals(signInMemberEmail)) {
            throw new BusinessException(
                    ErrorCode.MEMBER_UPDATE_PERMISSION_DENIED.getMessage(),
                    ErrorCode.MEMBER_UPDATE_PERMISSION_DENIED
            );
        }

        Member member = memberRepository.findByEmail(memberEmail)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.MEMBER_NOT_FOUND.getMessage(),
                        ErrorCode.MEMBER_NOT_FOUND
                ));

        // 닉네임 변경
        if (hasText(request.getNickname())) {
            member.updateNickname(request.getNickname().trim());
        }

        // 프로필사진 변경
        if (hasText(request.getImg())) {
            member.updateImg(request.getImg().trim());
        }

        // 수수료 변경 (HOST만, 그리고 유효 범위 체크)
        if (request.getCharge() != null) {
            if (member.getMemberRole() != MemberRole.HOST) {
                throw new BusinessException(
                        ErrorCode.MEMBER_UPDATE_PERMISSION_DENIED.getMessage(),
                        ErrorCode.MEMBER_UPDATE_PERMISSION_DENIED
                );
            }

            BigDecimal charge = request.getCharge();

            // 허용 범위 예: 0% ~ 20%
            if (charge.compareTo(BigDecimal.ZERO) < 0 ||
                    charge.compareTo(new BigDecimal("20")) > 0) {
                throw new BusinessException(
                        ErrorCode.CONTRACT_CHARGE_INVALID.getMessage(),
                        ErrorCode.CONTRACT_CHARGE_INVALID
                );
            }

            contractService.updateContract(member.getId(), charge);
        }
    }

    // 이미 가입한 회원이지 확인하는 메서드
    private void assertNotActiveMember(String email) {
        Member member = memberRepository.findByEmail(email).orElse(null);
        if (member != null && member.getDeletedAt() == null) {
            throw new BusinessException(ErrorCode.MEMBER_ALREADY_REGISTERED);
        }
    }

    // 메일 생성 메서드
    private MailCreateServiceRequestDto buildAuthMail(String email, String code) {
        NotificationTemplate template =
                notificationTemplateService.getNotificationTemplateById(NotificationKind.AUTH_CODE_SENT.getId());
        String title = template.getTitle();
        String content = String.format(template.getContent(), code);
        return new MailCreateServiceRequestDto(email, title, content);
    }

    // 이메일인증코드 코드 생성 메서드 (12자리 영문/숫자 랜덤)
    public String createAuthenticationCode() {
        return RandomStringUtils.random(12, true, true);
    }

    // 사업자 필드 유효성 확인 메서드
    private void requireHostFields(CreateMemberServiceRequestDto d) {
        boolean hostFieldsOk =
                hasText(d.hostBizNumber()) &&
                        hasText(d.hostBizName()) &&
                        hasText(d.hostBizBankName()) &&
                        hasText(d.hostBizDepositor()) &&
                        hasText(d.hostBizBankNumber());

        if (!hostFieldsOk) {
            throw new BusinessException(
                    "HOST 권한일 때는 사업자 필드가 모두 필요합니다.",
                    ErrorCode.INVALID_INPUT_VALUE
            );
        }

        // 수수료율 화이트리스트 검사
        if (!hostChargePolicy.isAllowed(d.hostContractCharge())) {
            throw new BusinessException(
                    "허용되지 않는 수수료율입니다. " + hostChargePolicy.message(),
                    ErrorCode.CONTRACT_CHARGE_INVALID
            );
        }
    }

    // 사업자 필드 기입여부 확인 메서드
    private boolean anyHostFieldFilled(CreateMemberServiceRequestDto d) {
        return hasText(d.hostBizNumber()) ||
                hasText(d.hostBizName()) ||
                hasText(d.hostBizBankName()) ||
                hasText(d.hostBizDepositor()) ||
                hasText(d.hostBizBankNumber()) ||
                d.hostContractCharge() != null;
    }

    private boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
