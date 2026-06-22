package com.ssafy.revibek.coaching.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CoachingResponseDto {
    private String emotionTendency;
    private String generationPreference;
    private String situationAdvice;
    private String recommendationDirection;
    private List<String> insightMessages;
    private String overallSummary;
}
