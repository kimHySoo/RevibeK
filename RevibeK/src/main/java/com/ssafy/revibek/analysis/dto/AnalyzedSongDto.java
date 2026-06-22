package com.ssafy.revibek.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyzedSongDto {

    private String id;
    private String youtubeVideoRawId;
    private String youtubeVideoId;
    private String songId;
    private String status;
    private String message;
    private Double durationSeconds;
    private Double bpm;
    private Double energy;
    private Double danceability;
    private Double loudness;
    private String musicalKey;
    private String musicalScale;
    private Double spectralCentroid;
    private Double zeroCrossingRate;
    private LocalDateTime analyzedAt;
    private LocalDateTime createdAt;
}
