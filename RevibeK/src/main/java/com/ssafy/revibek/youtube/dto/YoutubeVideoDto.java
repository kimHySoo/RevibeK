// com.ssafy.revibek.youtube.dto.YoutubeVideoDto.java
package com.ssafy.revibek.youtube.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class YoutubeVideoDto {
    private String id;
    private String channelId;
    private String videoId;
    private String videoUrl;
    private String title;
    private Integer durationSeconds;
    private String publishedAt;
    private Integer isImported;
    private Integer isAnalyzed;
    private String collectStatus;
    private String fetchedAt;
    private String updatedAt;
}