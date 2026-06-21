package com.ssafy.revibek.embedding.mapper;

import com.ssafy.revibek.embedding.dto.EmbeddingSongDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EmbeddingSongDao {
    int upsert(EmbeddingSongDto embeddingSong);
    EmbeddingSongDto selectBySongIdAndType(@Param("songId") String songId, @Param("embeddingType") String embeddingType);
}
