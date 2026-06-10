package com.ssafy.revibek.ai.dto;

public record ChatTtsResponseDto(
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
}
