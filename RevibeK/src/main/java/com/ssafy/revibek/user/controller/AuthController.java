package com.ssafy.revibek.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.revibek.auth.dto.AuthTokenResponseDto;
import com.ssafy.revibek.auth.dto.LogoutRequestDto;
import com.ssafy.revibek.auth.dto.RefreshTokenRequestDto;
import com.ssafy.revibek.user.dto.EmailVerificationCheckRequestDto;
import com.ssafy.revibek.user.dto.EmailVerificationSendRequestDto;
import com.ssafy.revibek.user.dto.UserLoginRequestDto;
import com.ssafy.revibek.user.dto.UserRegisterRequestDto;
import com.ssafy.revibek.user.service.AuthService;
import com.ssafy.revibek.user.service.EmailVerificationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/email/send")
    public ResponseEntity<String> sendVerificationCode(@Valid @RequestBody EmailVerificationSendRequestDto dto) {
        emailVerificationService.sendVerificationCode(dto.getEmail());
        return ResponseEntity.ok("인증코드 발송 완료");
    }

    @PostMapping("/email/verify")
    public ResponseEntity<String> verifyEmailCode(@Valid @RequestBody EmailVerificationCheckRequestDto dto) {
        emailVerificationService.verifyCode(dto.getEmail(), dto.getCode());
        return ResponseEntity.ok("이메일 인증 완료");
    }

    @PostMapping("/signup")
    public ResponseEntity<String> signUp(@Valid @RequestBody UserRegisterRequestDto dto) {
        authService.signUp(dto);
        return ResponseEntity.ok("회원가입 완료");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthTokenResponseDto> login(@Valid @RequestBody UserLoginRequestDto dto) {
        return ResponseEntity.ok(authService.login(dto));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthTokenResponseDto> refresh(@Valid @RequestBody RefreshTokenRequestDto dto) {
        return ResponseEntity.ok(authService.refresh(dto.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@Valid @RequestBody LogoutRequestDto dto) {
        authService.logout(dto.getRefreshToken());
        return ResponseEntity.ok("로그아웃 완료");
    }
}
