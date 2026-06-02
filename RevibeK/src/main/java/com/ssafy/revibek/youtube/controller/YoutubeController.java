// com.ssafy.revibek.youtube.controller.YoutubeController.java
package com.ssafy.revibek.youtube.controller;

import com.ssafy.revibek.youtube.service.YoutubeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/youtube")
@RequiredArgsConstructor
public class YoutubeController {

    private final YoutubeService youtubeService;

    // POST /api/youtube/collect
    // 한 번만 호출하면 전체 채널 수집
    @PostMapping("/collect")
    public ResponseEntity<String> collect() {
        youtubeService.collectAll();
        return ResponseEntity.ok("수집 완료");
    }
}