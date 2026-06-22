package com.ssafy.revibek.plan.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class PlanResponseDto {
    private String id;
    private String userId;
    private String title;
    private String description;
    private String planDate;
    private String planType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
