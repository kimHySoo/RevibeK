package com.ssafy.revibek.song.service;

import java.util.List;

import com.ssafy.revibek.song.dto.SongDto;

public interface SongService {
    int registerSong(SongDto song);
    List<SongDto> getAllSongs();
    SongDto getSongById(String id);
    SongDto getSongByYoutubeId(String youtubeId);
    List<SongDto> getSongByTitle(String title);
    List<SongDto> getSongsByGenre(String genre);
    List<SongDto> getRecommendSongs();
    int modifySong(SongDto song);
    int removeSong(String id);
}