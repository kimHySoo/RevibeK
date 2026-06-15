package com.ssafy.revibek.song.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.revibek.song.dto.SongDto;
import com.ssafy.revibek.song.service.SongService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/songs")
@Tag(name = "Song", description = "노래 관련 API")
public class SongController {

    @Autowired
    private SongService songService;

    // 노래 등록
    @PostMapping
    @Operation(summary = "노래 등록", description = "새로운 노래를 등록합니다.")
    public ResponseEntity<?> registerSong(@RequestBody SongDto song) {
        try {
            int result = songService.registerSong(song);
            if (result > 0)
                return ResponseEntity.ok("노래 등록 성공");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("노래 등록 실패");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    // 전체 조회
    @GetMapping
    @Operation(summary = "전체 노래 조회", description = "등록된 모든 노래를 조회합니다.")
    public ResponseEntity<?> getAllSongs() {
        try {
            List<SongDto> songs = songService.getAllSongs();
            return ResponseEntity.ok(songs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    // ID로 단건 조회
    @GetMapping("/{id}")
    @Operation(summary = "노래 단건 조회", description = "ID로 노래를 조회합니다.")
    public ResponseEntity<?> getSongById(
            @PathVariable @Parameter(description = "노래 ID") String id) {
        try {
            SongDto song = songService.getSongById(id);
            if (song != null)
                return ResponseEntity.ok(song);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("노래를 찾을 수 없습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    // 제목으로 검색
    @GetMapping("/search")
    @Operation(summary = "노래 검색", description = "제목으로 노래를 검색합니다.")
    public ResponseEntity<?> getSongByTitle(
            @RequestParam @Parameter(description = "검색할 노래 제목") String title) {
        try {
            List<SongDto> songs = songService.getSongByTitle(title);
            return ResponseEntity.ok(songs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    // 장르별 조회
    @GetMapping("/genre")
    @Operation(summary = "장르별 노래 조회", description = "장르로 노래 목록을 조회합니다.")
    public ResponseEntity<?> getSongsByGenre(
            @RequestParam @Parameter(description = "장르 (발라드|댄스|힙합|R&B|록)") String genre) {
        try {
            List<SongDto> songs = songService.getSongsByGenre(genre);
            return ResponseEntity.ok(songs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    // 추천 노래 조회
    @GetMapping("/recommend")
    @Operation(summary = "추천 노래 조회", description = "점수 기반 상위 노래를 조회합니다.")
    public ResponseEntity<?> getRecommendSongs() {
        try {
            List<SongDto> songs = songService.getRecommendSongs();
            return ResponseEntity.ok(songs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    // 노래 수정
    @PutMapping("/{id}")
    @Operation(summary = "노래 수정", description = "노래 정보를 수정합니다.")
    public ResponseEntity<?> modifySong(
            @PathVariable @Parameter(description = "노래 ID") String id,
            @RequestBody SongDto song) {
        try {
            song.setId(id);
            int result = songService.modifySong(song);
            if (result > 0)
                return ResponseEntity.ok("노래 수정 성공");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("노래 수정 실패");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    // 노래 삭제
    @DeleteMapping("/{id}")
    @Operation(summary = "노래 삭제", description = "노래를 삭제합니다.")
    public ResponseEntity<?> removeSong(
            @PathVariable @Parameter(description = "노래 ID") String id) {
        try {
            int result = songService.removeSong(id);
            if (result > 0)
                return ResponseEntity.ok("노래 삭제 성공");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("노래 삭제 실패");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}