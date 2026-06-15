package com.ssafy.revibek.song.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ssafy.revibek.song.dto.SongDto;
import com.ssafy.revibek.song.mapper.SongDao;

@Service
public class SongServiceImpl implements SongService {

    @Autowired
    private SongDao songDao;

    @Override
    public int registerSong(SongDto song) {
        return songDao.insertSong(song);
    }

    @Override
    public List<SongDto> getAllSongs() {
        return songDao.selectAllSongs();
    }

    @Override
    public SongDto getSongById(String id) {
        return songDao.selectSongById(id);
    }

    @Override
    public SongDto getSongByYoutubeId(String youtubeId) {
        return songDao.selectSongByYoutubeId(youtubeId);
    }

    @Override
    public List<SongDto> getSongByTitle(String title) {
        return songDao.selectSongByTitle(title);
    }

    @Override
    public List<SongDto> getSongsByGenre(String genre) {
        return songDao.selectSongsByGenre(genre);
    }

    @Override
    public List<SongDto> getRecommendSongs() {
        return songDao.selectRecommendSongs();
    }

    @Override
    public int modifySong(SongDto song) {
        return songDao.updateSong(song);
    }

    @Override
    public int removeSong(String id) {
        return songDao.deleteSong(id);
    }
}