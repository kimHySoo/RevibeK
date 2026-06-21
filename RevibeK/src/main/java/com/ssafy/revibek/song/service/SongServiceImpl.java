package com.ssafy.revibek.song.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.revibek.mood.MoodCode;
import com.ssafy.revibek.song.dto.SongDto;
import com.ssafy.revibek.song.mapper.SongDao;

@Service
public class SongServiceImpl implements SongService {

    private static final int MAX_MOODS_PER_SONG = 2;

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

    @Override
    @Transactional
    public void syncMoods(String songId, List<String> moodCodes) {
        List<String> normalized = moodCodes == null ? List.of() : moodCodes;
        LinkedHashSet<String> deduped = new LinkedHashSet<>();
        for (String raw : normalized) {
            if (raw == null) {
                continue;
            }
            String code = raw.trim().toUpperCase();
            boolean valid = false;
            for (MoodCode moodCode : MoodCode.values()) {
                if (moodCode.name().equals(code)) {
                    valid = true;
                    break;
                }
            }
            if (!valid) {
                throw new IllegalArgumentException("허용되지 않은 mood code: " + raw);
            }
            deduped.add(code);
        }
        if (deduped.size() > MAX_MOODS_PER_SONG) {
            throw new IllegalArgumentException(
                    "한 곡당 감정은 최대 " + MAX_MOODS_PER_SONG + "개까지만 저장할 수 있습니다: " + deduped);
        }
        List<String> codes = new ArrayList<>(deduped);
        songDao.deleteSongMoodsNotIn(songId, codes);
        if (!codes.isEmpty()) {
            songDao.insertSongMoodsIgnore(songId, codes);
        }
    }
}