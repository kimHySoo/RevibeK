package com.ssafy.revibek.analysis.dto;

import lombok.Data;

@Data
public class RawVideoDto {
    private String videoId;
    private String videoUrl;
    private String videoTitle;
    private Integer durationSeconds;
}
