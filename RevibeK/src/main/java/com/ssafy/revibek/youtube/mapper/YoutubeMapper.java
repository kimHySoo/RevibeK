package com.ssafy.revibek.youtube.mapper;

import com.ssafy.revibek.youtube.dto.YoutubeChannelDto;
import com.ssafy.revibek.youtube.dto.YoutubeVideoDto;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface YoutubeMapper {
    void insertChannel(YoutubeChannelDto channel);
    Long findChannelIdByChannelId(String channelId);
    void insertVideo(YoutubeVideoDto video);
}