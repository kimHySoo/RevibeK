package com.ssafy.revibek.preference.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.revibek.preference.dto.UserPreferenceDto;
import com.ssafy.revibek.preference.dto.UserPreferenceRequestDto;
import com.ssafy.revibek.preference.mapper.PreferenceMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PreferenceService {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final PreferenceMapper preferenceMapper;
    private final ObjectMapper objectMapper;

    @Transactional
    public UserPreferenceDto savePreference(String userId, UserPreferenceRequestDto request) {
        validateUserId(userId);
        UserPreferenceDto preference = toDto(userId, request);
        preferenceMapper.upsertUserPreference(preference);
        return getPreference(userId);
    }

    @Transactional(readOnly = true)
    public UserPreferenceDto getPreference(String userId) {
        validateUserId(userId);
        UserPreferenceDto preference = preferenceMapper.selectByUserId(userId);
        if (preference == null) {
            return emptyPreference(userId);
        }
        hydrateLists(preference);
        return preference;
    }

    @Transactional
    public void deletePreference(String userId) {
        validateUserId(userId);
        preferenceMapper.deleteByUserId(userId);
    }

    public String firstPreferredGeneration(UserPreferenceDto preference) {
        return first(preference.getPreferredGenerations());
    }

    public String firstPreferredMood(UserPreferenceDto preference) {
        return first(preference.getPreferredMoods());
    }

    public String firstPreferredGenre(UserPreferenceDto preference) {
        return first(preference.getPreferredGenres());
    }

    public String firstPreferredArtist(UserPreferenceDto preference) {
        return first(preference.getPreferredArtists());
    }

    public String firstPreferredVideoType(UserPreferenceDto preference) {
        return first(preference.getPreferredVideoTypes());
    }

    public String joinedExcludedKeywords(UserPreferenceDto preference) {
        return join(preference.getExcludedKeywords());
    }

    private UserPreferenceDto toDto(String userId, UserPreferenceRequestDto request) {
        return UserPreferenceDto.builder()
            .id(UUID.randomUUID().toString())
            .userId(userId)
            .preferredGenerations(safeList(request.getPreferredGenerations()))
            .preferredMoods(safeList(request.getPreferredMoods()))
            .preferredArtists(safeList(request.getPreferredArtists()))
            .preferredGenres(safeList(request.getPreferredGenres()))
            .preferredVideoTypes(safeList(request.getPreferredVideoTypes()))
            .excludedGenres(safeList(request.getExcludedGenres()))
            .excludedKeywords(safeList(request.getExcludedKeywords()))
            .preferredGenerationsJson(toJson(safeList(request.getPreferredGenerations())))
            .preferredMoodsJson(toJson(safeList(request.getPreferredMoods())))
            .preferredArtistsJson(toJson(safeList(request.getPreferredArtists())))
            .preferredGenresJson(toJson(safeList(request.getPreferredGenres())))
            .preferredVideoTypesJson(toJson(safeList(request.getPreferredVideoTypes())))
            .excludedGenresJson(toJson(safeList(request.getExcludedGenres())))
            .excludedKeywordsJson(toJson(safeList(request.getExcludedKeywords())))
            .build();
    }

    private UserPreferenceDto emptyPreference(String userId) {
        return UserPreferenceDto.builder()
            .userId(userId)
            .preferredGenerations(List.of())
            .preferredMoods(List.of())
            .preferredArtists(List.of())
            .preferredGenres(List.of())
            .preferredVideoTypes(List.of())
            .excludedGenres(List.of())
            .excludedKeywords(List.of())
            .build();
    }

    private void hydrateLists(UserPreferenceDto preference) {
        preference.setPreferredGenerations(fromJson(preference.getPreferredGenerationsJson()));
        preference.setPreferredMoods(fromJson(preference.getPreferredMoodsJson()));
        preference.setPreferredArtists(fromJson(preference.getPreferredArtistsJson()));
        preference.setPreferredGenres(fromJson(preference.getPreferredGenresJson()));
        preference.setPreferredVideoTypes(fromJson(preference.getPreferredVideoTypesJson()));
        preference.setExcludedGenres(fromJson(preference.getExcludedGenresJson()));
        preference.setExcludedKeywords(fromJson(preference.getExcludedKeywordsJson()));
    }

    private List<String> safeList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
            .filter(StringUtils::hasText)
            .map(String::trim)
            .distinct()
            .toList();
    }

    private String toJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<String> fromJson(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST_TYPE);
        } catch (Exception e) {
            return List.of();
        }
    }

    private String first(List<String> values) {
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    private String join(List<String> values) {
        return values == null || values.isEmpty() ? null : String.join(",", values);
    }

    private void validateUserId(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("사용자 ID가 필요합니다.");
        }
    }
}
