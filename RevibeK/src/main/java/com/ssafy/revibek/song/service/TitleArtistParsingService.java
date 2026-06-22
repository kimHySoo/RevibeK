package com.ssafy.revibek.song.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.revibek.song.dto.SongDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TitleArtistParsingService {

    private final RestTemplate restTemplate;
    private final SongService songService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gms.openai.enabled:false}")
    private boolean enabled;

    @Value("${gms.openai.base-url:https://gms.ssafy.io/gmsapi/api.openai.com/v1/responses}")
    private String baseUrl;

    @Value("${gms.openai.api-key:}")
    private String apiKey;

    @Value("${gms.openai.model:gpt-4.1}")
    private String model;

    /**
     * artist가 "미분류"인 곡들의 title(유튜브 영상 제목)을 GPT로 분석해
     * 실제 곡 제목과 아티스트로 분리한다. API 호출에 실패한 곡은 그대로 둔다.
     */
    public int splitUnclassifiedTitles() {
        if (!enabled || !StringUtils.hasText(apiKey)) {
            log.info("[TitleParsing] GMS OpenAI API 비활성화 또는 키 없음. 건너뜀.");
            return 0;
        }

        int updatedCount = 0;
        for (SongDto song : songService.getAllSongs()) {
            if (!"미분류".equals(song.getArtist())) {
                continue;
            }

            TitleArtist parsed = parseTitleArtist(song.getTitle());
            if (parsed == null) {
                continue;
            }

            song.setTitle(parsed.title());
            song.setArtist("미분류".equals(parsed.artist()) ? "" : parsed.artist());
            songService.modifySong(song);
            updatedCount++;
        }

        return updatedCount;
    }

    private record TitleArtist(String title, String artist) {}

    /**
     * 1회용 디버그: artist가 "미분류"인 곡 중 하나만 가져와 GPT 응답 원본을 그대로 반환한다.
     */
    public Map<String, Object> debugParseOne() {
        if (!enabled || !StringUtils.hasText(apiKey)) {
            return Map.of("error", "GMS OpenAI API 비활성화 또는 키 없음");
        }

        SongDto song = songService.getAllSongs().stream()
            .filter(s -> "미분류".equals(s.getArtist()))
            .findFirst()
            .orElse(null);

        if (song == null) {
            return Map.of("error", "artist='미분류'인 곡이 없습니다.");
        }

        String prompt = buildPrompt(song.getTitle());
        Map<String, Object> response = callGpt(prompt);
        String text = extractOutputText(response);

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("songId", song.getId());
        result.put("rawTitle", song.getTitle());
        result.put("rawResponse", response);
        result.put("extractedText", text);
        return result;
    }

    private String buildPrompt(String rawTitle) {
        return """
            다음은 유튜브 영상 제목입니다. 여기서 실제 노래 제목과 아티스트(가수)를 추출해주세요.
            다른 설명 없이 JSON 형식으로만 답하세요: {"title": "곡 제목", "artist": "아티스트"}
            아티스트를 알 수 없으면 "미분류"로 표시하세요.

            영상 제목: "%s"
            """.formatted(rawTitle);
    }

    private Map<String, Object> callGpt(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        Map<String, Object> body = Map.of(
            "model", model,
            "input", prompt
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(
            baseUrl, new HttpEntity<>(body, headers), Map.class
        );
        return response;
    }

    private TitleArtist parseTitleArtist(String rawTitle) {
        try {
            String prompt = buildPrompt(rawTitle);
            Map<String, Object> response = callGpt(prompt);

            String text = extractOutputText(response);
            if (text == null) {
                return null;
            }

            text = text.trim();
            if (text.startsWith("```")) {
                text = text.replaceAll("^```[a-zA-Z]*\\n", "").replaceAll("```$", "").trim();
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue(text, Map.class);
            String title = (String) parsed.get("title");
            String artist = (String) parsed.get("artist");
            if (title == null || artist == null) {
                return null;
            }

            return new TitleArtist(title, artist);
        } catch (Exception e) {
            log.warn("[TitleParsing] 파싱 실패: {} - {}", rawTitle, e.getMessage());
            return null;
        }
    }

    private String extractOutputText(Map<String, Object> response) {
        if (response == null) {
            return null;
        }

        Object output = response.get("output");
        if (!(output instanceof List<?> outputList)) {
            return null;
        }

        for (Object item : outputList) {
            if (!(item instanceof Map<?, ?> itemMap)) continue;
            if (!"message".equals(itemMap.get("type"))) continue;

            Object content = itemMap.get("content");
            if (!(content instanceof List<?> contentList)) continue;

            for (Object c : contentList) {
                if (c instanceof Map<?, ?> cMap && "output_text".equals(cMap.get("type"))) {
                    Object text = cMap.get("text");
                    if (text != null) return text.toString();
                }
            }
        }

        return null;
    }
}
