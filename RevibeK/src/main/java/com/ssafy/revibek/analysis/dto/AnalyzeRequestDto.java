package com.ssafy.revibek.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AnalyzeRequestDto {
    private String youtubeVideoId;
    private String youtubeUrl;
    private String title;
    private int durationSeconds;
}