package com.profect.tickle.domain.notification.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender javaMailSender;

    @Async
    public void sendSimpleMailMessage(String email, String title, String content) {
        log.info("{} 주소로 메일 전송", email);
        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();

        try {
            simpleMailMessage.setTo(email);
            simpleMailMessage.setSubject(title);
            simpleMailMessage.setText(content);

            javaMailSender.send(simpleMailMessage);
            log.info("메일 발송 성공");
        } catch (Exception e) {
            log.error("메일 발송 실패");
            log.error(e.getMessage());
            throw new RuntimeException("메일 발송 실패", e);
        }
    }

    @Async
    public void sendMimeMessage(String email, String title) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();

        try {
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, false, "UTF-8");

            // 메일을 받을 수신자 설정
            mimeMessageHelper.setTo(email);
            // 메일의 제목 설정
            mimeMessageHelper.setSubject(title);

            // html 문법 적용한 메일의 내용
            String content = """
                    <!DOCTYPE html>
                    <html xmlns:th="http://www.thymeleaf.org">
                                        
                    <body>
                    <div style="margin:100px;">
                        <h1> 테스트 메일 </h1>
                        <br>
                                        
                                        
                        <div align="center" style="border:1px solid black;">
                            <h3> 테스트 메일 내용 </h3>
                        </div>
                        <br/>
                    </div>
                                        
                    </body>
                    </html>
                    """;

            // 메일의 내용 설정
            mimeMessageHelper.setText(content, true);

            javaMailSender.send(mimeMessage);

            log.info("메일 발송 성공");
        } catch (Exception e) {
            log.info("메일 발송 실패");
            throw new RuntimeException(e);
        }
    }

}