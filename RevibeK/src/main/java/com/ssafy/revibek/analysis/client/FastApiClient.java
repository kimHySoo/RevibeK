package com.ssafy.revibek.analysis.client;

import com.ssafy.revibek.analysis.dto.AnalyzeRequestDto;
import com.ssafy.revibek.analysis.dto.AnalyzeResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class FastApiClient {

    @Value("${fastapi.host:http://localhost:8000}")
    private String fastApiHost;

    private final RestTemplate restTemplate = new RestTemplate();

    public AnalyzeResponseDto analyze(AnalyzeRequestDto request) {
        String url = fastApiHost + "/api/ai/analyze";
        try {
            ResponseEntity<AnalyzeResponseDto> response =
                restTemplate.postForEntity(url, request, AnalyzeResponseDto.class);
            return response.getBody();
        } catch (Exception e) {
            // FastAPI 호출 실패 시 FAILED 상태 반환
            AnalyzeResponseDto failed = new AnalyzeResponseDto();
            return failed;
        }
    }
}