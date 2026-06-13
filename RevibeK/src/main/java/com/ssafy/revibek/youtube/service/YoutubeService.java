// com.ssafy.revibek.youtube.service.YoutubeService.java
package com.ssafy.revibek.youtube.service;

import com.ssafy.revibek.youtube.dto.YoutubeFallbackResponseDto;
import com.ssafy.revibek.youtube.dto.YoutubeVideoStatsDto;

public interface YoutubeService {

    default void processChannel(String channelUrl) {
        processChannelWithResponse(channelUrl);
    }

    YoutubeFallbackResponseDto processChannelWithResponse(String channelUrl);

    /**
     * 영상의 썸네일/조회수/좋아요수를 조회한다.
     * API가 비활성화이거나 조회에 실패하면 null을 반환한다.
     */
    YoutubeVideoStatsDto fetchVideoStats(String videoId);
}