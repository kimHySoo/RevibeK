package com.ssafy.revibek.playlist.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.ssafy.revibek.playlist.dto.PlaylistDto;
import com.ssafy.revibek.playlist.dto.PlaylistItemDto;
import com.ssafy.revibek.playlist.mapper.PlaylistMapper;
import com.ssafy.revibek.song.mapper.SongDao;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlaylistService {

    private final PlaylistMapper playlistMapper;
    private final SongDao songDao;

    @Transactional
    public PlaylistDto createPlaylist(String userId, PlaylistDto request) {
        validateUserId(userId);
        if (!StringUtils.hasText(request.getName())) {
            throw new IllegalArgumentException("name은 필수입니다.");
        }

        PlaylistDto playlist = PlaylistDto.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .name(request.getName().trim())
                .moodTag(trimToNull(request.getMoodTag()))
                .isPublic(Boolean.TRUE.equals(request.getIsPublic()))
                .build();

        playlistMapper.insertPlaylist(playlist);
        return getPlaylist(userId, playlist.getId());
    }

    @Transactional
    public PlaylistDto createPlaylistWithSongs(
            String userId,
            String title,
            String mood,
            List<String> songIds
    ) {
        validateUserId(userId);

        String playlistTitle = StringUtils.hasText(title)
                ? title.trim()
                : "라디오 플레이리스트";

        PlaylistDto playlist = PlaylistDto.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .name(playlistTitle)
                .moodTag(trimToNull(mood))
                .isPublic(false)
                .build();

        playlistMapper.insertPlaylist(playlist);

        LinkedHashSet<String> normalizedSongIds = new LinkedHashSet<>();
        if (songIds != null) {
            for (String songId : songIds) {
                if (StringUtils.hasText(songId)) {
                    normalizedSongIds.add(songId.trim());
                }
            }
        }

        int orderNum = 1;
        for (String songId : normalizedSongIds) {
            if (songDao.selectSongById(songId) == null) {
                continue;
            }

            playlistMapper.insertPlaylistItem(
                    playlist.getId(),
                    songId,
                    orderNum
            );
            orderNum++;
        }

        return getPlaylist(userId, playlist.getId());
    }

    @Transactional(readOnly = true)
    public List<PlaylistDto> getMyPlaylists(String userId) {
        validateUserId(userId);
        return playlistMapper.selectPlaylistsByUserId(userId);
    }

    @Transactional(readOnly = true)
    public PlaylistDto getPlaylist(String userId, String playlistId) {
        validateUserId(userId);
        validatePlaylistId(playlistId);

        PlaylistDto playlist = playlistMapper.selectPlaylistByIdAndUserId(
                playlistId,
                userId
        );

        if (playlist == null) {
            throw new IllegalArgumentException(
                    "존재하지 않는 플레이리스트이거나 접근 권한이 없습니다."
            );
        }

        playlist.setItems(playlistMapper.selectPlaylistItems(playlistId));
        return playlist;
    }

    @Transactional
    public PlaylistItemDto addItem(
            String userId,
            String playlistId,
            PlaylistItemDto request
    ) {
        PlaylistDto playlist = getPlaylist(userId, playlistId);
        String songId = request.getSongId();

        if (!StringUtils.hasText(songId)) {
            throw new IllegalArgumentException("songId는 필수입니다.");
        }

        songId = songId.trim();

        if (songDao.selectSongById(songId) == null) {
            throw new IllegalArgumentException("존재하지 않는 곡입니다.");
        }

        if (playlistMapper.countPlaylistItem(playlist.getId(), songId) > 0) {
            throw new IllegalArgumentException(
                    "이미 플레이리스트에 추가된 곡입니다."
            );
        }

        int orderNum = playlistMapper.selectNextOrderNum(playlist.getId());
        playlistMapper.insertPlaylistItem(playlist.getId(), songId, orderNum);

        String finalSongId = songId;
        return playlistMapper.selectPlaylistItems(playlist.getId()).stream()
                .filter(item -> finalSongId.equals(item.getSongId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "플레이리스트 곡 추가 결과를 찾을 수 없습니다."
                ));
    }

    @Transactional
    public void deleteItem(String userId, String playlistId, String itemId) {
        getPlaylist(userId, playlistId);

        if (!StringUtils.hasText(itemId)) {
            throw new IllegalArgumentException("itemId는 필수입니다.");
        }

        int deletedRows = playlistMapper.deletePlaylistItem(
                playlistId,
                itemId
        );

        if (deletedRows == 0) {
            throw new IllegalArgumentException(
                    "존재하지 않는 플레이리스트 항목입니다."
            );
        }
    }

    @Transactional
    public void deletePlaylist(String userId, String playlistId) {
        validateUserId(userId);
        validatePlaylistId(playlistId);

        int deletedRows = playlistMapper.deletePlaylist(playlistId, userId);

        if (deletedRows == 0) {
            throw new IllegalArgumentException(
                    "존재하지 않는 플레이리스트이거나 접근 권한이 없습니다."
            );
        }
    }

    private void validateUserId(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("로그인 사용자 정보가 필요합니다.");
        }
    }

    private void validatePlaylistId(String playlistId) {
        if (!StringUtils.hasText(playlistId)) {
            throw new IllegalArgumentException("playlistId는 필수입니다.");
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}