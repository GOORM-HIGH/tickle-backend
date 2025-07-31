package com.profect.tickle.global.security.handler;

import io.jsonwebtoken.Jwts;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;

@Slf4j
public class SignInSuccessHandler implements AuthenticationSuccessHandler {
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        log.info("{}님 로그인 성공하였습니다.",authentication.getName() );

//        String token = Jwts.builder()
//                .setClaims() // 바디
//                .setExpiration() // 만료시간
//                .signWith() // 서명
//                .compact();
    }
}
