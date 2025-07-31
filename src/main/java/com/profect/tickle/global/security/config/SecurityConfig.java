package com.profect.tickle.global.security.config;

import com.profect.tickle.domain.member.entity.MemberRole;
import com.profect.tickle.domain.member.service.MemberService;
import com.profect.tickle.global.security.filter.CustomAuthenticationFilter;
import com.profect.tickle.global.security.filter.JwtFilter;
import com.profect.tickle.global.security.handler.JwtAccessDeniedHandler;
import com.profect.tickle.global.security.handler.JwtAuthenticationEntryPoint;
import com.profect.tickle.global.security.handler.SignInFailureHandler;
import com.profect.tickle.global.security.handler.SignInSuccessHandler;
import com.profect.tickle.global.security.util.JwtUtil;
import com.profect.tickle.global.security.util.properties.TokenProperties;
import jakarta.servlet.Filter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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

@Configuration
@EnableWebSecurity // 해당 클래스에서 시큐리티에 관한 설정을 할 것이다.
@RequiredArgsConstructor
public class SecurityConfig { // 주의: 클래스를 상속받아 시큐리티를 구현하는 방식은 구버전의 방식이다.

    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final MemberService memberService;
    private final TokenProperties tokenProperties;
    private final JwtUtil jwtUtil;

    @Bean
    protected SecurityFilterChain configure(HttpSecurity http) throws Exception {
        http
                // CSRF 토큰 발급 비활성화
                .csrf(AbstractHttpConfigurer::disable)

                // 요청별 접근 권한 설정
                .authorizeHttpRequests(auth ->
                        auth
                                // Swagger 문서: 인증 없이 접근 허용
                                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                                // 회원가입, 인증 관련 API: 인증 없이 접근 허용
                                .requestMatchers(HttpMethod.POST, "/api/v1/signUp", "/api/v1/auth/**").permitAll()

                                // 로그인 API: 인증 없이 접근 허용
                                .requestMatchers(HttpMethod.POST, "/api/v1/signIn").permitAll()

                                // 공연 조회: 인증 없이 접근 허용
                                .requestMatchers(HttpMethod.GET, "/api/v1/performance/**").permitAll()

                                // 이벤트 조회: 인증 없이 접근 허용
                                .requestMatchers(HttpMethod.GET, "/api/v1/event/**").permitAll()

                                // 이벤트(할인쿠폰 발급): 관리자 권한 필요
                                .requestMatchers(HttpMethod.POST, "/api/v1/event/coupon").hasRole(MemberRole.ADMIN.name())

                                // 나머지 모든 요청: 인증 필요
                                .anyRequest().authenticated()
                )

                // 세션 사용 안함 (STATELESS → JWT 방식)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // JWT 필터 추가: UsernamePasswordAuthenticationFilter 실행 전에 수행
                .addFilterBefore(new JwtFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class)

                // 커스텀 로그인 필터 추가: 기존 UsernamePasswordAuthenticationFilter 실행 전에 수행
                .addFilterBefore(getAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)

                // 인증, 인가 실패 시 핸들러 설정
                .exceptionHandling(exceptionHandling ->
                        exceptionHandling
                                .accessDeniedHandler(new JwtAccessDeniedHandler())            // 권한 부족(403) 처리
                                .authenticationEntryPoint(new JwtAuthenticationEntryPoint()) // 인증 실패(401) 처리
                );

        return http.build();
    }

    private Filter getAuthenticationFilter() {
        CustomAuthenticationFilter customAuthenticationFilter = new CustomAuthenticationFilter();
        customAuthenticationFilter.setAuthenticationManager(getAuthenticationManager());
        customAuthenticationFilter.setAuthenticationSuccessHandler(new SignInSuccessHandler(tokenProperties));
        customAuthenticationFilter.setAuthenticationFailureHandler(new SignInFailureHandler());

        return customAuthenticationFilter;
    }

    private AuthenticationManager getAuthenticationManager() {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
        daoAuthenticationProvider.setPasswordEncoder(bCryptPasswordEncoder);
        daoAuthenticationProvider.setUserDetailsService(memberService);

        return new ProviderManager(daoAuthenticationProvider);
    }
}
