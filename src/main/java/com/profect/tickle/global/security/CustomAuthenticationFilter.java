package com.profect.tickle.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.profect.tickle.global.security.request.SignInRequest;
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

    // 해당 요청이 올 때 이 필터가 작동하도록 설정한다.
    public CustomAuthenticationFilter() {
        super(new AntPathRequestMatcher("/api/v1/signIn", "POST"));
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, IOException, ServletException {
        // requestBody에 담긴 정보를 우리가 만든 signupRequest 타입에 담아준다.
        SignInRequest credentials = new ObjectMapper().readValue(request.getInputStream(), SignInRequest.class); // Controller의 @RequestBody 어노테이션을 통해 자동을 convert 되었던 부분을 filter에서 직접 처리하는 과정

        return getAuthenticationManager().authenticate(
                new UsernamePasswordAuthenticationToken(credentials.getEmail(), credentials.getPassword(), new ArrayList<>())
        );
    }
}
