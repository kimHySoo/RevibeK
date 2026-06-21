package com.ssafy.revibek.song.service;

import java.util.List;

import com.ssafy.revibek.song.dto.SongDto;

public interface SongService {
    int registerSong(SongDto song);
    List<SongDto> getAllSongs();
    List<SongDto> getSongsWithEmbeddingMeta(String embeddingType);
    SongDto getSongById(String id);
    SongDto getSongByYoutubeId(String youtubeId);
    List<SongDto> getSongByTitle(String title);
    List<SongDto> getSongsByGenre(String genre);
    List<SongDto> getRecommendSongs();
    int modifySong(SongDto song);
    int removeSong(String id);

    /**
     * 곡 하나의 song_moods를 moodCodes로 동기화한다(해당 곡 범위만 delete+insert).
     * 한 곡당 감정은 최대 2개라는 서비스 정책을 여기서 강제한다 — DB는 PK(song_id, mood_code)로
     * 행 수 자체를 막지 못하므로 코드 레벨 검증이 필요하다.
     *
     * @throws IllegalArgumentException moodCodes에 2개를 초과하는 값이 있거나,
     *         {@link com.ssafy.revibek.mood.MoodCode}의 7개 표준 코드가 아닌 값이 있을 때
     */
    void syncMoods(String songId, List<String> moodCodes);
}