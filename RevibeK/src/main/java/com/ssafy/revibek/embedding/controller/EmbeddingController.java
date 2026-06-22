package com.ssafy.revibek.embedding.controller;

import com.ssafy.revibek.analysis.dto.AnalyzeByUrlRequestDto;
import com.ssafy.revibek.analysis.dto.AnalyzeResponseDto;
import com.ssafy.revibek.analysis.service.AnalysisService;
import com.ssafy.revibek.embedding.service.EmbeddingQdrantSyncService;
import com.ssafy.revibek.embedding.service.SongEmbeddingService;
import com.ssafy.revibek.qdrant.QdrantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/embeddings")
@Tag(name = "Embedding", description = "곡 텍스트 임베딩 생성 및 Qdrant 동기화 API")
@RequiredArgsConstructor
public class EmbeddingController {

    private final SongEmbeddingService songEmbeddingService;
    private final EmbeddingQdrantSyncService embeddingQdrantSyncService;
    private final AnalysisService analysisService;
    private final QdrantService qdrantService;

    // embedding_songs(TEXT_OPENAI) 행이 없는 곡에 대해 GMS 임베딩 API를 호출해 vector 컬럼에 직접 저장
    // (과거에는 song_embeddings 폴더에 파일로 저장했으나 answer16.md 이후 DB가 단일 소스)
    @PostMapping("/generate")
    @Operation(summary = "곡 임베딩 생성", description = "embedding_songs(TEXT_OPENAI)에 없는 곡에 대해 GMS 임베딩 API를 호출해 vector 컬럼(JSON)에 직접 저장합니다.")
    public ResponseEntity<?> generate() {
        try {
            int count = songEmbeddingService.generateEmbeddings();
            return ResponseEntity.ok(count + "개 곡의 임베딩을 생성했습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    // YouTube URL 분석 → 9D 임베딩 생성 → Qdrant 유사곡 검색
    @PostMapping("/search-by-url")
    @Operation(summary = "URL 기반 유사곡 검색", description = "YouTube URL을 오디오 분석해 9D 임베딩을 생성하고 Qdrant에서 유사곡 ID 목록을 반환합니다.")
    public ResponseEntity<?> searchByUrl(@RequestBody AnalyzeByUrlRequestDto request) {
        if (!StringUtils.hasText(request.getYoutubeUrl())) {
            return ResponseEntity.badRequest().body("youtubeUrl은 필수입니다.");
        }
        try {
            AnalyzeResponseDto analysis = analysisService.analyzeByUrlAndPersist(request.getYoutubeUrl());
            if (analysis == null || analysis.getEmbedding() == null || analysis.getEmbedding().isEmpty()) {
                return ResponseEntity.ok(Map.of("embedding", List.of(), "similarSongIds", List.of()));
            }
            List<String> similarIds = qdrantService.searchByVector(analysis.getEmbedding(), 10);
            return ResponseEntity.ok(Map.of(
                "embedding", analysis.getEmbedding(),
                "similarSongIds", similarIds
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    // embedding_songs(TEXT_OPENAI, songs와 JOIN된 것만)를 Qdrant에 upsert
    @PostMapping("/sync-to-qdrant")
    @Operation(summary = "임베딩 Qdrant 동기화", description = "embedding_songs(TEXT_OPENAI)를 조회해 Qdrant에 upsert합니다.")
    public ResponseEntity<?> syncToQdrant() {
        try {
            int count = embeddingQdrantSyncService.syncToQdrant();
            return ResponseEntity.ok(count + "개 곡 벡터를 Qdrant에 동기화했습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
