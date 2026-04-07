package com.example.chatApp.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    public void sendOtpEmail(String to, String otp, String purpose) {
        try {
            Context context = new Context();
            context.setVariable("otp", otp);
            context.setVariable("purpose", purpose);
            context.setVariable("appName", "LiveChat");

            String htmlContent = templateEngine.process("otp-email", context);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("LiveChat - Your OTP Code for " + purpose);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("OTP email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send OTP email to: {}. Error: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send OTP email. Please try again later.", e);
        }
    }
}
