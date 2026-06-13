// com.ssafy.revibek.youtube.controller.YoutubeController.java
package com.ssafy.revibek.youtube.controller;

import com.ssafy.revibek.youtube.dto.YoutubeFallbackResponseDto;
import com.ssafy.revibek.youtube.service.YoutubeService;

import org.springframework.web.bind.annotation.RequestBody;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/youtube")
@RequiredArgsConstructor
public class YoutubeController {

    private final YoutubeService youtubeService;

    @PostMapping("/channel")
    public ResponseEntity<YoutubeFallbackResponseDto> addChannel(@RequestBody Map<String, String> request) {
        String url = request.get("url");
        return ResponseEntity.ok(youtubeService.processChannelWithResponse(url));
    }

    @PostMapping("/channels")
    public ResponseEntity<List<YoutubeFallbackResponseDto>> addChannels(@RequestBody Map<String, List<String>> request) {
        List<String> urls = request.get("urls");
        List<YoutubeFallbackResponseDto> responses = urls.stream()
            .map(youtubeService::processChannelWithResponse)
            .toList();
        return ResponseEntity.ok(responses);
    }
}