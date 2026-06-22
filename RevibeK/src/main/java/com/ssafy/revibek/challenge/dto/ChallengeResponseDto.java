package com.ssafy.revibek.challenge.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ChallengeResponseDto {
    private String id;
    private String creatorId;
    private String title;
    private String description;
    private String startDate;
    private String endDate;
    private int targetCount;
    private String challengeType;
    private LocalDateTime createdAt;
}
