package com.ssafy.revibek.coaching.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.revibek.coaching.dto.CoachingResponseDto;
import com.ssafy.revibek.coaching.service.CoachingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/coaching")
@RequiredArgsConstructor
public class CoachingController {

    private final CoachingService coachingService;

    @GetMapping("/me")
    public ResponseEntity<CoachingResponseDto> getMyCoaching(Authentication authentication) {
        return ResponseEntity.ok(coachingService.analyzeUser(authentication.getName()));
    }
}
