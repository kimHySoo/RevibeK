package com.ssafy.revibek.song.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.revibek.song.dto.SongDto;

@Mapper
public interface SongDao {
    int insertSong(SongDto song);
    List<SongDto> selectAllSongs();
    List<SongDto> selectSongsWithEmbeddingMeta(@Param("embeddingType") String embeddingType);
    SongDto selectSongById(String id);
    SongDto selectSongByYoutubeId(String youtubeId);
    List<SongDto> selectSongByTitle(String title);
    List<SongDto> selectSongsByGenre(String genre);
    List<SongDto> selectRecommendSongs();
    // mood로 좁히는 단계 (genre/generation 둘 다 항상 강제 — 둘 중 하나라도 새는 폴백은 두지 않음)
    List<SongDto> findRecommendedSongsByMoodCodeEraGenre(@Param("moodCode") String moodCode,
                                                          @Param("generation") String generation,
                                                          @Param("genre") String genre,
                                                          @Param("excludedKeywords") String excludedKeywords,
                                                          @Param("limit") int limit);
    // mood 없이 generation/genre만으로 좁히는 단계. generation은 "전체"(ALL)면 null로 건너뛸 수 있지만 genre는 항상 필수.
    List<SongDto> findRecommendedSongsByEraAndGenre(@Param("generation") String generation,
                                                     @Param("genre") String genre,
                                                     @Param("excludedKeywords") String excludedKeywords,
                                                     @Param("limit") int limit);
    List<String> selectMoodCodesBySongId(@Param("songId") String songId);
    int deleteSongMoodsNotIn(@Param("songId") String songId, @Param("moodCodes") List<String> moodCodes);
    int insertSongMoodsIgnore(@Param("songId") String songId, @Param("moodCodes") List<String> moodCodes);
    int updateSong(SongDto song);
    int deleteSong(String id);
}