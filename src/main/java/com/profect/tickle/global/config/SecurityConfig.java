package com.profect.tickle.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

용uestMatcher;

@Configuration
@EnableWebSecurity // 해당 클래스에서 시큐리티에 관한 설정을 할 것이다.
public class SecurityConfig { // 주의: 클래스를 상속받아 시큐리티를 구현하는 방식은 구버전의 방식이다.

    @Bean
    protected SecurityFilterChain configure(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable()) // CSRF 토큰 발행 시 클라이언트에서 매번 해당 토큰도 함께 요청에 넘겨 주어야 하므로 기능 비활성화
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(new AntPathRequestMatcher("/api/v1/**")).permitAll().anyRequest().authenticated(); // 모든 요청 허
                })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)); // session 로그인 방식을 사용하지 않음(Jwt 토큰 방식을 사용)

        return http.build();
    }
}
