package com.ssafy.revibek.ai.controller;

import java.util.Base64;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.revibek.ai.dto.TtsSynthesizeRequestDto;
import com.ssafy.revibek.ai.dto.TtsSynthesizeResponseDto;
import com.ssafy.revibek.ai.dto.TtsVoiceResponseDto;
import com.ssafy.revibek.ai.service.GoogleTtsService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ai/tts")
@RequiredArgsConstructor
public class GoogleTtsController {

    private final GoogleTtsService googleTtsService;

    @GetMapping("/voices")
    public ResponseEntity<List<TtsVoiceResponseDto>> listVoices(
        @RequestParam(required = false) String languageCode
    ) {
        return ResponseEntity.ok(googleTtsService.listVoices(languageCode));
    }

    @PostMapping("/synthesize")
    public ResponseEntity<TtsSynthesizeResponseDto> synthesize(
        @Valid @RequestBody TtsSynthesizeRequestDto request
    ) {
        return ResponseEntity.ok(googleTtsService.synthesize(request));
    }

    @PostMapping(value = "/synthesize/audio", produces = "audio/mpeg")
    public ResponseEntity<byte[]> synthesizeAudio(
        @Valid @RequestBody TtsSynthesizeRequestDto request,
        @RequestParam(required = false) String preset
    ) {
        TtsSynthesizeResponseDto result = googleTtsService.synthesizeWithPreset(request, preset);
        byte[] audioBytes = Base64.getDecoder().decode(result.audioContentBase64());
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"tts.mp3\"")
            .contentType(MediaType.parseMediaType(result.contentType()))
            .body(audioBytes);
    }
}
