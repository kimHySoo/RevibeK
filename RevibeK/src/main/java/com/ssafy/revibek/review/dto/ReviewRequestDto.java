package com.ssafy.revibek.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReviewRequestDto {

    @NotBlank(message = "songId는 필수입니다.")
    private String songId;

    @NotBlank(message = "내용은 필수입니다.")
    private String content;

    @Min(1) @Max(5)
    private Integer rating;
}
