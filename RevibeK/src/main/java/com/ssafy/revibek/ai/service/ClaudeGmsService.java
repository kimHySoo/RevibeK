package com.ssafy.revibek.ai.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.ssafy.revibek.ai.dto.AiChatResponseDto;
import com.ssafy.revibek.ai.dto.external.ClaudeMessageRequestDto;
import com.ssafy.revibek.ai.dto.external.ClaudeMessageResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClaudeGmsService {

    private final RestClient restClient = RestClient.create();
    private final GmsCreditBudgetTracker budgetTracker;

    @Value("${gms.enabled:false}")
    private boolean enabled;

    @Value("${gms.api.base-url:}")
    private String baseUrl;

    @Value("${gms.api.key:}")
    private String apiKey;

    @Value("${gms.api.anthropic-version:2023-06-01}")
    private String anthropicVersion;

    @Value("${gms.api.model:claude-3-7-sonnet-latest}")
    private String defaultModel;

    @Value("${gms.api.max-tokens:300}")
    private int defaultMaxTokens;

    @Value("${gms.api.budget-credit:30}")
    private BigDecimal budgetCredit;

    @Value("${gms.api.input-cost-per-1k:0.03}")
    private BigDecimal inputCostPer1k;

    @Value("${gms.api.output-cost-per-1k:0.15}")
    private BigDecimal outputCostPer1k;

    public AiChatResponseDto generateText(String prompt, String system, Integer maxTokens) {
        if (!enabled || !StringUtils.hasText(apiKey) || !StringUtils.hasText(baseUrl)) {
            return fallbackResponse(prompt);
        }
        if (budgetTracker.getSpent().compareTo(budgetCredit) >= 0) {
            return fallbackResponse(prompt);
        }

        ClaudeMessageRequestDto request = new ClaudeMessageRequestDto(
            defaultModel,
            maxTokens != null ? maxTokens : defaultMaxTokens,
            List.of(new ClaudeMessageRequestDto.Message("user", prompt)),
            StringUtils.hasText(system) ? system : null
        );

        ClaudeMessageResponseDto response;
        try {
            response = restClient
                .post()
                .uri(baseUrl)
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", anthropicVersion)
                .body(request)
                .retrieve()
                .body(ClaudeMessageResponseDto.class);
        } catch (RestClientResponseException e) {
            return fallbackResponse(prompt);
        }

        if (response == null || response.getContent() == null || response.getContent().isEmpty()) {
            return fallbackResponse(prompt);
        }

        String text = response.getContent().stream()
            .filter(content -> "text".equals(content.getType()))
            .map(ClaudeMessageResponseDto.ContentBlock::getText)
            .filter(StringUtils::hasText)
            .findFirst()
            .orElse(null);

        if (!StringUtils.hasText(text)) {
            return fallbackResponse(prompt);
        }

        long inputTokens = response.getUsage() == null ? 0L : response.getUsage().getInputTokens();
        long outputTokens = response.getUsage() == null ? 0L : response.getUsage().getOutputTokens();
        BigDecimal requestCost = calculateRequestCost(inputTokens, outputTokens);
        BigDecimal totalSpent = budgetTracker.add(requestCost);
        BigDecimal remainingBudget = budgetCredit.subtract(totalSpent);
        if (remainingBudget.compareTo(BigDecimal.ZERO) < 0) {
            remainingBudget = BigDecimal.ZERO;
        }

        return new AiChatResponseDto(
            text,
            requestCost.doubleValue(),
            totalSpent.doubleValue(),
            remainingBudget.doubleValue()
        );
    }

    private BigDecimal calculateRequestCost(long inputTokens, long outputTokens) {
        BigDecimal inputCost = BigDecimal.valueOf(inputTokens)
            .multiply(inputCostPer1k)
            .divide(BigDecimal.valueOf(1000L), 6, RoundingMode.HALF_UP);
        BigDecimal outputCost = BigDecimal.valueOf(outputTokens)
            .multiply(outputCostPer1k)
            .divide(BigDecimal.valueOf(1000L), 6, RoundingMode.HALF_UP);
        return inputCost.add(outputCost);
    }

    private AiChatResponseDto fallbackResponse(String prompt) {
        String text = "오늘의 RevibeK DJ입니다. 지금은 AI 연동이 비활성화되어 있어 기본 멘트로 진행할게요. "
            + "입력해주신 분위기에 맞춰 편안하게 들을 수 있는 음악을 준비하겠습니다.";
        if (StringUtils.hasText(prompt)) {
            text = text + " 잠시 숨을 고르고, 음악과 함께 천천히 이어가 봐요.";
        }
        BigDecimal remainingBudget = budgetCredit.subtract(budgetTracker.getSpent());
        if (remainingBudget.compareTo(BigDecimal.ZERO) < 0) {
            remainingBudget = BigDecimal.ZERO;
        }
        return new AiChatResponseDto(
            text,
            0.0,
            budgetTracker.getSpent().doubleValue(),
            remainingBudget.doubleValue()
        );
    }
}
