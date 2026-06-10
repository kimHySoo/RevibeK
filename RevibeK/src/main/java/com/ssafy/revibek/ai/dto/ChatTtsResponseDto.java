package com.ssafy.revibek.ai.dto;

public record ChatTtsResponseDto(
    String text,
    String ttsMode,
    String audioUrl,
    String audioContentBase64,
    String contentType,
    String languageCode,
    String voiceName,
    String audioEncoding,
    double requestCostCredit,
    double totalSpentCredit,
    double remainingBudgetCredit
) {
    public ChatTtsResponseDto(
        String text,
        String audioContentBase64,
        String contentType,
        String languageCode,
        String voiceName,
        String audioEncoding,
        double requestCostCredit,
        double totalSpentCredit,
        double remainingBudgetCredit
    ) {
        this(
            text,
            audioContentBase64 == null ? "BROWSER_TTS" : "GOOGLE_TTS",
            null,
            audioContentBase64,
            contentType,
            languageCode,
            voiceName,
            audioEncoding,
            requestCostCredit,
            totalSpentCredit,
            remainingBudgetCredit
        );
    }
}
