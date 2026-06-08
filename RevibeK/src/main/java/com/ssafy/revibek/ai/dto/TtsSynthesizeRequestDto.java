package com.ssafy.revibek.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record TtsSynthesizeRequestDto(
    @NotBlank(message = "text는 필수입니다.")
    String text,
    String languageCode,
    String voiceName,
    @Min(value = 0, message = "speakingRate는 0 이상이어야 합니다.")
    @Max(value = 4, message = "speakingRate는 4 이하여야 합니다.")
    Double speakingRate,
    @Min(value = -20, message = "pitch는 -20 이상이어야 합니다.")
    @Max(value = 20, message = "pitch는 20 이하여야 합니다.")
    Double pitch,
    String audioEncoding
) {
}
