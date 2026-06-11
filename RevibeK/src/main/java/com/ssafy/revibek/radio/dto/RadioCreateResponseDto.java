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
    private String userId;
    private String mood;
    private String story;
    private String era;
    private String genre;
    private String situation;
    private String desiredMood;
    private String videoType;
    private String preferredArtist;
    private String excludedKeywords;
    private String djMent;
    private String playlistId;
    private String title;
    private String  djComment;
    private String recommendationSource;
    private TtsFallbackResponseDto tts;
    private List<RadioSongResponseDto> songs;
    private List<RecommendedSongResponseDto> recommendedSongs;
}
