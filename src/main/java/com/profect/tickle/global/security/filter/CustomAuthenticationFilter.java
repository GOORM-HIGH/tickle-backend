package com.profect.tickle.global.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.profect.tickle.domain.member.dto.request.SignInRequestDto;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.io.IOException;
import java.util.ArrayList;

public class CustomAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    public CustomAuthenticationFilter() {
        super(new AntPathRequestMatcher("/api/v1/sign-in", "POST"));
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException, IOException, ServletException {

        // requestBody → DTO 변환
        SignInRequestDto credentials = new ObjectMapper()
                .readValue(request.getInputStream(), SignInRequestDto.class);

        // Authentication 객체 생성 후 AuthenticationManager로 위임
        return getAuthenticationManager().authenticate(
                new UsernamePasswordAuthenticationToken(
                        credentials.email(),
                        credentials.password(),
                        new ArrayList<>()
                )
        );
    }
}
