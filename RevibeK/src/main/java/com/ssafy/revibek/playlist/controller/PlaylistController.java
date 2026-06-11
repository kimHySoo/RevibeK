package com.ssafy.revibek.playlist.controller;

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

import com.ssafy.revibek.playlist.dto.PlaylistDto;
import com.ssafy.revibek.playlist.dto.PlaylistItemDto;
import com.ssafy.revibek.playlist.service.PlaylistService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/playlists")
@RequiredArgsConstructor
public class PlaylistController {

    private final PlaylistService playlistService;

    @PostMapping
    public ResponseEntity<PlaylistDto> createPlaylist(Authentication authentication,
                                                      @Valid @RequestBody PlaylistDto request) {
        return ResponseEntity.ok(playlistService.createPlaylist(authentication.getName(), request));
    }

    @GetMapping
    public ResponseEntity<List<PlaylistDto>> getMyPlaylists(Authentication authentication) {
        return ResponseEntity.ok(playlistService.getMyPlaylists(authentication.getName()));
    }

    @GetMapping("/{playlistId}")
    public ResponseEntity<PlaylistDto> getPlaylist(Authentication authentication,
                                                   @PathVariable String playlistId) {
        return ResponseEntity.ok(playlistService.getPlaylist(authentication.getName(), playlistId));
    }

    @PostMapping("/{playlistId}/items")
    public ResponseEntity<PlaylistItemDto> addItem(Authentication authentication,
                                                   @PathVariable String playlistId,
                                                   @Valid @RequestBody PlaylistItemDto request) {
        return ResponseEntity.ok(playlistService.addItem(authentication.getName(), playlistId, request));
    }

    @DeleteMapping("/{playlistId}/items/{itemId}")
    public ResponseEntity<Map<String, String>> deleteItem(Authentication authentication,
                                                          @PathVariable String playlistId,
                                                          @PathVariable String itemId) {
        playlistService.deleteItem(authentication.getName(), playlistId, itemId);
        return ResponseEntity.ok(Map.of("message", "플레이리스트 항목 삭제 완료"));
    }

    @DeleteMapping("/{playlistId}")
    public ResponseEntity<Map<String, String>> deletePlaylist(Authentication authentication,
                                                              @PathVariable String playlistId) {
        playlistService.deletePlaylist(authentication.getName(), playlistId);
        return ResponseEntity.ok(Map.of("message", "플레이리스트 삭제 완료"));
    }
}
