package com.ssafy.revibek.ai.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.revibek.ai.dto.AiChatRequestDto;
import com.ssafy.revibek.ai.dto.AiChatResponseDto;
import com.ssafy.revibek.ai.dto.ChatTtsRequestDto;
import com.ssafy.revibek.ai.dto.ChatTtsResponseDto;
import com.ssafy.revibek.ai.dto.TtsSynthesizeRequestDto;
import com.ssafy.revibek.ai.dto.TtsSynthesizeResponseDto;
import com.ssafy.revibek.ai.service.ClaudeGmsService;
import com.ssafy.revibek.ai.service.GoogleTtsService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final ClaudeGmsService claudeGmsService;
    private final GoogleTtsService googleTtsService;

    @PostMapping("/chat")
    public ResponseEntity<AiChatResponseDto> chat(@Valid @RequestBody AiChatRequestDto request) {
        AiChatResponseDto response = claudeGmsService.generateText(
            request.prompt(),
            request.system(),
            request.maxTokens()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/chat-tts")
    public ResponseEntity<ChatTtsResponseDto> chatTts(@Valid @RequestBody ChatTtsRequestDto request) {
        AiChatResponseDto chat = claudeGmsService.generateText(
            request.prompt(),
            request.system(),
            request.maxTokens()
        );

        TtsSynthesizeRequestDto ttsReq = new TtsSynthesizeRequestDto(
            chat.text(),
            request.languageCode(),
            request.voiceName(),
            request.speakingRate(),
            request.pitch(),
            request.audioEncoding()
        );
        TtsSynthesizeResponseDto tts = googleTtsService.synthesizeWithPreset(ttsReq, request.preset());

        return ResponseEntity.ok(new ChatTtsResponseDto(
            chat.text(),
            tts.audioContentBase64(),
            tts.contentType(),
            tts.languageCode(),
            tts.voiceName(),
            tts.audioEncoding(),
            chat.requestCostCredit(),
            chat.totalSpentCredit(),
            chat.remainingBudgetCredit()
        ));
    }
}
