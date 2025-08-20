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
import com.profect.tickle.domain.notification.entity.NotificationTemplate;
import com.profect.tickle.domain.notification.entity.NotificationTemplateId;
import com.profect.tickle.domain.notification.event.member.event.EmailAuthenticationCodePublishEvent;
import com.profect.tickle.domain.notification.service.NotificationTemplateService;
import com.profect.tickle.domain.notification.service.mail.MailSender;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
    private final MailSender mailSender;
    private final ApplicationEventPublisher eventPublisher;

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

        // 3) 수수료 유효 범위(있는 경우만). 음수 또는 기타 범위를 막고 싶으면 이곳에서
        if (request.hostContractCharge() != null
                && request.hostContractCharge().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.CONTRACT_CHARGE_INVALID);
        }

        // 4) 엔티티 생성 + 비밀번호 암호화 + 기본값 보정
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
    public void createEmailAuthenticationCode(String email) {
        // 1. 이미 가입된 회원인지 확인
        log.info("이미 가입된 회원인지 확인");
        Member member = memberRepository.findByEmail(email).orElse(null);
        if (member != null && member.getDeletedAt() == null) {
            throw new BusinessException(ErrorCode.MEMBER_ALREADY_REGISTERED);
        }

        // 2. 인증번호 생성
        log.info("새로운 인증번호 생성");
        String newAuthenticationCode = createAuthenticationCode();

        // 3. 기존 인증코드 확인
        EmailAuthenticationCode emailAuthencationCode = emailAuthenticationRepository.findByEmail(email)
                .orElse(null);

        if (emailAuthencationCode != null) {
            // 쿨타임 체크: 최근 생성 1분 이내 요청이면 차단
            if (emailAuthencationCode.getCreatedAt().isAfter(Instant.now().minus(1, ChronoUnit.MINUTES))) {
                log.info("최신의 인증코드가 존재합니다.");
                throw new BusinessException(ErrorCode.VALIDATION_CODE_REQUEST_TOO_SOON);
            }
            log.info("인증코드 갱신");
            emailAuthencationCode.regenerateCode(newAuthenticationCode);
        } else {
            emailAuthencationCode = EmailAuthenticationCode.builder()
                    .email(email)
                    .validationCode(newAuthenticationCode)
                    .build();
        }

        log.info("인증코드 DB 저장");
        emailAuthenticationRepository.save(emailAuthencationCode);

        // 4. 메일 발송
        log.info("인증코드 이메일 발송 준비");
        NotificationTemplate template = notificationTemplateService.getNotificationTemplateById(NotificationTemplateId.AUTH_CODE_SENT.getId());
        String title = template.getTitle();
        String content = String.format(template.getContent(), newAuthenticationCode);

        MailCreateServiceRequestDto req = new MailCreateServiceRequestDto(email, title, content);
        eventPublisher.publishEvent(new EmailAuthenticationCodePublishEvent(req));
        log.info("인증코드 메일 발송 이벤트 발행 완료: {}", email);
    }

    // 랜덤 인증번호 생성 함수
    public String createAuthenticationCode() {
        // 12자리, 문자, 숫자 포함 문자열 생성
        return RandomStringUtils.random(12, true, true);
    }

    // 인증코드 검증
    public void verifyEmailCode(String email, String code) {
        // 1. 인증코드 조회
        EmailAuthenticationCode emailAuthenticationCode = emailAuthenticationRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.MEMBER_NOT_FOUND.getMessage(),
                        ErrorCode.MEMBER_NOT_FOUND
                ));

        // 2. 만료 여부 확인
        if (emailAuthenticationCode.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException(ErrorCode.VALIDATION_CODE_EXPIRED); // 400
        }

        // 3. 코드 일치 여부 확인
        if (!emailAuthenticationCode.getValidationCode().equals(code)) {
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
        } else { // 유저인 경우
            return memberMapper.getMemberDtoByEmail(email)
                    .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND.getMessage(), ErrorCode.MEMBER_NOT_FOUND));
        }
    }

    @Transactional
    public void deleteUser(Long memberId, String signInMemberEmail) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND.getMessage(), ErrorCode.MEMBER_NOT_FOUND));

        // 이미 탈퇴된 유저
        if (member.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND.getMessage(), ErrorCode.MEMBER_NOT_FOUND);
        }

        // 권한 확인
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
        if (request.getNickname() != null && !request.getNickname().trim().isEmpty()) {
            String nickname = request.getNickname().trim();
            member.updateNickname(nickname);
        }

        // 프로필사진 변경
        if (request.getImg() != null && !request.getImg().trim().isEmpty()) {
            String img = request.getImg().trim();
            member.updateImg(img);
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

        return;
    }

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

        // ✅ 수수료율 화이트리스트 검사
        if (!hostChargePolicy.isAllowed(d.hostContractCharge())) {
            throw new BusinessException(
                    "허용되지 않는 수수료율입니다. " + hostChargePolicy.message(),
                    ErrorCode.CONTRACT_CHARGE_INVALID
            );
        }
    }

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

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
