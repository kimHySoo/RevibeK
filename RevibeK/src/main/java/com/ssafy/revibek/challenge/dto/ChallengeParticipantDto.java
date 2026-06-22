package com.ssafy.revibek.challenge.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ChallengeParticipantDto {
    private String challengeId;
    private String challengeTitle;
    private String userId;
    private int currentCount;
    private int targetCount;
    private String status;
    private LocalDateTime joinedAt;
}
