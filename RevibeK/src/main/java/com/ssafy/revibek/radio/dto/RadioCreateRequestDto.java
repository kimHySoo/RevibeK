package com.ssafy.revibek.radio.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RadioCreateRequestDto {

    private String mood;
    private String story;
    private String era;
    private String genre;
    private String situation;
    private String desiredMood;
    private String videoType;
    private String preferredArtist;
    private String excludedKeywords;
    private String youtubeUrl;
}
