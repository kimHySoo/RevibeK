package com.ssafy.revibek.embedding.mapper;

import java.util.List;

import com.ssafy.revibek.embedding.dto.EmbeddingSongDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EmbeddingSongDao {
    int upsert(EmbeddingSongDto embeddingSong);
    EmbeddingSongDto selectBySongIdAndType(@Param("songId") String songId, @Param("embeddingType") String embeddingType);
    List<EmbeddingSongDto> selectByEmbeddingType(@Param("embeddingType") String embeddingType);
}
