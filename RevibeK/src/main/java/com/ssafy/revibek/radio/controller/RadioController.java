package com.ssafy.revibek.radio.controller;

import com.ssafy.revibek.radio.dto.RadioRequestDto;
import com.ssafy.revibek.radio.dto.RadioResponseDto;
import com.ssafy.revibek.radio.service.RadioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/radio")
@RequiredArgsConstructor
public class RadioController {

    private final RadioService radioService;

    // 라디오 세션 생성
    @PostMapping
    public ResponseEntity<String> createSession(@RequestBody RadioRequestDto dto) {
        radioService.createSession(dto);
        return ResponseEntity.ok("세션 생성 완료");
    }

    // 세션 단건 조회
    @GetMapping("/{id}")
    public ResponseEntity<RadioResponseDto> getSession(@PathVariable String id) {
        return ResponseEntity.ok(radioService.getSession(id));
    }

    // 유저 세션 목록 조회
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<RadioResponseDto>> getSessionByUser(@PathVariable String userId) {
        return ResponseEntity.ok(radioService.getSessionByUser(userId));
    }
}