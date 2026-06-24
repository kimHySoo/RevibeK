package com.ssafy.revibek.config;

import com.ssafy.revibek.embedding.service.EmbeddingQdrantSyncService;
import com.ssafy.revibek.embedding.service.SongEmbeddingService;
import com.ssafy.revibek.qdrant.QdrantService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 컨테이너 기동 시마다 song_embeddings 생성 + Qdrant upsert(TEXT_OPENAI, AUDIO_9D)를 자동 실행한다.
// AUTO_SYNC_QDRANT_ON_STARTUP=false(기본값)면 동작하지 않는다.
// docker-compose의 depends_on이 qdrant를 service_healthy로 기다리게 해뒀어도, 네트워크/디스크가
// 느린 환경(error11.md 1번)에서는 여전히 타이밍이 빠듯할 수 있어 재시도를 둔다.
@Slf4j
@Component
@Order(100)
@RequiredArgsConstructor
public class QdrantStartupSync implements ApplicationRunner {

    private static final int MAX_ATTEMPTS = 5;
    private static final long BACKOFF_MS = 2000;

    private final SongEmbeddingService songEmbeddingService;
    private final EmbeddingQdrantSyncService embeddingQdrantSyncService;
    private final QdrantService qdrantService;

    @Value("${app.qdrant.auto-sync-on-startup:false}")
    private boolean autoSyncOnStartup;

    @Override
    public void run(ApplicationArguments args) {
        if (!autoSyncOnStartup) {
            return;
        }

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                int generated = songEmbeddingService.generateEmbeddings();
                int textSynced = embeddingQdrantSyncService.syncToQdrant();
                int audioSynced = qdrantService.embedAllAudioSongs();
                log.info("[QdrantStartupSync] 동기화 완료(시도 {}회): 임베딩 생성 {}개, TEXT_OPENAI {}개, AUDIO_9D {}개",
                    attempt, generated, textSynced, audioSynced);
                return;
            } catch (Exception e) {
                log.warn("[QdrantStartupSync] 동기화 실패(시도 {}/{}): {}", attempt, MAX_ATTEMPTS, e.getMessage());
                if (attempt == MAX_ATTEMPTS) {
                    return;
                }
                sleep(BACKOFF_MS);
            }
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
