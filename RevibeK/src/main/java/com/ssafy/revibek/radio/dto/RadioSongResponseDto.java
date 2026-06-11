package com.ssafy.revibek.radio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RadioSongResponseDto {

    private String songId;
    private String title;
    private String artist;
    private String youtubeUrl;
    private String thumbnailUrl;
    private String source;
    private Integer sortOrder;


}
