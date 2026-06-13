package com.ssafy.revibek.embedding.controller;

import com.ssafy.revibek.embedding.service.EmbeddingQdrantSyncService;
import com.ssafy.revibek.embedding.service.SongEmbeddingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/embeddings")
@Tag(name = "Embedding", description = "곡 텍스트 임베딩 생성 및 Qdrant 동기화 API")
@RequiredArgsConstructor
public class EmbeddingController {

    private final SongEmbeddingService songEmbeddingService;
    private final EmbeddingQdrantSyncService embeddingQdrantSyncService;

    // 임베딩 파일이 없는 곡에 대해 임베딩 생성 후 song_embeddings 폴더에 저장
    @PostMapping("/generate")
    @Operation(summary = "곡 임베딩 생성", description = "임베딩 파일이 없는 곡에 대해 GMS 임베딩 API를 호출해 song_embeddings 폴더에 저장합니다.")
    public ResponseEntity<?> generate() {
        try {
            int count = songEmbeddingService.generateEmbeddings();
            return ResponseEntity.ok(count + "개 곡의 임베딩을 생성했습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    // song_embeddings 폴더의 임베딩 파일을 Qdrant에 upsert
    @PostMapping("/sync-to-qdrant")
    @Operation(summary = "임베딩 Qdrant 동기화", description = "song_embeddings 폴더의 임베딩 파일을 Qdrant에 upsert합니다.")
    public ResponseEntity<?> syncToQdrant() {
        try {
            int count = embeddingQdrantSyncService.syncToQdrant();
            return ResponseEntity.ok(count + "개 곡 벡터를 Qdrant에 동기화했습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
