package com.app.WsCancelations.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MailService {
    @Value("${app.mail.from}")
    private String from;


    public void sendSimpleMail(String[] to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);

        try {
            mailSender.send(message);
            log.info(" Se ha enviado un email simple exitosamente a {}", (Object) to);
        } catch (Exception e) {
            log.error("Error al intentar enviar un email simple: {}", e.getMessage(), e);
        }
    }
}
