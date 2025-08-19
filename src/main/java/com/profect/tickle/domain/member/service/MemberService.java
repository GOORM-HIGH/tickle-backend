package com.profect.tickle.domain.member.service;

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
import com.profect.tickle.domain.notification.entity.NotificationTemplate;
import com.profect.tickle.domain.notification.entity.NotificationTemplateId;
import com.profect.tickle.domain.notification.service.MailService;
import com.profect.tickle.domain.notification.service.NotificationTemplateService;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.security.util.SecurityUtil;
import com.profect.tickle.global.security.util.principal.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
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
public class MemberService implements UserDetailsService {

    private final PasswordEncoder passwordEncoder;

    private final MailService mailService;
    private final NotificationTemplateService notificationTemplateService;
    private final ContractService contractService;

    private final MemberMapper memberMapper;
    private final MemberRepository memberRepository;
    private final EmailAuthenticationCodeRepository emailAuthenticationRepository;


    @Transactional
    public void createMember(CreateMemberServiceRequestDto createUserRequest) {
        // 1. 신규 회원 생성
        Member newMember = Member.createMember(createUserRequest);
        newMember.encryptPassword(passwordEncoder.encode(createUserRequest.password()));

        // 2. 신규 회원 저장
        memberRepository.save(newMember);

        // 3. 저장된 회원 조회
        Member member = memberRepository.findByEmail(newMember.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // 4. 신규 계약 생성
        if (createUserRequest.hostContractCharge() == null || createUserRequest.hostContractCharge().compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        contractService.createContract(member, createUserRequest.hostContractCharge());
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
        mailService.sendSimpleMailMessage(email, title, content);
        log.info("인증코드 발송 완료");
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
}
