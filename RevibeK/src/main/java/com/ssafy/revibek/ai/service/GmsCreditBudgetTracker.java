package com.ssafy.revibek.ai.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

@Component
public class GmsCreditBudgetTracker {

    private BigDecimal spent = BigDecimal.ZERO;

    public synchronized BigDecimal getSpent() {
        return spent;
    }

    public synchronized BigDecimal add(BigDecimal requestCost) {
        spent = spent.add(requestCost);
        return spent;
    }
}
