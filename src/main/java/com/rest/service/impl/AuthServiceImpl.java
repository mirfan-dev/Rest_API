package com.rest.service.impl;

import com.rest.entity.Customer;
import com.rest.exception.ResourceNotFoundException;
import com.rest.repository.CustomerRepository;
import com.rest.service.AuthService;
import com.rest.service.EmailService;
import com.rest.util.OtpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final EmailService emailService;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void sendOtp(String email) {

        Customer user = customerRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found with this email " + email));

        String otp = OtpUtil.generateOtp();
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(15);

        user.setOtp(otp);
        user.setOtpExpiredAt(expiryTime);
        customerRepository.save(user);

        try {
            emailService.sendOtp(user.getEmail(), otp);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to send OTP");
        }
    }

    @Override
    public void verifyOtp(String email, String otp) {
        Customer user = customerRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with this email " + email));

        if (user.getOtp() == null || !user.getOtp().equals(otp)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OTP");
        }

        if (user.getOtpExpiredAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP expired");
        }


        user.setOtp(null);
        user.setOtpExpiredAt(null);

        customerRepository.save(user);
    }



        @Override
        public void sendResetOtp(String email) {
            Customer user = customerRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with this email " + email));

            String otp = OtpUtil.generateOtp();
            LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(15);

            user.setResetOtp(otp);
            user.setResetOtpExpiredAt(expiryTime);
            customerRepository.save(user);

            try {
                emailService.sendResetOtp(user.getEmail(), otp);
            } catch (Exception ex) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to send OTP");
            }
        }


@Override
public void resetPassword(String email, String otp, String newPassword) {
    Customer user = customerRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with this email " + email));

    if (user.getResetOtp() == null || !user.getResetOtp().equals(otp)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OTP");
    }

    if (user.getResetOtpExpiredAt().isBefore(LocalDateTime.now())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP expired");
    }

    user.setPassword(passwordEncoder.encode(newPassword));
    user.setResetOtp(null);
    user.setResetOtpExpiredAt(null);

    customerRepository.save(user);
}
}