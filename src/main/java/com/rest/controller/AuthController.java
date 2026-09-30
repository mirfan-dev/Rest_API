package com.rest.controller;


import com.rest.dtos.CustomerDtos;
import com.rest.helper.*;
import com.rest.repository.CustomerRepository;
import com.rest.security.JwtToken;
import com.rest.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {


    private final AuthenticationManager authenticationManager;

    private final UserDetailsService userDetailsService;

    private final ModelMapper modelMapper;

    private final JwtToken jwtToken;

    private final CustomerRepository userRepository;

    private final AuthService authService;


    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {

        try {

            //created authentication
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword());
            //authenticating
            authenticationManager.authenticate(authentication);

            //getting userdetail
            UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.getEmail());

            CustomerDtos userDto = modelMapper.map(userRepository.findByEmail(userDetails.getUsername()).get(), CustomerDtos.class);
            //getting token
            String token = jwtToken.generateToken(userDto.getEmail(), true);
            String refreshToken = jwtToken.generateToken(userDto.getEmail(), false);
            JwtResponse build = JwtResponse
                    .builder()
                    .accessToken(token)
                    .refreshToken(refreshToken)
                    .user(userDto)
                    .build();
            return ResponseEntity.ok(build);

        } catch (Exception ex) {
            // 6. Handle invalid credentials
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ApiResponse.builder()
                            .status(HttpStatus.BAD_REQUEST)
                            .errors(List.of("Invalid Username or Password"))
                            .success(false)
                            .timestamp(LocalDateTime.now())
                            .build()
            );
        }
    }


    //refresh token api

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(
            @RequestBody RefreshTokenRequest refreshTokenRequeest
    ) {


        //refresh token ko valildate
        if (jwtToken.validateToken(refreshTokenRequeest.getRefreshToken()) && jwtToken.isRefreshToken(refreshTokenRequeest.getRefreshToken())) {

            //refresh token se username nikal rehe
            String usernameFromRefreshToken = jwtToken.extractEmail(refreshTokenRequeest.getRefreshToken());
            //user data fetch kiya hia from database
            CustomerDtos userDto = modelMapper.map(userRepository.findByEmail(usernameFromRefreshToken).get(), CustomerDtos.class);

            //new access token generate kiya hai.
            String accessToken = jwtToken.generateToken(userDto.getEmail(), true);
            //new refresh token
            String newRefreshToken = jwtToken.generateToken(userDto.getEmail(), false);
            JwtResponse response = JwtResponse
                    .builder()
                    .accessToken(accessToken)
                    .refreshToken(newRefreshToken)
                    .user(userDto).build();

            return ResponseEntity.ok(response);

        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ApiResponse.builder()
                            .status(HttpStatus.BAD_REQUEST)
                            .errors(List.of("Invalid Username or Password"))
                            .timestamp(LocalDateTime.now())
                            .success(false)
                            .build()
            );
        }


    }

    @GetMapping("/is-authenticated")
    public ResponseEntity<Boolean> isAuthenticated(@RequestParam String email) {
        return ResponseEntity.ok(email != null);
    }

    @PostMapping("/send-reset-otp")
    public ResponseEntity<String> sendResetOtp(@RequestParam String email) {
        authService.sendResetOtp(email);
        return ResponseEntity.ok("OTP sent successfully");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {

        authService.resetPassword(request.getEmail(), request.getOtp(), request.getNewPassword());
        return ResponseEntity.ok("Password change successfully");


    }

    @PostMapping("/send-otp")
    public ResponseEntity<String> sendOtp(@RequestParam String email) {
        try {
            authService.sendOtp(email);
            return ResponseEntity.ok("OTP sent successfully");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @PostMapping("verify-otp")
    public void verifyEmail(@RequestBody Map<String, Object> request,
                            @RequestParam String email
    ) {
        if (request.get("otp").toString() == null) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing Details");
        }
        try {
            authService.verifyOtp(email, request.get("otp").toString());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
