package com.ssafy.revibek.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record AiChatRequestDto(
    @NotBlank(message = "prompt는 필수입니다.")
    String prompt,
    String system,
    @Min(value = 1, message = "maxTokens는 1 이상이어야 합니다.")
    @Max(value = 4096, message = "maxTokens는 4096 이하여야 합니다.")
    Integer maxTokens
) {
}
