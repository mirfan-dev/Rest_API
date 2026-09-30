package com.rest.service;

public interface AuthService {


    // Send OTP for login or verification
    void sendOtp(String email);

    public void verifyOtp(String email, String otp);

    // Send OTP for password reset
    void sendResetOtp(String email);

    // Reset password using OTP
    void resetPassword(String email, String otp, String newPassword);
}
