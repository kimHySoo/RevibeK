package com.ssafy.revibek.analysis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnalyzeResponseDto {
    @JsonProperty("youtube_video_id")
    private String youtubeVideoId;

    private String title;
    private String status;
    private String source;
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

    @JsonProperty("spectral_centroid")
    private Double spectralCentroid;

    @JsonProperty("zero_crossing_rate")
    private Double zeroCrossingRate;

    /** FastAPI embedding_service가 생성한 9차원 벡터. null이면 SongVectorUtil 폴백 사용. */
    private List<Float> embedding;
}
