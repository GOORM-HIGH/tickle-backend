package com.profect.tickle.global.security.util;

import com.profect.tickle.global.security.util.principal.CustomUserDetails;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class SecurityUtil {

    /**
     * 현재 인증 객체(Authentication) 반환
     */
    private static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * 현재 로그인한 사용자(CustomUserDetails) 반환
     * - 인증되지 않은 경우 AccessDeniedException 발생
     */
    public static CustomUserDetails getSignInMember() {
        Authentication authentication = getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails customUserDetails)) {
            throw new AccessDeniedException("인증 정보가 없습니다. 로그인이 필요합니다.");
        }

        return customUserDetails;
    }

    /**
     * 현재 로그인한 사용자의 UserDetails(Optional)
     */
    public static Optional<UserDetails> getSignInMemberDetails() {
        Authentication authentication = getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof UserDetails userDetails) {
            return Optional.of(userDetails);
        }

        return Optional.empty();
    }

    /**
     * 현재 로그인한 사용자의 권한 목록을 쉼표로 연결한 문자열 반환
     */
    public static String getSignInMemberAuthorities() {
        return getSignInMemberDetails()
                .map(userDetails -> userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.joining(",")))
                .orElse("");
    }

    /**
     * 현재 로그인한 사용자의 PK(Seq) 반환
     */
    public static Long getSignInMemberId() {
        return getSignInMember().getId();
    }

    /**
     * 현재 로그인한 사용자의 ID(이메일) 반환
     */
    public static String getSignInMemberEmail() {
        return getSignInMember().getUsername();
    }
}
