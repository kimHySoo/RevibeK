package com.ssafy.revibek.analysis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnalyzeResponseDto {
    @JsonProperty("youtube_video_id")
    private String youtubeVideoId;

    private String title;
    private String status;
    private String message;

    @JsonProperty("audio_path")
    private String audioPath;

    @JsonProperty("duration_seconds")
    private int durationSeconds;

    private Double bpm;
    private Double energy;
    private Double danceability;
    private Double loudness;

    @JsonProperty("musical_key")
    private String musicalKey;

    @JsonProperty("musical_scale")
    private String musicalScale;
}
