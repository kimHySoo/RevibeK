package com.ssafy.revibek.plan.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PlanRequestDto {

    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    private String description;

    private String planDate;

    private String planType;
}
