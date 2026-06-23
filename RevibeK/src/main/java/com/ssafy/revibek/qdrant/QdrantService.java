package com.ssafy.revibek.qdrant;

import com.ssafy.revibek.embedding.dto.EmbeddingSongDto;
import com.ssafy.revibek.embedding.mapper.EmbeddingSongDao;
import com.ssafy.revibek.mood.GenerationCode;
import com.ssafy.revibek.mood.GenreNormalizer;
import com.ssafy.revibek.song.dto.SongDto;
import com.ssafy.revibek.song.mapper.SongDao;
import com.ssafy.revibek.song.service.SongService;
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
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.qdrant.client.PointIdFactory.id;
import static io.qdrant.client.QueryFactory.nearest;
import static io.qdrant.client.ValueFactory.list;
import static io.qdrant.client.ValueFactory.value;
import static io.qdrant.client.VectorsFactory.vectors;
import static io.qdrant.client.WithPayloadSelectorFactory.enable;

@Slf4j
@Service
@RequiredArgsConstructor
public class QdrantService {

    public static final String AUDIO_9D = "AUDIO_9D";

    private final QdrantClient qdrantClient;
    private final SongDao songDao;
    private final SongService songService;
    private final EmbeddingSongDao embeddingSongDao;

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
            createCollectionInternal();
        } catch (Exception e) {
            log.warn("Qdrant 컬렉션 생성 실패. fallback 사용 예정: {}", e.getMessage());
        }
    }

    private void createCollectionInternal() throws Exception {
        List<String> existing = qdrantClient.listCollectionsAsync().get();
        if (existing.contains(collection)) return;

        qdrantClient.createCollectionAsync(collection,
            VectorParams.newBuilder()
                .setSize(VECTOR_SIZE)
                .setDistance(Distance.Cosine)
                .build()).get();
        log.info("Qdrant 컬렉션 생성: {}", collection);
    }

    /**
     * songs 테이블 전체(AUDIO_9D, embedding_songs와 JOIN된 것만)를 Qdrant에 upsert한다.
     * 기동 시 자동 동기화(QdrantStartupSync)와 수동 트리거(POST /api/qdrant/embed)가
     * 이 메서드를 공유한다. 실패 시 예외를 그대로 던져 호출자가 재시도할 수 있게 한다.
     *
     * @return Qdrant에 실제로 upsert된 곡 수
     */
    public int embedAllAudioSongs() throws Exception {
        if (!enabled) {
            log.info("Qdrant disabled. Skip AUDIO_9D batch upsert.");
            return 0;
        }

        List<SongDto> songs = songService.getSongsWithEmbeddingMeta(AUDIO_9D);
        Map<String, List<Float>> vectorsBySongId = embeddingSongDao.selectByEmbeddingType(AUDIO_9D).stream()
            .collect(Collectors.toMap(EmbeddingSongDto::getSongId, EmbeddingSongDto::getVector));

        createCollectionInternal();
        return upsertSongsInternal(songs, vectorsBySongId);
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

    /**
     * embedding_songs.vector(AUDIO_9D)에 저장된 벡터를 우선 사용하고, 없는 곡만
     * SongVectorUtil로 즉석 계산한다(answer16.md 4.1 — 재계산과 시드 계산 로직이
     * 어긋날 여지를 없애기 위해 저장된 값을 신뢰).
     */
    public void upsertSongs(List<SongDto> songs, Map<String, List<Float>> vectorsBySongId) {
        if (!enabled) {
            log.info("Qdrant disabled. Skip batch upsert: {} songs", songs.size());
            return;
        }

        try {
            int upserted = upsertSongsInternal(songs, vectorsBySongId);
            log.info("Qdrant upsert 완료: {}곡", upserted);
        } catch (Exception e) {
            log.warn("Qdrant batch upsert 실패. fallback 사용 예정: {}", e.getMessage());
        }
    }

    private int upsertSongsInternal(List<SongDto> songs, Map<String, List<Float>> vectorsBySongId) throws Exception {
        List<PointStruct> points = songs.stream()
            .flatMap(s -> {
                float[] vec = toStoredVector(vectorsBySongId.get(s.getId()));
                if (vec == null) vec = SongVectorUtil.toVector(s);
                if (vec == null) return Stream.empty();
                return Stream.of(PointStruct.newBuilder()
                    .setId(id(UUID.fromString(s.getId())))
                    .setVectors(vectors(vec))
                    .putAllPayload(buildPayload(s))
                    .build());
            })
            .toList();

        if (points.isEmpty()) return 0;
        qdrantClient.upsertAsync(collection, points).get();
        return points.size();
    }

    private float[] toStoredVector(List<Float> stored) {
        if (stored == null || stored.isEmpty()) return null;
        float[] vec = new float[stored.size()];
        for (int i = 0; i < stored.size(); i++) vec[i] = stored.get(i);
        return vec;
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
            if (isPointNotFound(e)) {
                // 시드 곡이 아직 Qdrant에 색인되지 않은, 예상 가능한 상태(서버 장애가 아님).
                // POST /api/qdrant/embed로 재색인하면 해소된다.
                log.debug("Qdrant point 없음(미색인 곡일 수 있음). songId={}", songId);
            } else {
                log.warn("Qdrant 검색 실패(연결/서버 오류 가능). songId={}, reason={}", songId, e.getMessage());
            }
            return List.of();
        }
    }

    private boolean isPointNotFound(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            String message = t.getMessage();
            if (message != null && message.contains("NOT_FOUND")) {
                return true;
            }
        }
        return false;
    }

    // Value 타입 충돌 방지: 완전한 클래스명 사용
    private Map<String, io.qdrant.client.grpc.JsonWithInt.Value> buildPayload(SongDto song) {
        Map<String, io.qdrant.client.grpc.JsonWithInt.Value> payload = new HashMap<>();
        if (song.getTitle()     != null) payload.put("title",      value(song.getTitle()));
        if (song.getGenre()     != null) payload.put("genre",      value(song.getGenre()));
        if (song.getEra()       != null) payload.put("era",        value(song.getEra()));
        if (song.getType()      != null) payload.put("type",       value(song.getType()));
        if (song.getYoutubeId() != null) payload.put("youtube_id", value(song.getYoutubeId()));

        // 표준 코드 필드(과도기). song_moods/generation/genre가 아직 채워지지 않은 곡은
        // 매칭되는 항목이 없어 필드 자체를 생략한다(기존 payload 구조를 깨지 않음).
        List<String> moodCodes = safeFindMoodCodes(song.getId());
        if (!moodCodes.isEmpty()) {
            payload.put("mood_codes", list(moodCodes.stream().map(io.qdrant.client.ValueFactory::value).toList()));
        }

        GenerationCode generationCode = toGenerationCode(song.getGeneration());
        if (generationCode != null) {
            payload.put("generation", value(generationCode.name()));
        }

        GenreNormalizer.normalize(song.getGenre())
                .ifPresent(genreCode -> payload.put("genre_code", value(genreCode.name())));

        return payload;
    }

    private List<String> safeFindMoodCodes(String songId) {
        if (!StringUtils.hasText(songId)) return List.of();
        try {
            return songDao.selectMoodCodesBySongId(songId);
        } catch (Exception e) {
            log.warn("song_moods 조회 실패. songId={}, reason={}", songId, e.getMessage());
            return List.of();
        }
    }

    private GenerationCode toGenerationCode(String generation) {
        if ("2세대".equals(generation)) return GenerationCode.SECOND;
        if ("3세대".equals(generation)) return GenerationCode.THIRD;
        return null;
    }
}
