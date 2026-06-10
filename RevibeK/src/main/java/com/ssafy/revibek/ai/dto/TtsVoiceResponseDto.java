package com.ssafy.revibek.ai.dto;

import java.util.List;

public record TtsVoiceResponseDto(
    String name,
    List<String> languageCodes,
    String ssmlGender,
    Integer naturalSampleRateHertz
) {
}
