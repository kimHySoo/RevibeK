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
    List<SongDto> selectSongByTitle(String title);
    List<SongDto> selectSongsByGenre(String genre);
    List<SongDto> selectRecommendSongs();
    List<SongDto> findRecommendedSongsByMoodCodeEraGenre(@Param("moodCode") String moodCode,
                                                          @Param("era") String era,
                                                          @Param("generation") String generation,
                                                          @Param("genre") String genre,
                                                          @Param("excludedKeywords") String excludedKeywords,
                                                          @Param("limit") int limit);
    List<SongDto> findRecommendedSongsByMoodCodeEra(@Param("moodCode") String moodCode,
                                                      @Param("era") String era,
                                                      @Param("generation") String generation,
                                                      @Param("excludedKeywords") String excludedKeywords,
                                                      @Param("limit") int limit);
    List<SongDto> findRecommendedSongsByMoodCodeGenre(@Param("moodCode") String moodCode,
                                                        @Param("genre") String genre,
                                                        @Param("excludedKeywords") String excludedKeywords,
                                                        @Param("limit") int limit);
    List<SongDto> findRecommendedSongsByMoodCode(@Param("moodCode") String moodCode,
                                                  @Param("excludedKeywords") String excludedKeywords,
                                                  @Param("limit") int limit);
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
    List<String> selectMoodCodesBySongId(@Param("songId") String songId);
    int deleteSongMoodsNotIn(@Param("songId") String songId, @Param("moodCodes") List<String> moodCodes);
    int insertSongMoodsIgnore(@Param("songId") String songId, @Param("moodCodes") List<String> moodCodes);
    int updateSong(SongDto song);
    int deleteSong(String id);
}