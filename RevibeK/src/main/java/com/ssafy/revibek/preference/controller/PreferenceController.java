package com.ssafy.revibek.preference.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ssafy.revibek.common.dto.ApiResponseDto;
import com.ssafy.revibek.preference.dto.UserPreferenceDto;
import com.ssafy.revibek.preference.dto.UserPreferenceRequestDto;
import com.ssafy.revibek.preference.service.PreferenceService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/preferences")
@RequiredArgsConstructor
public class PreferenceController {

    private final PreferenceService preferenceService;

    @PostMapping
    public ResponseEntity<ApiResponseDto<UserPreferenceDto>> savePreference(
            Authentication authentication,
            @RequestBody UserPreferenceRequestDto request
    ) {
        String userId = getAuthenticatedUserId(authentication);
        UserPreferenceDto data =
                preferenceService.savePreference(userId, request);

        return ResponseEntity.ok(
                ApiResponseDto.success("사용자 취향이 저장되었습니다.", data)
        );
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponseDto<UserPreferenceDto>> getMyPreference(
            Authentication authentication
    ) {
        String userId = getAuthenticatedUserId(authentication);
        UserPreferenceDto data = preferenceService.getPreference(userId);

        return ResponseEntity.ok(
                ApiResponseDto.success("사용자 취향 조회 완료", data)
        );
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponseDto<UserPreferenceDto>> updateMyPreference(
            Authentication authentication,
            @RequestBody UserPreferenceRequestDto request
    ) {
        String userId = getAuthenticatedUserId(authentication);
        UserPreferenceDto data =
                preferenceService.savePreference(userId, request);

        return ResponseEntity.ok(
                ApiResponseDto.success("사용자 취향을 수정했습니다.", data)
        );
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponseDto<Void>> deleteMyPreference(
            Authentication authentication
    ) {
        String userId = getAuthenticatedUserId(authentication);
        preferenceService.deletePreference(userId);

        return ResponseEntity.ok(
                ApiResponseDto.success("사용자 취향을 삭제했습니다.", null)
        );
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