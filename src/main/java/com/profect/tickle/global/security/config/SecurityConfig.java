package com.profect.tickle.global.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.profect.tickle.domain.member.repository.MemberRepository;
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
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final MemberService memberService;
    private final TokenProperties tokenProperties;
    private final JwtUtil jwtUtil;
    private final MemberRepository memberRepository; // 🎯 추가
    private final ObjectMapper objectMapper; // 🎯 추가

    @Bean
    @Profile("prod")
    public CorsConfigurationSource prodCorsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(Arrays.asList("https://tickle.kr", "https://www.tickle.kr"));
        cfg.setAllowedMethods(Arrays.asList("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        cfg.setAllowedHeaders(Arrays.asList("*"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        // 프리플라이트까지 확실히 잡히도록 /** 로 등록
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }

    @Bean
    @Profile("local")
    public CorsConfigurationSource localCorsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(Arrays.asList("http://localhost:5173","http://localhost:3000","http://localhost:8081"));
        cfg.setAllowedMethods(Arrays.asList("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        cfg.setAllowedHeaders(Arrays.asList("*"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }


    @Bean
    protected SecurityFilterChain configure(HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                .cors(c -> c.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth ->
                        auth
                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                .requestMatchers("/swagger-ui/**",
                                        "/swagger-resources/**",
                                        "/v3/api-docs/**",
                                        "/webjars/**",
                                        "/api-docs/**",
                                        "/ws/**").permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/v1/sign-up", "/api/v1/auth/**").permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/v1/sign-in").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/v1/performance/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/v1/event/**").permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/v1/event/coupon").hasAuthority("ADMIN")
                                .anyRequest().authenticated()
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(new JwtFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(getAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exceptionHandling ->
                        exceptionHandling
                                .accessDeniedHandler(new JwtAccessDeniedHandler())
                                .authenticationEntryPoint(new JwtAuthenticationEntryPoint())
                );

        return http.build();
    }

    private Filter getAuthenticationFilter() {
        CustomAuthenticationFilter customAuthenticationFilter = new CustomAuthenticationFilter();
        customAuthenticationFilter.setAuthenticationManager(getAuthenticationManager());
        // 🎯 수정: 필요한 의존성들을 주입하여 SignInSuccessHandler 생성
        customAuthenticationFilter.setAuthenticationSuccessHandler(new SignInSuccessHandler(tokenProperties, memberRepository, objectMapper));
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
