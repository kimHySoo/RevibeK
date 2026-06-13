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
    List<SongDto> findRecommendedSongsByMoodEraGenre(@Param("mood") String mood,
                                                      @Param("era") String era,
                                                      @Param("generation") String generation,
                                                      @Param("genre") String genre,
                                                      @Param("excludedKeywords") String excludedKeywords,
                                                      @Param("limit") int limit);
    List<SongDto> findRecommendedSongsByMoodEra(@Param("mood") String mood,
                                                 @Param("era") String era,
                                                 @Param("generation") String generation,
                                                 @Param("excludedKeywords") String excludedKeywords,
                                                 @Param("limit") int limit);
    List<SongDto> findRecommendedSongsByMoodGenre(@Param("mood") String mood,
                                                   @Param("genre") String genre,
                                                   @Param("excludedKeywords") String excludedKeywords,
                                                   @Param("limit") int limit);
    List<SongDto> findRecommendedSongsByMood(@Param("mood") String mood,
                                              @Param("excludedKeywords") String excludedKeywords,
                                              @Param("limit") int limit);
    List<SongDto> findRecommendedSongsByEraAndGenre(@Param("era") String era,
                                                     @Param("generation") String generation,
                                                     @Param("genre") String genre,
                                                     @Param("excludedKeywords") String excludedKeywords,
                                                     @Param("limit") int limit);
    List<SongDto> findRecommendedSongsByEra(@Param("era") String era,
                                             @Param("generation") String generation,
                                             @Param("excludedKeywords") String excludedKeywords,
                                             @Param("limit") int limit);
    List<SongDto> findRecommendedSongsByGenre(@Param("genre") String genre,
                                               @Param("excludedKeywords") String excludedKeywords,
                                               @Param("limit") int limit);
    List<SongDto> findRecommendedSongsByPreference(@Param("moods") List<String> moods,
                                                    @Param("generations") List<String> generations,
                                                    @Param("eras") List<String> eras,
                                                    @Param("genres") List<String> genres,
                                                    @Param("artists") List<String> artists,
                                                    @Param("excludedKeywords") String excludedKeywords,
                                                    @Param("limit") int limit);
    List<SongDto> findTopScoreSongs(@Param("limit") int limit);
    int updateSong(SongDto song);
    int deleteSong(String id);
}