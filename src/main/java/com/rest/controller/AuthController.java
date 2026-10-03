package com.rest.controller;


import com.rest.dtos.CustomerDtos;
import com.rest.helper.*;
import com.rest.repository.CustomerRepository;
import com.rest.security.JwtToken;
import com.rest.service.AuthService;
import com.rest.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
@CrossOrigin(origins = "*")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final ModelMapper modelMapper;
    private final JwtToken jwtToken;
    private final CustomerRepository userRepository;
    private final AuthService authService;
    private final CustomerService customerService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody CustomerDtos customerDto) {
        try {
            CustomerDtos created = customerService.createCustomer(customerDto);
            String token = jwtToken.generateToken(created.getEmail(), true);
            String refreshToken = jwtToken.generateToken(created.getEmail(), false);
            JwtResponse response = JwtResponse.builder()
                    .accessToken(token)
                    .refreshToken(refreshToken)
                    .user(created)
                    .build();
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ApiResponse.builder()
                            .status(HttpStatus.BAD_REQUEST)
                            .errors(List.of(ex.getMessage() != null ? ex.getMessage() : "Registration failed"))
                            .success(false)
                            .timestamp(LocalDateTime.now())
                            .build()
            );
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword());
            authenticationManager.authenticate(authentication);
            UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.getEmail());
            CustomerDtos userDto = modelMapper.map(userRepository.findByEmail(userDetails.getUsername()).get(), CustomerDtos.class);
            String token = jwtToken.generateToken(userDto.getEmail(), true);
            String refreshToken = jwtToken.generateToken(userDto.getEmail(), false);
            JwtResponse build = JwtResponse.builder()
                    .accessToken(token)
                    .refreshToken(refreshToken)
                    .user(userDto)
                    .build();
            return ResponseEntity.ok(build);
        } catch (Exception ex) {
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

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName().equalsIgnoreCase("anonymousUser")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }
        return userRepository.findByEmail(auth.getName())
                .map(user -> ResponseEntity.ok(modelMapper.map(user, CustomerDtos.class)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        if (jwtToken.validateToken(refreshTokenRequest.getRefreshToken()) && jwtToken.isRefreshToken(refreshTokenRequest.getRefreshToken())) {
            String username = jwtToken.extractEmail(refreshTokenRequest.getRefreshToken());
            CustomerDtos userDto = modelMapper.map(userRepository.findByEmail(username).get(), CustomerDtos.class);
            String accessToken = jwtToken.generateToken(userDto.getEmail(), true);
            String newRefreshToken = jwtToken.generateToken(userDto.getEmail(), false);
            JwtResponse response = JwtResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(newRefreshToken)
                    .user(userDto).build();
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ApiResponse.builder()
                            .status(HttpStatus.BAD_REQUEST)
                            .errors(List.of("Invalid Refresh Token"))
                            .timestamp(LocalDateTime.now())
                            .success(false)
                            .build()
            );
        }
    }

    @GetMapping("/is-authenticated")
    public ResponseEntity<Boolean> isAuthenticated(@RequestParam String email) {
        return ResponseEntity.ok(email != null && userRepository.findByEmail(email).isPresent());
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getEmail(), request.getOtp(), request.getNewPassword());
        return ResponseEntity.ok("Password changed successfully");
    }

    @PostMapping("/resend-registration-otp")
    public ResponseEntity<String> resendRegistrationOtp(
            @RequestParam String email) {

        authService.sendOtp(email);

        return ResponseEntity.ok("Registration OTP sent successfully");
    }
    @PostMapping("/verify-otp")
    public ResponseEntity<String> verifyEmail(@RequestBody Map<String, Object> request, @RequestParam String email) {
        if (request.get("otp") == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing OTP in request");
        }
        try {
            authService.verifyOtp(email, request.get("otp").toString());
            return ResponseEntity.ok("OTP verified successfully");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
