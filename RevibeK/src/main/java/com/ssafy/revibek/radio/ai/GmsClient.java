package com.ssafy.revibek.radio.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class GmsClient {

    private final RestTemplate restTemplate;

    @Value("${gms.enabled:false}")
    private boolean enabled;

    @Value("${gms.api.base-url:}")
    private String baseUrl;

    @Value("${gms.api.key:}")
    private String apiKey;

    // SSAFY GMS 프록시는 일부 모델명을 허용하지 않는다(예: claude-3-7-sonnet-latest -> 400).
    // 실제 호출 테스트로 확인된 사용 가능한 모델만 기본값으로 사용한다.
    @Value("${gms.api.model:claude-sonnet-4-5-20250929}")
    private String model;

    @Value("${gms.api.anthropic-version:2023-06-01}")
    private String anthropicVersion;

    @Value("${gms.api.max-tokens:300}")
    private int maxTokens;

    public Optional<String> generate(String prompt) {
        if (!enabled) {
            log.debug("[GMS] gms.enabled=false. Using fallback DJ ment.");
            return Optional.empty();
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            log.warn("[GMS] gms.api.base-url is missing while gms.enabled=true. Using fallback DJ ment.");
            return Optional.empty();
        }
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[GMS] gms.api.key is missing while gms.enabled=true. Using fallback DJ ment.");
            return Optional.empty();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", apiKey);
            headers.set("anthropic-version", anthropicVersion);

            Map<String, Object> body = Map.of(
                    "model", model,
                    "max_tokens", maxTokens,
                    "messages", List.of(Map.of(
                            "role", "user",
                            "content", prompt
                    ))
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    baseUrl,
                    new HttpEntity<>(body, headers),
                    Map.class
            );

            Optional<String> text = extractText(response);
            if (text.isEmpty()) {
                log.warn("[GMS] Claude response had no usable content. Using fallback DJ ment.");
            } else {
                log.debug("[GMS] Claude DJ ment generated successfully. length={}", text.get().length());
            }
            return text;
        } catch (Exception e) {
            log.warn("[GMS] Claude API call failed. Using fallback DJ ment. reason={}", e.getMessage());
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private Optional<String> extractText(Map<String, Object> response) {
        if (response == null) {
            return Optional.empty();
        }

        Object content = response.get("content");
        if (content instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            if (first instanceof Map<?, ?> map) {
                Object text = map.get("text");
                if (text != null && !text.toString().isBlank()) {
                    return Optional.of(text.toString().trim());
                }
            } else if (first != null && !first.toString().isBlank()) {
                return Optional.of(first.toString().trim());
            }
        }

        for (String key : List.of("text", "message", "output")) {
            Object value = response.get(key);
            if (value != null && !value.toString().isBlank()) {
                return Optional.of(value.toString().trim());
            }
        }

        return Optional.empty();
    }
}