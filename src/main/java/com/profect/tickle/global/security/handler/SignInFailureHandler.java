package com.profect.tickle.global.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.profect.tickle.global.security.dto.response.ExceptionResponseDto;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.io.IOException;

public class SignInFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); /* 선언하지 않으면, Spring Security에서 오류가 발생하므로 403 코드가 전달된다. */
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(new ObjectMapper().writeValueAsString(new ExceptionResponseDto(401, "로그인 실패")));
    }
}
