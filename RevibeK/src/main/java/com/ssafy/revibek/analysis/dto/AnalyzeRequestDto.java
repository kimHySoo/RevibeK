package com.ssafy.revibek.analysis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AnalyzeRequestDto {
    @JsonProperty("youtube_video_id")
    private String youtubeVideoId;

    @JsonProperty("youtube_url")
    private String youtubeUrl;

    private String title;

    @JsonProperty("duration_seconds")
    private int durationSeconds;
}
