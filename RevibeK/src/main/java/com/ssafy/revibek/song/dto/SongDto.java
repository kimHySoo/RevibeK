package com.ssafy.revibek.song.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SongDto {

    private String id;
    private String title;
    private String artist;
    private String genre;
    private String era;
    private String type;
    private String youtubeUrl;
    private String youtubeId;
    private int viewCount;
    private int likeCount;
    private float trendScore;
    private float score;
    private LocalDateTime scoreUpdatedAt;
    private LocalDate releasedAt;
    private LocalDateTime createdAt;
    private String generation;
    private String mood;
    private String thumbnailUrl;
    private Integer durationSeconds;
    private Double bpm;
    private Double energy;
    private Double danceability;
    private Double loudness;
    private String musicalKey;
    private String musicalScale;
    private Integer beatsCount;
    private Double beatsConfidence;
    private Double keyStrength;
    private Double spectralCentroid;
    private Double zeroCrossingRate;
    private int isAnalyzed;
}
