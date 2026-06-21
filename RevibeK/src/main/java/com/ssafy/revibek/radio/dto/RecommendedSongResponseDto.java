package com.ssafy.revibek.radio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendedSongResponseDto {

    private String songId;
    private String title;
    private String artist;
    private String era;
    private String genre;
    private String youtubeUrl;
    private String youtubeId;
    private String reason;
}
