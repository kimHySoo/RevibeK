package com.ssafy.revibek.analysis.client;

import com.ssafy.revibek.analysis.dto.AnalyzeRequestDto;
import com.ssafy.revibek.analysis.dto.AnalyzeResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class FastApiClient {

    @Value("${fastapi.enabled:false}")
    private boolean enabled;

    @Value("${fastapi.host:http://localhost:8000}")
    private String fastApiHost;

    private final RestTemplate restTemplate = new RestTemplate();

    public AnalyzeResponseDto analyze(AnalyzeRequestDto request) {
        if (!enabled) {
            return mockAnalyze(request, "FastAPI is disabled. Using mock analysis result.");
        }

        String url = fastApiHost + "/api/ai/analyze";
        try {
            ResponseEntity<AnalyzeResponseDto> response =
                restTemplate.postForEntity(url, request, AnalyzeResponseDto.class);
            AnalyzeResponseDto body = response.getBody();
            if (body != null && body.getSource() == null) {
                body.setSource("fastapi");
            }
            return body != null ? body : mockAnalyze(request, "FastAPI returned an empty response. Using mock analysis result.");
        } catch (Exception e) {
            return mockAnalyze(request, "FastAPI unavailable. Using mock analysis result.");
        }
    }

    private AnalyzeResponseDto mockAnalyze(AnalyzeRequestDto request, String message) {
        AnalyzeResponseDto response = new AnalyzeResponseDto();
        response.setYoutubeVideoId(request.getYoutubeVideoId());
        response.setTitle(request.getTitle() == null || request.getTitle().isBlank()
            ? request.getYoutubeVideoId()
            : request.getTitle());
        response.setStatus("MOCK");
        response.setSource("fallback");
        response.setMessage(message);
        response.setAudioPath(null);
        response.setDurationSeconds(request.getDurationSeconds());
        response.setBpm(120.0);
        response.setEnergy(0.6);
        response.setDanceability(0.6);
        response.setLoudness(-8.0);
        response.setMusicalKey("C");
        response.setMusicalScale("major");
        return response;
    }
}