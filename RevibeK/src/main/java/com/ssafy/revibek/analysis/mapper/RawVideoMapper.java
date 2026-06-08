package com.ssafy.revibek.analysis.mapper;

import com.ssafy.revibek.analysis.dto.RawVideoDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RawVideoMapper {
    List<RawVideoDto> selectRawVideos(@Param("limit") int limit);
    void updateIsAnalyzed(@Param("videoId") String videoId);
}
