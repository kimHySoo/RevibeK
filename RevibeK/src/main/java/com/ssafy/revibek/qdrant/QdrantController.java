package com.ssafy.revibek.qdrant;

import com.ssafy.revibek.embedding.dto.EmbeddingSongDto;
import com.ssafy.revibek.embedding.mapper.EmbeddingSongDao;
import com.ssafy.revibek.song.dto.SongDto;
import com.ssafy.revibek.song.service.SongService;
import com.ssafy.revibek.qdrant.dto.VectorSearchResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/qdrant")
@Tag(name = "Qdrant", description = "벡터 DB 관리 API")
@RequiredArgsConstructor
public class QdrantController {

    private static final String AUDIO_9D = "AUDIO_9D";

    private final QdrantService qdrantService;
    private final SongService songService;
    private final EmbeddingSongDao embeddingSongDao;

    @PostMapping("/embed")
    @Operation(summary = "곡 벡터 저장", description = "embedding_songs(AUDIO_9D)에 등록된 곡만 Qdrant에 upsert")
    public ResponseEntity<String> embedAll() {
        qdrantService.createCollectionIfNotExists();
        List<SongDto> songs = songService.getSongsWithEmbeddingMeta(AUDIO_9D);
        Map<String, List<Float>> vectorsBySongId = embeddingSongDao.selectByEmbeddingType(AUDIO_9D).stream()
            .collect(Collectors.toMap(EmbeddingSongDto::getSongId, EmbeddingSongDto::getVector));
        qdrantService.upsertSongs(songs, vectorsBySongId);
        return ResponseEntity.ok(songs.size() + "곡 Qdrant 저장 완료");
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
