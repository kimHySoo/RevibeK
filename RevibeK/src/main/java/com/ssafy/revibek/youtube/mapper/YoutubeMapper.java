package com.ssafy.revibek.youtube.mapper;

import com.ssafy.revibek.youtube.dto.YoutubeChannelDto;
import com.ssafy.revibek.youtube.dto.YoutubeVideoDto;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface YoutubeMapper {

    // Channel CRUD
    void insertChannel(YoutubeChannelDto channel);
    YoutubeChannelDto findChannelByChannelId(String channelId);
    Long findChannelIdByChannelId(String channelId);
    List<YoutubeChannelDto> findAllChannels();
    void updateChannel(YoutubeChannelDto channel);
    void deleteChannel(String channelId);

    // Video CRUD
    void insertVideo(YoutubeVideoDto video);
    YoutubeVideoDto findVideoByVideoId(String videoId);
    List<YoutubeVideoDto> findVideosByChannelId(Long channelId);
    List<YoutubeVideoDto> findPendingVideos();  // FastAPI 연동용
    void updateVideoStatus(String videoId, String status);
    void deleteVideo(String videoId);
}