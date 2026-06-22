package com.ssafy.revibek.like.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.ssafy.revibek.like.dto.LikeDto;
import com.ssafy.revibek.like.dto.LikeStatusDto;
import com.ssafy.revibek.like.mapper.LikeMapper;
import com.ssafy.revibek.song.dto.SongDto;
import com.ssafy.revibek.song.mapper.SongDao;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikeMapper likeMapper;
    private final SongDao songDao;

    @Transactional
    public LikeStatusDto addLike(String userId, String songId) {
        validate(userId, songId);
        ensureSongExists(songId);

        int insertedRows = likeMapper.insertLike(userId, songId);
        if (insertedRows > 0) {
            likeMapper.increaseSongLikeCount(songId);
        }

        return getLikeStatus(userId, songId);
    }

    @Transactional
    public LikeStatusDto removeLike(String userId, String songId) {
        validate(userId, songId);
        ensureSongExists(songId);

        int deletedRows = likeMapper.deleteLike(userId, songId);
        if (deletedRows > 0) {
            likeMapper.decreaseSongLikeCount(songId);
        }

        return getLikeStatus(userId, songId);
    }

    @Transactional(readOnly = true)
    public LikeStatusDto getLikeStatus(String userId, String songId) {
        validate(userId, songId);
        ensureSongExists(songId);

        boolean liked = likeMapper.existsLike(userId, songId) > 0;
        int likeCount = likeMapper.countLikesBySongId(songId);

        return LikeStatusDto.builder()
            .songId(songId)
            .liked(liked)
            .likeCount(likeCount)
            .build();
    }

    @Transactional(readOnly = true)
    public List<LikeDto> getMyLikes(String userId) {
        validateUserId(userId);
        return likeMapper.selectLikesByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<SongDto> getMyLikedSongs(String userId) {
        validateUserId(userId);
        return likeMapper.selectLikedSongsByUserId(userId);
    }

    @Transactional(readOnly = true)
    public int getLikeCount(String songId) {
        validateSongId(songId);
        ensureSongExists(songId);
        return likeMapper.countLikesBySongId(songId);
    }

    private void ensureSongExists(String songId) {
        if (songDao.selectSongById(songId) == null) {
            throw new IllegalArgumentException("존재하지 않는 곡입니다.");
        }
    }

    private void validate(String userId, String songId) {
        validateUserId(userId);
        validateSongId(songId);
    }

    private void validateUserId(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("로그인 사용자 정보가 필요합니다.");
        }
    }

    private void validateSongId(String songId) {
        if (!StringUtils.hasText(songId)) {
            throw new IllegalArgumentException("songId는 필수입니다.");
        }
    }
}
