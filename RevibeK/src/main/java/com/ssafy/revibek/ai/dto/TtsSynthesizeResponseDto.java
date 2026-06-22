package com.ssafy.revibek.ai.dto;

public record TtsSynthesizeResponseDto(
    String mode,
    String text,
    String audioUrl,
    String audioContentBase64,
    String contentType,
    String languageCode,
    String voiceName,
    String audioEncoding
) {
    public TtsSynthesizeResponseDto(
        String audioContentBase64,
        String contentType,
        String languageCode,
        String voiceName,
        String audioEncoding
    ) {
        this("GOOGLE_TTS", null, null, audioContentBase64, contentType, languageCode, voiceName, audioEncoding);
    }

    public static TtsSynthesizeResponseDto browserTts(String text, String languageCode, String voiceName) {
        return new TtsSynthesizeResponseDto(
            "BROWSER_TTS",
            text,
            null,
            null,
            "text/plain",
            languageCode,
            voiceName,
            null
        );
    }
}
