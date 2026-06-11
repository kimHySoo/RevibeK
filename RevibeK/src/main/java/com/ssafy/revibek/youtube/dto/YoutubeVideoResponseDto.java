package com.ssafy.revibek.youtube.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YoutubeVideoResponseDto {

    private String title;
    private String youtubeId;
    private String youtubeUrl;
    private String thumbnailUrl;
}
