package com.ssafy.revibek.analysis.mapper;

import com.ssafy.revibek.analysis.dto.AnalyzedSongDto;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AnalyzedSongDao {
    int upsert(AnalyzedSongDto analyzedSong);
}
