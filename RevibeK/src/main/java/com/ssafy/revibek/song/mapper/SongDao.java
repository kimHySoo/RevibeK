package com.ssafy.revibek.song.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.revibek.song.dto.SongDto;

@Mapper
public interface SongDao {
    int insertSong(SongDto song);
    List<SongDto> selectAllSongs();
    SongDto selectSongById(String id);
    SongDto selectSongByYoutubeId(String youtubeId);
    SongDto selectSongByTitle(String title);
    List<SongDto> selectSongsByGenre(String genre);
    List<SongDto> selectRecommendSongs();
    List<SongDto> findRecommendedSongsByEraAndGenre(@Param("era") String era,
                                                     @Param("genre") String genre,
                                                     @Param("limit") int limit);
    List<SongDto> findRecommendedSongsByEra(@Param("era") String era,
                                             @Param("limit") int limit);
    List<SongDto> findRecommendedSongsByGenre(@Param("genre") String genre,
                                               @Param("limit") int limit);
    List<SongDto> findTopScoreSongs(@Param("limit") int limit);
    int updateSong(SongDto song);
    int deleteSong(String id);
}