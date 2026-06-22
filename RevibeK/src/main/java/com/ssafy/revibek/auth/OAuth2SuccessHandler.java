package com.ssafy.revibek.auth;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.ssafy.revibek.auth.dto.AuthTokenResponseDto;
import com.ssafy.revibek.user.service.AuthService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
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
        invalidateOAuth2Session(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    // 프론트/백엔드가 같은 origin(docker의 nginx 리버스 프록시)일 때, oauth2Login()이
    // 세션에 남겨둔 OAuth2AuthenticationToken(principal.getName() == Google sub)을 브라우저가
    // 이후의 /api/users/me 같은 JWT 기반 요청에도 쿠키로 함께 보내버린다. JwtAuthenticationFilter는
    // SecurityContext가 비어있을 때만 토큰을 해석하므로, 세션이 남아있으면 우리 DB의 user.id가 아니라
    // Google sub 값이 인증 주체로 쓰여 "존재하지 않는 유저입니다" 500이 발생한다(docs/error/error08.md 3번).
    // 이 앱은 로그인 직후부터 전부 JWT만 쓰므로 세션을 즉시 없애 재발을 막는다.
    private void invalidateOAuth2Session(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
