package com.ssafy.revibek.like.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikeDto {

    private String id;
    private String userId;

    @NotBlank(message = "songId는 필수입니다.")
    private String songId;

    private LocalDateTime createdAt;
}
