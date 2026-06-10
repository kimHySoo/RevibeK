package com.ssafy.revibek.ai.dto;

public record AiChatResponseDto(
    String text,
    double requestCostCredit,
    double totalSpentCredit,
    double remainingBudgetCredit
) {
}
