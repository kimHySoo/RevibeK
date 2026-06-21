package com.ssafy.revibek.embedding.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.revibek.embedding.dto.EmbeddingSongDto;
import com.ssafy.revibek.embedding.mapper.EmbeddingSongDao;
import com.ssafy.revibek.song.dto.SongDto;
import com.ssafy.revibek.song.service.SongService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class SongEmbeddingServiceImpl implements SongEmbeddingService {

    @Value("${fastapi.project.path:../RevibeK_AI}")
    private String fastApiProjectPath;

    @Value("${gms.openai.enabled:false}")
    private boolean enabled;

    @Value("${gms.openai.embedding-url:https://gms.ssafy.io/gmsapi/api.openai.com/v1/embeddings}")
    private String embeddingUrl;

    @Value("${gms.openai.embedding-model:text-embedding-3-small}")
    private String embeddingModel;

    @Value("${gms.openai.api-key:}")
    private String apiKey;

    @Value("${qdrant.text-collection:revibek_song_text_embeddings}")
    private String textCollection;

    private static final String EMBEDDING_TYPE = "TEXT_OPENAI";

    private final SongService songService;
    private final EmbeddingSongDao embeddingSongDao;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public int generateEmbeddings() {
        if (!enabled || !StringUtils.hasText(apiKey)) {
            log.info("[Embedding] GMS OpenAI API 비활성화 또는 키 없음. 건너뜀.");
            return 0;
        }

        File dir = new File(fastApiProjectPath, "song_embeddings");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        int generatedCount = 0;
        int backfilledCount = 0;
        for (SongDto song : songService.getAllSongs()) {
            if (embeddingSongDao.selectBySongIdAndType(song.getId(), EMBEDDING_TYPE) != null) {
                continue;
            }

            File file = new File(dir, song.getId() + ".json");
            if (file.exists()) {
                if (backfillFromExistingFile(song, file)) {
                    backfilledCount++;
                }
                continue;
            }

            String text = buildText(song);
            float[] vector = requestEmbedding(text);
            if (vector == null) {
                continue;
            }

            try {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("songId", song.getId());
                data.put("model", embeddingModel);
                data.put("text", text);
                data.put("vector", vector);
                objectMapper.writeValue(file, data);
                saveEmbeddingSong(song.getId(), text, vector.length, file);
                generatedCount++;
            } catch (IOException e) {
                log.warn("[Embedding] 파일 저장 실패: {} - {}", song.getId(), e.getMessage());
            }
        }

        log.info("[Embedding] 생성 완료: 신규 {}개, DB 백필 {}개", generatedCount, backfilledCount);
        return generatedCount + backfilledCount;
    }

    /**
     * 과거에 만들어진 song_embeddings/{songId}.json은 있지만 embedding_songs 행이 없는 곡을
     * GMS API 재호출 없이 기존 파일 내용만으로 DB에 채운다.
     */
    private boolean backfillFromExistingFile(SongDto song, File file) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(file, Map.class);
            @SuppressWarnings("unchecked")
            List<Number> vector = (List<Number>) data.get("vector");
            String text = (String) data.get("text");
            if (vector == null || vector.isEmpty()) {
                return false;
            }
            saveEmbeddingSong(song.getId(), text, vector.size(), file);
            return true;
        } catch (IOException e) {
            log.warn("[Embedding] 기존 파일 읽기 실패(백필 스킵): {} - {}", song.getId(), e.getMessage());
            return false;
        }
    }

    private void saveEmbeddingSong(String songId, String sourceText, int dimension, File file) {
        embeddingSongDao.upsert(EmbeddingSongDto.builder()
            .songId(songId)
            .embeddingType(EMBEDDING_TYPE)
            .modelName(embeddingModel)
            .dimension(dimension)
            .sourceText(sourceText)
            .vectorFilePath(file.getAbsolutePath())
            .qdrantCollection(textCollection)
            .qdrantPointId(songId)
            .isIndexed(false)
            .indexedAt(null)
            .build());
    }

    private String buildText(SongDto song) {
        return Stream.of(song.getTitle(), song.getArtist(), song.getGenre(), song.getMood(), song.getEra())
            .filter(StringUtils::hasText)
            .collect(Collectors.joining(" "));
    }

    private float[] requestEmbedding(String text) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> body = Map.of(
                "model", embeddingModel,
                "input", text
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                embeddingUrl, new HttpEntity<>(body, headers), Map.class
            );

            if (response == null) {
                return null;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
            if (data == null || data.isEmpty()) {
                return null;
            }

            @SuppressWarnings("unchecked")
            List<Number> embedding = (List<Number>) data.get(0).get("embedding");
            if (embedding == null) {
                return null;
            }

            float[] vector = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                vector[i] = embedding.get(i).floatValue();
            }
            return vector;
        } catch (Exception e) {
            log.warn("[Embedding] 임베딩 요청 실패: {} - {}", text, e.getMessage());
            return null;
        }
    }
}
