package com.ssafy.revibek.ai.dto;

public record TtsSynthesizeResponseDto(
    String audioContentBase64,
    String contentType,
    String languageCode,
    String voiceName,
    String audioEncoding
) {
}
