package com.ssafy.revibek.youtube.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YoutubeFallbackResponseDto {

    private String source;
    private String message;
    private List<YoutubeVideoResponseDto> videos;
}
