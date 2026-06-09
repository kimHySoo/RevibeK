package com.ssafy.revibek.explore.controller;

import com.ssafy.revibek.explore.dto.ExploreResponseDto;
import com.ssafy.revibek.explore.service.ExploreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/explore")
@Tag(name = "Explore", description = "YouTube URL 기반 유사곡 탐색 API")
@RequiredArgsConstructor
public class ExploreController {

    private final ExploreService exploreService;

    @GetMapping
    @Operation(summary = "유사곡 탐색", description = "YouTube URL 입력 → 분석 → 유사곡 추천")
    public ResponseEntity<?> explore(
            @RequestParam String url,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            ExploreResponseDto result = exploreService.explore(url, limit);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}
