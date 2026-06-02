// com.ssafy.revibek.youtube.controller.YoutubeController.java
package com.ssafy.revibek.youtube.controller;

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
    public ResponseEntity<String> addChannel(@RequestBody Map<String, String> request) {
        String url = request.get("url");
        youtubeService.processChannel(url);
        return ResponseEntity.ok("채널 추가 완료");
    }

    @PostMapping("/channels")
    public ResponseEntity<String> addChannels(@RequestBody Map<String, List<String>> request) {
        List<String> urls = request.get("urls");
        urls.forEach(youtubeService::processChannel);
        return ResponseEntity.ok("채널 " + urls.size() + "개 추가 완료");
    }
}