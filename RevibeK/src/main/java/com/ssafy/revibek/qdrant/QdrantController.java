package com.ssafy.revibek.qdrant;

import com.ssafy.revibek.song.dto.SongDto;
import com.ssafy.revibek.song.service.SongService;
import com.ssafy.revibek.qdrant.dto.VectorSearchResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/qdrant")
@Tag(name = "Qdrant", description = "벡터 DB 관리 API")
@RequiredArgsConstructor
public class QdrantController {

    private final QdrantService qdrantService;
    private final SongService songService;

    @PostMapping("/embed")
    @Operation(summary = "곡 벡터 저장", description = "embedding_songs(AUDIO_9D)에 등록된 곡만 Qdrant에 upsert")
    public ResponseEntity<String> embedAll() {
        try {
            int count = qdrantService.embedAllAudioSongs();
            return ResponseEntity.ok(count + "곡 Qdrant 저장 완료");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/similar/{songId}")
    @Operation(summary = "유사곡 조회", description = "특정 곡과 유사한 곡 N개 반환")
    public ResponseEntity<VectorSearchResponseDto> getSimilar(
            @PathVariable String songId,
            @RequestParam(defaultValue = "10") int limit) {

        List<String> similarIds = qdrantService.searchSimilar(songId, limit);
        List<SongDto> songs = similarIds.stream()
            .map(songService::getSongById)
            .filter(song -> song != null)
            .toList();

        if (!songs.isEmpty()) {
            return ResponseEntity.ok(VectorSearchResponseDto.builder()
                .source("qdrant")
                .message("Qdrant vector search result.")
                .results(songs)
                .build());
        }

        List<SongDto> fallback = songService.getRecommendSongs().stream()
            .limit(limit)
            .toList();
        return ResponseEntity.ok(VectorSearchResponseDto.builder()
            .source("fallback")
            .message("Qdrant unavailable or no vector results. Using DB score fallback.")
            .results(fallback)
            .build());
    }
}
