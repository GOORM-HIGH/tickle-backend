package com.profect.tickle.global.security.util;

import com.profect.tickle.domain.member.service.MemberService;
import com.profect.tickle.global.security.util.properties.TokenProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;

@Slf4j
@Component
public class JwtUtil {

    private final Key key;
    private final MemberService smtpMailSender;

    public JwtUtil(
            TokenProperties tokenProperties,
            MemberService smtpMailSender
    ) {
        byte[] keyBytes = Decoders.BASE64.decode(tokenProperties.getSecretKey());
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.smtpMailSender = smtpMailSender;
    }

    /* Token 검증(Bearer 토큰이 넘어왔고, 우리 사이트의 secret key로 만들어 졌는가, 만료되었는지와 내용이 비어있진 않은지) */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.info("Invalid JWT Token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.info("Expired JWT Token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.info("Unsupported JWT Token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.info("JWT Token claims empty: {}", e.getMessage());
        }
        return false;
    }

    /* 넘어온 AccessToken으로 인증 객체 추출 */
    public Authentication getAuthentication(String token) {
        /* 토큰을 들고 왔던 들고 오지 않았던(로그인 시) 동일하게 security가 관리 할 UserDetails 타입을 정의 */
        UserDetails userDetails = smtpMailSender.loadUserByUsername(this.getEmail(token));

        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    /* Token에서 Claims 추출 */
    public Claims parseClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }

    /* Token에서 사용자의 이메일(subject 클레임) 추출 */
    public String getEmail(String token) {
        return parseClaims(token).getSubject();
    }

    /* Token에서 사용자의 ID(userId 클레임) 추출 */
    public Long getUserId(String token) {
        Claims claims = parseClaims(token);
        log.info("🎯 JWT 클레임 전체: {}", claims);
        log.info("🎯 JWT 클레임 키들: {}", claims.keySet());
        
        if (claims.containsKey("userId")) {
            Long userId = claims.get("userId", Long.class);
            log.info("🎯 JWT에서 userId 클레임 발견: {}", userId);
            return userId;
        } else {
            log.warn("🎯 JWT에 userId 클레임이 없습니다. 사용 가능한 클레임: {}", claims.keySet());
        }
        return null;
    }

    /* Token에서 사용자의 닉네임(nickname 클레임) 추출 */
    public String getNickname(String token) {
        Claims claims = parseClaims(token);
        if (claims.containsKey("nickname")) {
            return claims.get("nickname", String.class);
        }
        return null;
    }
}