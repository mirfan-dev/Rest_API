package com.rest.service.impl;

import com.rest.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;


    @Override
    public void sendOtp(String email, String otp) {
        String subject = "Your Account Verification OTP";
        String message = "Dear customer,\n\nYour OTP for account verification is: " + otp +
                "\nThis OTP is valid for 15 minutes.\n\nRegards,\nYour Company";
        sendEmail(email, subject, message);
    }

    @Override
    public void sendResetOtp(String email, String otp) {
        String subject = "Your Password Reset OTP";
        String message = "Dear user,\n\nYour OTP for password reset is: " + otp +
                "\nThis OTP is valid for 15 minutes.\n\nIf you did not request a password reset, please ignore this email.\n\nRegards,\nYour Company";
        sendEmail(email, subject, message);
    }


    // Private helper method for sending email
    private void sendEmail(String toEmail, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
