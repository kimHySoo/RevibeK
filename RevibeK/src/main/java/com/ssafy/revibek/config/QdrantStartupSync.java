package com.ssafy.revibek.config;

import com.ssafy.revibek.embedding.service.EmbeddingQdrantSyncService;
import com.ssafy.revibek.embedding.service.SongEmbeddingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 컨테이너 기동 시마다 song_embeddings 생성 + Qdrant upsert를 자동 실행한다.
// AUTO_SYNC_QDRANT_ON_STARTUP=false(기본값)면 동작하지 않는다.
@Slf4j
@Component
@Order(100)
@RequiredArgsConstructor
public class QdrantStartupSync implements ApplicationRunner {

    private final SongEmbeddingService songEmbeddingService;
    private final EmbeddingQdrantSyncService embeddingQdrantSyncService;

    @Value("${app.qdrant.auto-sync-on-startup:false}")
    private boolean autoSyncOnStartup;

    @Override
    public void run(ApplicationArguments args) {
        if (!autoSyncOnStartup) {
            return;
        }
        try {
            int generated = songEmbeddingService.generateEmbeddings();
            log.info("[QdrantStartupSync] 임베딩 생성 완료: {}개", generated);
            int synced = embeddingQdrantSyncService.syncToQdrant();
            log.info("[QdrantStartupSync] Qdrant 동기화 완료: {}개", synced);
        } catch (Exception e) {
            log.warn("[QdrantStartupSync] 자동 동기화 실패: {}", e.getMessage());
        }
    }
}
