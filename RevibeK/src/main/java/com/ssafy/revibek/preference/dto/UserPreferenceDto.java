package com.ssafy.revibek.preference.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferenceDto {

    private String id;
    private String userId;
    private List<String> preferredGenerations;
    private List<String> preferredMoods;
    private List<String> preferredArtists;
    private List<String> preferredGenres;
    private List<String> preferredVideoTypes;
    private List<String> excludedGenres;
    private List<String> excludedKeywords;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @JsonIgnore
    private String preferredGenerationsJson;

    @JsonIgnore
    private String preferredMoodsJson;

    @JsonIgnore
    private String preferredArtistsJson;

    @JsonIgnore
    private String preferredGenresJson;

    @JsonIgnore
    private String preferredVideoTypesJson;

    @JsonIgnore
    private String excludedGenresJson;

    @JsonIgnore
    private String excludedKeywordsJson;
}
