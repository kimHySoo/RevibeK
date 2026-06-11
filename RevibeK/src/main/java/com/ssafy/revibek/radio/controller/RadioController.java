package com.ssafy.revibek.radio.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ssafy.revibek.radio.dto.RadioCreateRequestDto;
import com.ssafy.revibek.radio.dto.RadioCreateResponseDto;
import com.ssafy.revibek.radio.dto.RadioResponseDto;
import com.ssafy.revibek.radio.service.RadioService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/radio")
@RequiredArgsConstructor
public class RadioController {

    private final RadioService radioService;

    @PostMapping
    public ResponseEntity<RadioCreateResponseDto> createRadio(
            Authentication authentication,
            @Valid @RequestBody RadioCreateRequestDto request
    ) {
        String userId = getAuthenticatedUserId(authentication);
        return ResponseEntity.ok(radioService.createRadio(userId, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RadioResponseDto> getSession(
            Authentication authentication,
            @PathVariable String id
    ) {
        String userId = getAuthenticatedUserId(authentication);
        return ResponseEntity.ok(radioService.getSession(id, userId));
    }

    @GetMapping("/me")
    public ResponseEntity<List<RadioResponseDto>> getSessionsByUser(
            Authentication authentication
    ) {
        String userId = getAuthenticatedUserId(authentication);
        return ResponseEntity.ok(radioService.getSessionByUser(userId));
    }

    private String getAuthenticatedUserId(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || !StringUtils.hasText(authentication.getName())
                || "anonymousUser".equals(authentication.getName())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "인증된 사용자 정보가 필요합니다."
            );
        }

        String userId = authentication.getName();
        return userId.trim();
    }
}