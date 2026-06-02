package com.ssafy.revibek.youtube.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class YoutubeChannelDto {
 private String channelId;
 private String channelName;
 private String channelUrl;
}