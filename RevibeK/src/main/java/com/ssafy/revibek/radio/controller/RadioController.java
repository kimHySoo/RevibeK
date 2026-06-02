package com.ssafy.revibek.radio.controller;

import com.ssafy.revibek.radio.dto.RadioRequestDto;
import com.ssafy.revibek.radio.dto.RadioResponseDto;
import com.ssafy.revibek.radio.service.RadioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/radio")
@RequiredArgsConstructor
public class RadioController {

    private final RadioService radioService;

    // 라디오 세션 생성
    @PostMapping
    public ResponseEntity<Map<String, String>> createSession(Authentication authentication,
                                                             @Valid @RequestBody RadioRequestDto dto) {
        // [FIX] 생성 결과로 sessionId를 응답해 프론트가 후속 조회/재생성에 활용 가능하게 수정.
        String sessionId = radioService.createSession(authentication.getName(), dto);
        return ResponseEntity.ok(Map.of(
                "message", "세션 생성 완료",
                "sessionId", sessionId
        ));
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