// com.ssafy.revibek.youtube.service.YoutubeService.java
package com.ssafy.revibek.youtube.service;

import com.ssafy.revibek.youtube.dto.YoutubeFallbackResponseDto;

public interface YoutubeService {

    default void processChannel(String channelUrl) {
        processChannelWithResponse(channelUrl);
    }

    YoutubeFallbackResponseDto processChannelWithResponse(String channelUrl);
}