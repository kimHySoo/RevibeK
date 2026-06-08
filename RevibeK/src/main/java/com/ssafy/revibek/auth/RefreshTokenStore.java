package com.ssafy.revibek.auth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class RefreshTokenStore {

    private final Map<String, String> userRefreshTokenMap = new ConcurrentHashMap<>();

    public void save(String userId, String refreshToken) {
        userRefreshTokenMap.put(userId, refreshToken);
    }

    public boolean isValid(String userId, String refreshToken) {
        String storedToken = userRefreshTokenMap.get(userId);
        return storedToken != null && storedToken.equals(refreshToken);
    }

    public void revoke(String refreshToken) {
        userRefreshTokenMap.entrySet()
            .removeIf(entry -> entry.getValue().equals(refreshToken));
    }

    public void revokeByUserId(String userId) {
        userRefreshTokenMap.remove(userId);
    }
}
