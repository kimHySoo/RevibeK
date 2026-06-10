package com.ssafy.revibek.radio.controller;

import com.ssafy.revibek.radio.dto.RadioCreateRequestDto;
import com.ssafy.revibek.radio.dto.RadioCreateResponseDto;
import com.ssafy.revibek.radio.dto.RadioResponseDto;
import com.ssafy.revibek.radio.service.RadioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/radio")
@RequiredArgsConstructor
public class RadioController {

    private final RadioService radioService;

    // 라디오 세션 생성
    @PostMapping
    public ResponseEntity<RadioCreateResponseDto> createRadio(Authentication authentication,
                                                              @Valid @RequestBody RadioCreateRequestDto dto) {
        return ResponseEntity.ok(radioService.createRadio(authentication.getName(), dto));
    }

    // 세션 단건 조회
    @GetMapping("/{id}")
    public ResponseEntity<RadioResponseDto> getSession(Authentication authentication,
                                                       @PathVariable String id) {
        return ResponseEntity.ok(radioService.getSession(id, authentication.getName()));
    }

    // 내 세션 목록 조회
    @GetMapping("/me")
    public ResponseEntity<List<RadioResponseDto>> getSessionByUser(Authentication authentication) {
        return ResponseEntity.ok(radioService.getSessionByUser(authentication.getName()));
    }
}