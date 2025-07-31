package com.profect.tickle.global.security.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.util.Base64;

@Slf4j
public class HMACKeyGenerator {

    public static void main(String[] args) {
        try {
            // HS512를 위한 KeyGenerator 생성
            KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacSHA512");
            keyGenerator.init(512); // 512비트 키 사이즈 설정

            // 비밀키 생성
            SecretKey secretKey = keyGenerator.generateKey();

            // 키를 Base64로 인코딩하여 문자열로 변환
            String encodedKey = Base64.getEncoder().encodeToString(secretKey.getEncoded());

            log.info("HS512 Key: {}", encodedKey);
        } catch (Exception e) {
            log.error("HS512 키 생성 중 오류 발생", e);
        }
    }
}
