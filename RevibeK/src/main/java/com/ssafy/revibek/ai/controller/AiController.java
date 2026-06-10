package com.ssafy.revibek.ai.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.revibek.ai.dto.AiChatRequestDto;
import com.ssafy.revibek.ai.dto.AiChatResponseDto;
import com.ssafy.revibek.ai.service.ClaudeGmsService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final ClaudeGmsService claudeGmsService;

    @PostMapping("/chat")
    public ResponseEntity<AiChatResponseDto> chat(@Valid @RequestBody AiChatRequestDto request) {
        AiChatResponseDto response = claudeGmsService.generateText(
            request.prompt(),
            request.system(),
            request.maxTokens()
        );
        return ResponseEntity.ok(response);
    }
}
