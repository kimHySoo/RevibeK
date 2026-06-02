package com.ssafy.revibek.youtube.dto;

import lombok.Data;

@Data
public class YoutubeVideoDto {
    private Long youtubeChannelId;
    private String videoId;
    private String videoUrl;
    private String videoTitle;
    private String publishedAt;
}