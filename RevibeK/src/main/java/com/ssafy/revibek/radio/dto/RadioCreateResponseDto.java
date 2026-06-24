package com.ssafy.revibek.radio.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RadioCreateResponseDto {

    private String radioSessionId;
    private String playlistId;
    private String userId;
    private String mood;
    private String story;
    private String generation;
    private String genre;
    private String situation;
    private String desiredMood;
    private String videoType;
    private String preferredArtist;
    private String excludedKeywords;
    private String djMent;
    private String recommendationSource;
    private TtsFallbackResponseDto tts;
    private List<RecommendedSongResponseDto> recommendedSongs;
}
