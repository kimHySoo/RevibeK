package com.ssafy.revibek.youtube.mapper;

import com.ssafy.revibek.youtube.dto.YoutubeChannelDto;
import com.ssafy.revibek.youtube.dto.YoutubeVideoDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface YoutubeMapper {

    // Channel CRUD
    void insertChannel(YoutubeChannelDto channel);
    YoutubeChannelDto findChannelByChannelId(String channelId);
    String findChannelIdByChannelId(String channelId);
    List<YoutubeChannelDto> findAllChannels();
    void updateChannel(YoutubeChannelDto channel);
    void deleteChannel(String channelId);

    // Video CRUD
    void insertVideo(YoutubeVideoDto video);
    YoutubeVideoDto findVideoByVideoId(String videoId);
    List<YoutubeVideoDto> findVideosByChannelId(String channelId);
    List<YoutubeVideoDto> findPendingVideos();  // FastAPI 연동용
    void updateVideoStatus(@Param("videoId") String videoId, @Param("status") String status);
    void deleteVideo(String videoId);
}