package com.ssafy.revibek.user.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.revibek.auth.JwtTokenProvider;
import com.ssafy.revibek.auth.RefreshTokenStore;
import com.ssafy.revibek.auth.dto.AuthTokenResponseDto;
import com.ssafy.revibek.user.dto.UserAuthDto;
import com.ssafy.revibek.user.dto.UserLoginRequestDto;
import com.ssafy.revibek.user.dto.UserRegisterRequestDto;
import com.ssafy.revibek.user.dto.UserResponseDto;
import com.ssafy.revibek.user.mapper.UserMapper;

import lombok.RequiredArgsConstructor;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final EmailVerificationService emailVerificationService;

    @Transactional
    public void signUp(UserRegisterRequestDto dto) {
        String email = normalizeEmail(dto.getEmail());
        if (!emailVerificationService.isVerified(email)) {
            throw new RuntimeException("회원가입 전 이메일 인증이 필요합니다.");
        }
        UserAuthDto existing = userMapper.selectUserAuthByEmail(email);
        if (existing != null) {
            throw new RuntimeException("이미 사용중인 이메일입니다.");
        }
        String passwordHash = passwordEncoder.encode(dto.getPassword());
        userMapper.insertLocalUser(dto.getNickname(), email, passwordHash);
        emailVerificationService.consumeVerification(email);
    }

    public AuthTokenResponseDto login(UserLoginRequestDto dto) {
        UserAuthDto user = userMapper.selectUserAuthByEmail(normalizeEmail(dto.getEmail()));
        if (user == null || user.getPasswordHash() == null) {
            throw new RuntimeException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        return issueTokens(user);
    }

    @Transactional
    public AuthTokenResponseDto loginWithGoogle(String email, String providerId, String nickname) {
        email = normalizeEmail(email);
        if (email == null || email.isBlank() || providerId == null || providerId.isBlank()) {
            throw new RuntimeException("Google 인증 정보가 올바르지 않습니다.");
        }

        UserAuthDto user = userMapper.selectUserAuthByEmail(email);
        if (user == null) {
            userMapper.insertGoogleUser(nickname, email, providerId);
            user = userMapper.selectUserAuthByEmail(email);
        } else if ("local".equalsIgnoreCase(user.getProvider())) {
            throw new RuntimeException("이미 로컬 계정으로 가입된 이메일입니다.");
        }
        if (user == null) {
            throw new RuntimeException("Google 로그인 유저 생성에 실패했습니다.");
        }

        return issueTokens(user);
    }

    public AuthTokenResponseDto refresh(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("유효하지 않은 refresh token 입니다.");
        }
        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new RuntimeException("refresh token 타입이 아닙니다.");
        }
        String userId = jwtTokenProvider.getUserId(refreshToken);
        if (!refreshTokenStore.isValid(userId, refreshToken)) {
            throw new RuntimeException("로그아웃되었거나 만료된 refresh token 입니다.");
        }
        UserResponseDto user = userMapper.selectUserById(userId);
        if (user == null) {
            throw new RuntimeException("존재하지 않는 유저입니다.");
        }
        UserAuthDto authDto = userMapper.selectUserAuthByEmail(user.getEmail());
        if (authDto == null) {
            throw new RuntimeException("유저 인증 정보를 찾을 수 없습니다.");
        }
        refreshTokenStore.revoke(refreshToken);
        return issueTokens(authDto);
    }

    public void logout(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("유효하지 않은 refresh token 입니다.");
        }
        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new RuntimeException("refresh token 타입이 아닙니다.");
        }
        refreshTokenStore.revoke(refreshToken);
    }

    private AuthTokenResponseDto issueTokens(UserAuthDto user) {
        String accessToken = jwtTokenProvider.createAccessToken(user);
        String refreshToken = jwtTokenProvider.createRefreshToken(user);
        refreshTokenStore.save(user.getId(), refreshToken);
        UserResponseDto responseUser = new UserResponseDto(
            user.getId(),
            user.getNickname(),
            user.getEmail(),
            user.getProvider()
        );

        return new AuthTokenResponseDto(
            accessToken,
            refreshToken,
            "Bearer",
            jwtTokenProvider.getAccessTokenExpirationMs(),
            responseUser
        );
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
