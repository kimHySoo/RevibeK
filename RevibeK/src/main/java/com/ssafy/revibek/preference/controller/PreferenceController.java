package com.ssafy.revibek.preference.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
        @RequestHeader(value = "X-USER-ID", required = false) String headerUserId,
        @RequestParam(value = "userId", required = false) String requestUserId,
        @RequestBody UserPreferenceRequestDto request
    ) {
        String userId = resolveUserId(authentication, headerUserId, requestUserId);
        UserPreferenceDto data = preferenceService.savePreference(userId, request);
        return ResponseEntity.ok(ApiResponseDto.success("사용자 취향이 저장되었습니다.", data));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponseDto<UserPreferenceDto>> getMyPreference(
        Authentication authentication,
        @RequestHeader(value = "X-USER-ID", required = false) String headerUserId,
        @RequestParam(value = "userId", required = false) String requestUserId
    ) {
        String userId = resolveUserId(authentication, headerUserId, requestUserId);
        UserPreferenceDto data = preferenceService.getPreference(userId);
        return ResponseEntity.ok(ApiResponseDto.success("사용자 취향 조회 완료", data));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponseDto<UserPreferenceDto>> updateMyPreference(
        Authentication authentication,
        @RequestHeader(value = "X-USER-ID", required = false) String headerUserId,
        @RequestParam(value = "userId", required = false) String requestUserId,
        @RequestBody UserPreferenceRequestDto request
    ) {
        String userId = resolveUserId(authentication, headerUserId, requestUserId);
        UserPreferenceDto data = preferenceService.savePreference(userId, request);
        return ResponseEntity.ok(ApiResponseDto.success("사용자 취향이 수정되었습니다.", data));
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponseDto<Void>> deleteMyPreference(
        Authentication authentication,
        @RequestHeader(value = "X-USER-ID", required = false) String headerUserId,
        @RequestParam(value = "userId", required = false) String requestUserId
    ) {
        String userId = resolveUserId(authentication, headerUserId, requestUserId);
        preferenceService.deletePreference(userId);
        return ResponseEntity.ok(ApiResponseDto.success("사용자 취향이 삭제되었습니다.", null));
    }

    private String resolveUserId(Authentication authentication, String headerUserId, String requestUserId) {
        if (StringUtils.hasText(headerUserId)) {
            return headerUserId.trim();
        }
        if (StringUtils.hasText(requestUserId)) {
            return requestUserId.trim();
        }
        if (authentication != null && StringUtils.hasText(authentication.getName())) {
            return authentication.getName();
        }
        throw new IllegalArgumentException("사용자 ID가 필요합니다. Authorization 또는 X-USER-ID를 전달해주세요.");
    }
}
