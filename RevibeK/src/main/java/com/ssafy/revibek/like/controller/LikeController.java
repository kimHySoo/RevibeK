package com.ssafy.revibek.like.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.revibek.like.dto.LikeDto;
import com.ssafy.revibek.like.dto.LikeStatusDto;
import com.ssafy.revibek.like.service.LikeService;
import com.ssafy.revibek.song.dto.SongDto;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/likes")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    @PostMapping
    public ResponseEntity<LikeStatusDto> addLike(Authentication authentication,
                                                 @Valid @RequestBody LikeDto request) {
        return ResponseEntity.ok(
            likeService.addLike(authentication.getName(), request.getSongId())
        );
    }

    @DeleteMapping("/{songId}")
    public ResponseEntity<LikeStatusDto> removeLike(Authentication authentication,
                                                    @PathVariable String songId) {
        return ResponseEntity.ok(
            likeService.removeLike(authentication.getName(), songId)
        );
    }

    @GetMapping("/{songId}/status")
    public ResponseEntity<LikeStatusDto> getLikeStatus(Authentication authentication,
                                                       @PathVariable String songId) {
        return ResponseEntity.ok(
            likeService.getLikeStatus(authentication.getName(), songId)
        );
    }

    @GetMapping
    public ResponseEntity<List<LikeDto>> getMyLikes(Authentication authentication) {
        return ResponseEntity.ok(likeService.getMyLikes(authentication.getName()));
    }

    @GetMapping("/songs")
    public ResponseEntity<List<SongDto>> getMyLikedSongs(Authentication authentication) {
        return ResponseEntity.ok(likeService.getMyLikedSongs(authentication.getName()));
    }

    @GetMapping("/{songId}/count")
    public ResponseEntity<Map<String, Object>> getLikeCount(@PathVariable String songId) {
        return ResponseEntity.ok(Map.of(
            "songId", songId,
            "likeCount", likeService.getLikeCount(songId)
        ));
    }
}
