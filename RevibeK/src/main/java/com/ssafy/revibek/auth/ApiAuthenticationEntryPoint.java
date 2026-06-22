package com.ssafy.revibek.auth;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * /api/** 요청이 인증되지 않았을 때, oauth2Login()의 기본 동작(구글 로그인 페이지로 302 리다이렉트)
 * 대신 401 JSON을 반환한다. 리다이렉트를 그대로 두면 브라우저(axios/XHR)가 accounts.google.com까지
 * 따라가다가 CORS 정책에 막혀 "CORS policy" 에러로만 보이고 진짜 원인(미인증)이 가려진다.
 */
@Component
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"message\":\"로그인이 필요합니다.\"}");
    }
}
