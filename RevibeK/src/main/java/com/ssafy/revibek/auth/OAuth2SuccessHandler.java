package com.ssafy.revibek.auth;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.ssafy.revibek.auth.dto.AuthTokenResponseDto;
import com.ssafy.revibek.user.service.AuthService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Google 로그인은 외부(구글) 도메인을 거치는 풀 페이지 redirect라서, 일반 로그인처럼
 * JSON 응답을 프론트 JS가 직접 받을 방법이 없다. 그래서 토큰을 발급한 뒤 프론트 URL로
 * 다시 redirect하면서 토큰을 URL 프래그먼트(#)에 실어 보낸다. 프래그먼트는 서버로
 * 전송되지 않고 access log에도 남지 않아 쿼리스트링보다 안전하다.
 */
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;

    @Value("${app.oauth.frontend-redirect-url:http://localhost:5173}")
    private String frontendRedirectUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = (String) oAuth2User.getAttributes().get("email");
        String providerId = (String) oAuth2User.getAttributes().get("sub");
        String nickname = (String) oAuth2User.getAttributes().getOrDefault("name", "google-user");

        AuthTokenResponseDto tokenResponse = authService.loginWithGoogle(email, providerId, nickname);

        String fragment = "accessToken=" + encode(tokenResponse.getAccessToken())
            + "&refreshToken=" + encode(tokenResponse.getRefreshToken());
        String targetUrl = frontendRedirectUrl + "/oauth/callback#" + fragment;

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
