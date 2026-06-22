package com.ssafy.revibek.embedding.service;

import com.ssafy.revibek.embedding.dto.EmbeddingSongDto;
import com.ssafy.revibek.embedding.mapper.EmbeddingSongDao;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.Points.PointStruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.qdrant.client.PointIdFactory.id;
import static io.qdrant.client.ValueFactory.value;
import static io.qdrant.client.VectorsFactory.vectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingQdrantSyncService {

    private static final String EMBEDDING_TYPE = "TEXT_OPENAI";

    @Value("${qdrant.enabled:false}")
    private boolean enabled;

    @Value("${qdrant.text-collection:revibek_song_text_embeddings}")
    private String collection;

    private final QdrantClient qdrantClient;
    private final EmbeddingSongDao embeddingSongDao;

    /**
     * embedding_songs(TEXT_OPENAI, songs와 JOIN된 것만)를 읽어 Qdrant 텍스트 임베딩
     * 컬렉션에 upsert한다. answer16.md 4.2 -- 과거에는 song_embeddings/ 디렉터리를
     * 직접 스캔했으나, 벡터 본체가 embedding_songs.vector로 옮겨오면서 DB 조회로 대체했다.
     *
     * @return Qdrant에 동기화된 곡 수
     */
    public int syncToQdrant() {
        if (!enabled) {
            log.info("[Embedding] Qdrant disabled. Skip sync.");
            return 0;
        }

        List<EmbeddingSongDto> embeddings = embeddingSongDao.selectByEmbeddingType(EMBEDDING_TYPE);
        if (embeddings.isEmpty()) {
            log.info("[Embedding] 동기화 대상 없음 (embedding_songs.embedding_type={})", EMBEDDING_TYPE);
            return 0;
        }

        List<PointStruct> points = new ArrayList<>();
        int vectorSize = 0;

        for (EmbeddingSongDto e : embeddings) {
            String songId = e.getSongId();
            List<Float> vectorList = e.getVector();
            if (vectorList == null || vectorList.isEmpty()) {
                continue;
            }

            float[] vector = new float[vectorList.size()];
            for (int i = 0; i < vectorList.size(); i++) {
                vector[i] = vectorList.get(i);
            }
            vectorSize = vector.length;

            points.add(PointStruct.newBuilder()
                .setId(id(UUID.nameUUIDFromBytes(songId.getBytes())))
                .setVectors(vectors(vector))
                .putAllPayload(Map.of("song_id", value(songId)))
                .build());
        }

        if (points.isEmpty()) {
            return 0;
        }

        try {
            createCollectionIfNotExists(vectorSize);
            qdrantClient.upsertAsync(collection, points).get();
            log.info("[Embedding] Qdrant 동기화 완료: {}개", points.size());
            return points.size();
        } catch (Exception e) {
            log.warn("[Embedding] Qdrant upsert 실패: {}", e.getMessage());
            return 0;
        }
    }

    private void createCollectionIfNotExists(int vectorSize) throws Exception {
        List<String> existing = qdrantClient.listCollectionsAsync().get();
        if (existing.contains(collection)) {
            return;
        }

        qdrantClient.createCollectionAsync(collection,
            VectorParams.newBuilder()
                .setSize(vectorSize)
                .setDistance(Distance.Cosine)
                .build()).get();
        log.info("[Embedding] Qdrant 컬렉션 생성: {} (size={})", collection, vectorSize);
    }
}
