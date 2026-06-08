package com.ssafy.revibek.ai.dao;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.ssafy.revibek.ai.dto.external.GoogleTtsSynthesizeRequestDto;
import com.ssafy.revibek.ai.dto.external.GoogleTtsSynthesizeResponseDto;
import com.ssafy.revibek.ai.dto.external.GoogleTtsVoicesResponseDto;

@Repository
public class GoogleTtsRestDao implements GoogleTtsDao {

    private final RestClient restClient = RestClient.create();

    @Value("${gcp.tts.base-url:https://texttospeech.googleapis.com/v1}")
    private String baseUrl;

    @Value("${gcp.tts.api-key:}")
    private String apiKey;

    @Override
    public GoogleTtsSynthesizeResponseDto synthesize(GoogleTtsSynthesizeRequestDto requestDto) {
        String url = UriComponentsBuilder.fromUriString(baseUrl)
            .path("/text:synthesize")
            .queryParam("key", requireApiKey())
            .toUriString();
        try {
            return restClient.post()
                .uri(url)
                .header("Content-Type", "application/json")
                .body(requestDto)
                .retrieve()
                .body(GoogleTtsSynthesizeResponseDto.class);
        } catch (RestClientResponseException e) {
            throw new RuntimeException("Google TTS synthesize 호출 실패: " + e.getResponseBodyAsString(), e);
        }
    }

    @Override
    public GoogleTtsVoicesResponseDto listVoices(String languageCode) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl)
            .path("/voices")
            .queryParam("key", requireApiKey());
        if (StringUtils.hasText(languageCode)) {
            builder.queryParam("languageCode", languageCode);
        }
        try {
            return restClient.get()
                .uri(builder.toUriString())
                .retrieve()
                .body(GoogleTtsVoicesResponseDto.class);
        } catch (RestClientResponseException e) {
            throw new RuntimeException("Google TTS voices 조회 실패: " + e.getResponseBodyAsString(), e);
        }
    }

    private String requireApiKey() {
        if (!StringUtils.hasText(apiKey)) {
            throw new RuntimeException("Google TTS API 키(gcp.tts.api-key)가 비어 있습니다.");
        }
        return apiKey;
    }
}
