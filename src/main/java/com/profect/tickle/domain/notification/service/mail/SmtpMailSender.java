package com.profect.tickle.domain.notification.service.mail;

import com.profect.tickle.domain.notification.dto.request.MailCreateServiceRequestDto;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmtpMailSender implements MailSender {

    private final JavaMailSender javaMailSender;

    @Async(value = "mailExecutor")
    @Override
    public void sendText(MailCreateServiceRequestDto request) {
        log.info("{} 주소로 메일 전송", request.to());
        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();

        try {
            simpleMailMessage.setTo(request.to());
            simpleMailMessage.setSubject(request.subject());
            simpleMailMessage.setText(request.content());

            javaMailSender.send(simpleMailMessage);
            log.info("메일 발송 성공");
        } catch (Exception e) {
            log.error("메일 발송 실패");
            log.error(e.getMessage());
            throw new RuntimeException("메일 발송 실패", e);
        }
    }

    @Async(value = "mailExecutor")
    @Override
    public void sendHtml(MailCreateServiceRequestDto request) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();

        try {
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, false, "UTF-8");

            // 메일을 받을 수신자 설정
            mimeMessageHelper.setTo(request.to());
            // 메일의 제목 설정
            mimeMessageHelper.setSubject(request.subject());

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