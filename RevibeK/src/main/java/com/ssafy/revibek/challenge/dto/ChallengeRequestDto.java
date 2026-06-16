package com.ssafy.revibek.challenge.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChallengeRequestDto {

    @NotBlank(message = "챌린지 제목은 필수입니다.")
    private String title;

    private String description;

    private String startDate;

    private String endDate;

    @Min(1)
    private int targetCount;

    private String challengeType;
}
