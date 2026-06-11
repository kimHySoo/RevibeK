package com.ssafy.revibek.radio.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RadioCreateRequestDto {

    private String title;
    private String mood;
    private String situation;
    private String desiredMood;
    private String story;
    private String era;
    private String genre;
    private String videoType;
    private String preferredArtist;
    private String excludedKeywords;
    private Boolean saveAsPlaylist;
    private List<RadioSelectedSongDto> selectedSongs = new ArrayList<>();
}