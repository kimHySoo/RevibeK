package com.ssafy.revibek.embedding.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.Points.PointStruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
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

    @Value("${fastapi.project.path:../RevibeK_AI}")
    private String fastApiProjectPath;

    @Value("${qdrant.enabled:false}")
    private boolean enabled;

    @Value("${qdrant.text-collection:revibek_song_text_embeddings}")
    private String collection;

    private final QdrantClient qdrantClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * song_embeddings 폴더의 임베딩 파일을 읽어 Qdrant 텍스트 임베딩 컬렉션에 upsert한다.
     *
     * @return Qdrant에 동기화된 곡 수
     */
    public int syncToQdrant() {
        if (!enabled) {
            log.info("[Embedding] Qdrant disabled. Skip sync.");
            return 0;
        }

        File dir = new File(fastApiProjectPath, "song_embeddings");
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) {
            log.info("[Embedding] 동기화 대상 파일 없음: {}", dir.getAbsolutePath());
            return 0;
        }

        List<PointStruct> points = new ArrayList<>();
        int vectorSize = 0;

        for (File file : files) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = objectMapper.readValue(file, Map.class);
                String songId = (String) data.get("songId");

                @SuppressWarnings("unchecked")
                List<Number> vectorList = (List<Number>) data.get("vector");
                if (songId == null || vectorList == null || vectorList.isEmpty()) {
                    continue;
                }

                float[] vector = new float[vectorList.size()];
                for (int i = 0; i < vectorList.size(); i++) {
                    vector[i] = vectorList.get(i).floatValue();
                }
                vectorSize = vector.length;

                points.add(PointStruct.newBuilder()
                    .setId(id(UUID.nameUUIDFromBytes(songId.getBytes())))
                    .setVectors(vectors(vector))
                    .putAllPayload(Map.of("song_id", value(songId)))
                    .build());
            } catch (Exception e) {
                log.warn("[Embedding] 파일 읽기 실패: {} - {}", file.getName(), e.getMessage());
            }
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
