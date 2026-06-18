package com.ssafy.revibek.auth;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Google 로그인 실패(예: 이미 로컬 계정으로 가입된 이메일) 시 500 에러 화면 대신
 * 프론트 로그인 페이지로 안전하게 돌려보낸다.
 */
@Slf4j
@Component
public class OAuth2FailureHandler implements AuthenticationFailureHandler {

    @Value("${app.oauth.frontend-redirect-url:http://localhost:5173}")
    private String frontendRedirectUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        log.error("Google 로그인 실패: {}", exception.getMessage(), exception);
        response.sendRedirect(frontendRedirectUrl + "/login?error=google-auth-failed");
    }
}
