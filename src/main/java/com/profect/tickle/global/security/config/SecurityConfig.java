package com.profect.tickle.global.config;

import com.profect.tickle.domain.member.service.MemberService;
import com.profect.tickle.global.security.CustomAuthenticationFilter;
import jakarta.servlet.Filter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity // 해당 클래스에서 시큐리티에 관한 설정을 할 것이다.
@RequiredArgsConstructor
public class SecurityConfig { // 주의: 클래스를 상속받아 시큐리티를 구현하는 방식은 구버전의 방식이다.

    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final MemberService memberService;

    @Bean
    protected SecurityFilterChain configure(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // CSRF 토큰 발행 시 클라이언트에서 매번 해당 토큰도 함께 요청에 넘겨 주어야 하므로 기능 비활성화
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(new AntPathRequestMatcher("/api/v1/**")).permitAll().anyRequest().authenticated(); // 모든 요청 허
                })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)); // session 로그인 방식을 사용하지 않음(Jwt 토큰 방식을 사용)

        // 커스텀 로그인 필터 추가: 기존 인증필터 이전에 작동
        http.addFilterBefore(getAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private Filter getAuthenticationFilter() {
        CustomAuthenticationFilter customAuthenticationFilter = new CustomAuthenticationFilter();
        customAuthenticationFilter.setAuthenticationManager(getAuthenticationManager());
        customAuthenticationFilter.setAuthenticationSuccessHandler(new SignInSuccessHandler());

        return customAuthenticationFilter;
    }

    private AuthenticationManager getAuthenticationManager() {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
        daoAuthenticationProvider.setPasswordEncoder(bCryptPasswordEncoder);
        daoAuthenticationProvider.setUserDetailsService(memberService);

        return new ProviderManager(daoAuthenticationProvider);
    }
}
