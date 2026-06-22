package com.ssafy.revibek.like.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.revibek.like.dto.LikeDto;
import com.ssafy.revibek.song.dto.SongDto;

@Mapper
public interface LikeMapper {

    int insertLike(@Param("userId") String userId,
                   @Param("songId") String songId);

    int deleteLike(@Param("userId") String userId,
                   @Param("songId") String songId);

    int existsLike(@Param("userId") String userId,
                   @Param("songId") String songId);

    int countLikesBySongId(@Param("songId") String songId);

    List<LikeDto> selectLikesByUserId(@Param("userId") String userId);

    List<SongDto> selectLikedSongsByUserId(@Param("userId") String userId);

    int increaseSongLikeCount(@Param("songId") String songId);

    int decreaseSongLikeCount(@Param("songId") String songId);
}
