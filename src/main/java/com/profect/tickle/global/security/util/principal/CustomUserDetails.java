package com.profect.tickle.global.security.util.principal;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Long id;    // DB PK
    private final String email;  // 로그인용 이메일
    private final String password; // DB에 저장된 암호화된 비밀번호
    private final String nickname; // 사용자 닉네임
    private final Collection<? extends GrantedAuthority> authorities; // 권한 리스트

    public CustomUserDetails(Long id, String email, String password, String nickname,
                             Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.authorities = authorities;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    /**
     * DB에 저장된 암호화된 비밀번호를 반환
     * (로그인 시 DaoAuthenticationProvider에서 비밀번호 검증에 사용)
     */
    @Override
    public String getPassword() {
        return this.password;
    }

    /**
     * Spring Security에서 username은 고유 식별자. 여기서는 이메일을 username으로 사용.
     */
    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
