package com.ssafy.revibek.analysis.dto;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@NoArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnalyzeResponseDto {
    private String youtubeVideoId;
    private String title;
    private String status;        // COMPLETED | SKIPPED | FAILED
    private String message;
    private String audioPath;
    private int durationSeconds;
    private Double bpm;
    private Double energy;
    private Double danceability;
    private Double loudness;
    private String musicalKey;
    private String musicalScale;
}