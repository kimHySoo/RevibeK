package com.ssafy.revibek.challenge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeTemplateDto {
    private String templateId;
    private String challengeType;
    private String title;
    private String description;
    private int targetCount;
    private int durationDays;
    private String badgeText;
    private String recommendedMood;
    private String recommendedGeneration;
}
