package com.ssafy.revibek.preference.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferenceRequestDto {

    private List<String> preferredGenerations;
    private List<String> preferredMoods;
    private List<String> preferredArtists;
    private List<String> preferredGenres;
    private List<String> preferredVideoTypes;
    private List<String> excludedGenres;
    private List<String> excludedKeywords;
}
