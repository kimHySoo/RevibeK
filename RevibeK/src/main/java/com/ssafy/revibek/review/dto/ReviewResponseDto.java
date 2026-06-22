package com.ssafy.revibek.review.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ReviewResponseDto {
    private String id;
    private String userId;
    private String userNickname;
    private String songId;
    private String content;
    private Integer rating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
