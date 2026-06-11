package com.ssafy.revibek.radio.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RadioSelectedSongDto {

    private String songId;
    private String title;
    private String artist;
    private String youtubeUrl;
    private String thumbnailUrl;
    private String generation;
    private String genre;
    private String mood;



}
