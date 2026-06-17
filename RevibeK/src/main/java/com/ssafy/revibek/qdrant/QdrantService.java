package com.ssafy.revibek.qdrant;

import com.ssafy.revibek.song.dto.SongDto;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.QueryPoints;
import io.qdrant.client.grpc.Points.ScoredPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static io.qdrant.client.PointIdFactory.id;
import static io.qdrant.client.QueryFactory.nearest;
import static io.qdrant.client.ValueFactory.value;
import static io.qdrant.client.VectorsFactory.vectors;
import static io.qdrant.client.WithPayloadSelectorFactory.enable;

@Slf4j
@Service
@RequiredArgsConstructor
public class QdrantService {

    private final QdrantClient qdrantClient;

    @Value("${qdrant.enabled:false}")
    private boolean enabled;

    // Spring @Value 어노테이션과 Qdrant Value 타입 충돌 방지: 필드명으로 직접 주입
    @Value("${qdrant.collection:revibek_songs}")
    private String collection;

    private static final int VECTOR_SIZE = 9;

    public void createCollectionIfNotExists() {
        if (!enabled) {
            log.info("Qdrant disabled. Skip collection initialization.");
            return;
        }

        try {
            List<String> existing = qdrantClient.listCollectionsAsync().get();
            if (existing.contains(collection)) return;

            qdrantClient.createCollectionAsync(collection,
                VectorParams.newBuilder()
                    .setSize(VECTOR_SIZE)
                    .setDistance(Distance.Cosine)
                    .build()).get();
            log.info("Qdrant 컬렉션 생성: {}", collection);
        } catch (Exception e) {
            log.warn("Qdrant 컬렉션 생성 실패. fallback 사용 예정: {}", e.getMessage());
        }
    }

    public void upsertSong(SongDto song) {
        if (!enabled) {
            log.info("Qdrant disabled. Skip song upsert: {}", song.getId());
            return;
        }

        float[] vector = SongVectorUtil.toVector(song);
        if (vector == null) {
            log.warn("벡터 생성 불가 (분석값 없음): {}", song.getId());
            return;
        }
        try {
            qdrantClient.upsertAsync(collection, List.of(
                PointStruct.newBuilder()
                    .setId(id(UUID.fromString(song.getId())))
                    .setVectors(vectors(vector))
                    .putAllPayload(buildPayload(song))
                    .build()
            )).get();
        } catch (Exception e) {
            log.warn("Qdrant upsert 실패. songId={}, reason={}", song.getId(), e.getMessage());
        }
    }

    public void upsertSongs(List<SongDto> songs) {
        if (!enabled) {
            log.info("Qdrant disabled. Skip batch upsert: {} songs", songs.size());
            return;
        }

        List<PointStruct> points = songs.stream()
            .flatMap(s -> {
                float[] vec = SongVectorUtil.toVector(s);
                if (vec == null) return Stream.empty();
                return Stream.of(PointStruct.newBuilder()
                    .setId(id(UUID.fromString(s.getId())))
                    .setVectors(vectors(vec))
                    .putAllPayload(buildPayload(s))
                    .build());
            })
            .toList();

        if (points.isEmpty()) return;
        try {
            qdrantClient.upsertAsync(collection, points).get();
            log.info("Qdrant upsert 완료: {}곡", points.size());
        } catch (Exception e) {
            log.warn("Qdrant batch upsert 실패. fallback 사용 예정: {}", e.getMessage());
        }
    }

    /**
     * FastAPI embedding_service가 생성한 벡터를 직접 저장한다.
     * embedding이 null이면 SongVectorUtil 폴백으로 upsert한다.
     */
    public void upsertSongWithEmbedding(SongDto song, List<Float> embedding) {
        if (!enabled) {
            log.info("Qdrant disabled. Skip upsert: {}", song.getId());
            return;
        }
        if (song.getId() == null) return;

        float[] vector;
        if (embedding != null && !embedding.isEmpty()) {
            vector = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) vector[i] = embedding.get(i);
        } else {
            vector = SongVectorUtil.toVector(song);
        }
        if (vector == null) {
            log.warn("벡터 생성 불가 (분석값 없음): {}", song.getId());
            return;
        }

        try {
            qdrantClient.upsertAsync(collection, List.of(
                PointStruct.newBuilder()
                    .setId(id(UUID.fromString(song.getId())))
                    .setVectors(vectors(vector))
                    .putAllPayload(buildPayload(song))
                    .build()
            )).get();
            log.info("Qdrant upsert 완료 (embedding): songId={}", song.getId());
        } catch (Exception e) {
            log.warn("Qdrant upsert 실패. songId={}, reason={}", song.getId(), e.getMessage());
        }
    }

    /**
     * 9D 임베딩 벡터로 Qdrant를 직접 검색한다.
     * DB에 없는 외부 URL을 실시간 분석한 결과로 유사곡을 찾을 때 사용한다.
     */
    public List<String> searchByVector(List<Float> embedding, int limit) {
        if (!enabled) {
            log.info("Qdrant disabled. Return empty vector result for searchByVector");
            return List.of();
        }
        if (embedding == null || embedding.isEmpty()) return List.of();

        float[] vector = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) vector[i] = embedding.get(i);

        try {
            List<ScoredPoint> results = qdrantClient.queryAsync(
                QueryPoints.newBuilder()
                    .setCollectionName(collection)
                    .setQuery(nearest(vector))
                    .setLimit((long) limit)
                    .setWithPayload(enable(true))
                    .build()
            ).get();

            return results.stream()
                .map(p -> p.getId().getUuid())
                .toList();
        } catch (Exception e) {
            log.warn("Qdrant 벡터 검색 실패: {}", e.getMessage());
            return List.of();
        }
    }

    public List<String> searchSimilar(String songId, int limit) {
        if (!enabled) {
            log.info("Qdrant disabled. Return empty vector result for songId={}", songId);
            return List.of();
        }

        try {
            List<ScoredPoint> results = qdrantClient.queryAsync(
                QueryPoints.newBuilder()
                    .setCollectionName(collection)
                    .setQuery(nearest(id(UUID.fromString(songId))))
                    .setLimit((long) (limit + 1))
                    .setWithPayload(enable(true))
                    .build()
            ).get();

            return results.stream()
                .filter(p -> !p.getId().getUuid().equals(songId))
                .limit(limit)
                .map(p -> p.getId().getUuid())
                .toList();
        } catch (Exception e) {
            log.warn("Qdrant 검색 실패. songId={}, reason={}", songId, e.getMessage());
            return List.of();
        }
    }

    // Value 타입 충돌 방지: 완전한 클래스명 사용
    private Map<String, io.qdrant.client.grpc.JsonWithInt.Value> buildPayload(SongDto song) {
        Map<String, io.qdrant.client.grpc.JsonWithInt.Value> payload = new HashMap<>();
        if (song.getTitle()     != null) payload.put("title",      value(song.getTitle()));
        if (song.getGenre()     != null) payload.put("genre",      value(song.getGenre()));
        if (song.getEra()       != null) payload.put("era",        value(song.getEra()));
        if (song.getType()      != null) payload.put("type",       value(song.getType()));
        if (song.getYoutubeId() != null) payload.put("youtube_id", value(song.getYoutubeId()));
        return payload;
    }
}
