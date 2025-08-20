package com.profect.tickle.testsecurity;

import com.profect.tickle.global.security.util.principal.CustomUserDetails;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.Arrays;
import java.util.Collection;

public class WithMockMemberSecurityContextFactory implements WithSecurityContextFactory<WithMockMember> {

    @Override
    public SecurityContext createSecurityContext(WithMockMember a) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        // ✅ getAuthorities()의 시그니처에 정확히 맞춰서 타입 선언
        Collection<? extends GrantedAuthority> authorities = Arrays.stream(a.roles())
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                .map(SimpleGrantedAuthority::new)
                .toList();

        CustomUserDetails principal = Mockito.mock(CustomUserDetails.class);
        Mockito.when(principal.getId()).thenReturn(a.id());
        Mockito.when(principal.getUsername()).thenReturn(a.email());

        // ✅ 1) thenReturn에 딱 맞는 타입으로 전달
        // Mockito.when(principal.getAuthorities()).thenReturn(authorities);

        // ✅ 2) 또는 doReturn(..).when(..) 패턴 사용 (제네릭 추론 이슈 회피)
        Mockito.doReturn(authorities).when(principal).getAuthorities();

        Mockito.when(principal.getPassword()).thenReturn("N/A");
        Mockito.when(principal.isAccountNonExpired()).thenReturn(true);
        Mockito.when(principal.isAccountNonLocked()).thenReturn(true);
        Mockito.when(principal.isCredentialsNonExpired()).thenReturn(true);
        Mockito.when(principal.isEnabled()).thenReturn(true);

        var auth = new UsernamePasswordAuthenticationToken(principal, "N/A", authorities);
        context.setAuthentication(auth);
        return context;
    }
}
