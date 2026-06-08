package com.ssafy.revibek.auth;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ssafy.revibek.user.dto.UserAuthDto;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Getter;

@Component
public class JwtTokenProvider {
    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    @Value("${jwt.secret}")
    private String secret;

    @Getter
    @Value("${jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

    @Value("${jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("jwt.secret must be at least 32 bytes.");
        }
        secretKey = Keys.hmacShaKeyFor(secretBytes);
    }

    public String createAccessToken(UserAuthDto user) {
        return createToken(user, accessTokenExpirationMs, TOKEN_TYPE_ACCESS);
    }

    public String createRefreshToken(UserAuthDto user) {
        return createToken(user, refreshTokenExpirationMs, TOKEN_TYPE_REFRESH);
    }

    public String getUserId(String token) {
        return parseToken(token).getSubject();
    }

    public String getEmail(String token) {
        Object email = parseToken(token).get("email");
        return email == null ? null : email.toString();
    }

    public String getTokenType(String token) {
        Object tokenType = parseToken(token).get("tokenType");
        return tokenType == null ? null : tokenType.toString();
    }

    public boolean isRefreshToken(String token) {
        return TOKEN_TYPE_REFRESH.equals(getTokenType(token));
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private String createToken(UserAuthDto user, long expiresInMs, String tokenType) {
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(expiresInMs);

        return Jwts.builder()
            .subject(user.getId())
            .claim("email", user.getEmail())
            .claim("provider", user.getProvider())
            .claim("tokenType", tokenType)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiration))
            .signWith(secretKey)
            .compact();
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
