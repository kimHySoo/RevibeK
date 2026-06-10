// com.ssafy.revibek.youtube.dto.YoutubeVideoDto.java
package com.ssafy.revibek.youtube.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class YoutubeVideoDto {
    private Long id;
    private Long youtubeChannelId;
    private String videoId;
    private String videoUrl;
    private String videoTitle;
    private Integer durationSeconds;
    private String publishedAt;
    private String collectStatus;
    private String createdAt;
    private String updatedAt;
}