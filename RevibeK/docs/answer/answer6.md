실제 프로젝트 파일은 수정하지 않았으며, `apply_patch`, 파일 저장, 빌드 명령도 실행하지 않았습니다.

## 1. 수정 필요 파일 목록

### 수정 필요

1. `RadioService.java`
2. `RadioController.java`
3. `RadioSelectedSongDto.java`
4. `PlaylistService.java`
5. `PreferenceController.java`
6. `SecurityConfig.java`

### 수정 불필요

1. `RadioCreateRequestDto.java`
2. `RadioCreateResponseDto.java`
3. `PlaylistMapper.java`
4. `PlaylistMapper.xml`
5. `SongDao.java`
6. `SongMapper.xml`
7. `JwtAuthenticationFilter.java`
8. `JwtTokenProvider.java`
9. `OAuth2SuccessHandler.java`

## 2. 파일별 수정 후 전체 코드

### `src/main/java/com/ssafy/revibek/radio/dto/RadioSelectedSongDto.java`

```java
package com.ssafy.revibek.radio.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RadioSelectedSongDto {

    private String songId;
    private String title;
    private String artist;
    private String youtubeUrl;
    private String thumbnailUrl;
    private String generation;
    private String genre;
    private String mood;
}
```

### `src/main/java/com/ssafy/revibek/playlist/service/PlaylistService.java`

```java
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
```

### `src/main/java/com/ssafy/revibek/radio/service/RadioService.java`

```java
package com.ssafy.revibek.radio.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.ssafy.revibek.playlist.dto.PlaylistDto;
import com.ssafy.revibek.preference.dto.UserPreferenceDto;
import com.ssafy.revibek.preference.service.PreferenceService;
import com.ssafy.revibek.radio.ai.AiDjMentService;
import com.ssafy.revibek.radio.dto.RadioCreateRequestDto;
import com.ssafy.revibek.radio.dto.RadioCreateResponseDto;
import com.ssafy.revibek.radio.dto.RadioRequestDto;
import com.ssafy.revibek.radio.dto.RadioResponseDto;
import com.ssafy.revibek.radio.dto.RadioSelectedSongDto;
import com.ssafy.revibek.radio.dto.RecommendedSongResponseDto;
import com.ssafy.revibek.radio.dto.TtsFallbackResponseDto;
import com.ssafy.revibek.radio.mapper.RadioMapper;
import com.ssafy.revibek.song.dto.SongDto;
import com.ssafy.revibek.song.mapper.SongDao;
import com.ssafy.revibek.tts.TtsResponseDto;
import com.ssafy.revibek.tts.TtsService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RadioService {

    private static final int DEFAULT_RECOMMENDATION_LIMIT = 5;

    private final RadioMapper radioMapper;
    private final SongDao songDao;
    private final AiDjMentService aiDjMentService;
    private final TtsService ttsService;
    private final PreferenceService preferenceService;
    private final PlaylistService playlistService;

    @Transactional
    public RadioCreateResponseDto createRadio(
            String userId,
            RadioCreateRequestDto request
    ) {
        validateUserId(userId);

        UserPreferenceDto preference = preferenceService.getPreference(userId);
        normalizeRequest(request, preference);

        RecommendationResult recommendationResult = recommendSongs(
                effectiveMoodForRecommendation(request),
                normalizeEraForDb(request.getEra()),
                request.getEra(),
                request.getGenre(),
                preference,
                request.getExcludedKeywords(),
                DEFAULT_RECOMMENDATION_LIMIT
        );

        List<RecommendedSongResponseDto> recommendedSongs = toRecommendedSongs(
                recommendationResult.songs(),
                request
        );

        String djMent = aiDjMentService.createDjMent(request, recommendedSongs);
        String sessionId = UUID.randomUUID().toString();

        radioMapper.insertRadioSessionWithMent(
                sessionId,
                userId,
                request.getMood(),
                request.getStory(),
                request.getEra(),
                request.getGenre(),
                request.getSituation(),
                request.getDesiredMood(),
                request.getVideoType(),
                request.getPreferredArtist(),
                request.getExcludedKeywords(),
                recommendationResult.source(),
                djMent
        );

        for (int i = 0; i < recommendedSongs.size(); i++) {
            RecommendedSongResponseDto song = recommendedSongs.get(i);

            if (StringUtils.hasText(song.getSongId())) {
                radioMapper.insertRecommendation(
                        sessionId,
                        song.getSongId(),
                        i + 1,
                        song.getReason()
                );
            }
        }

        String playlistId = null;

        if (Boolean.TRUE.equals(request.getSaveAsPlaylist())) {
            List<String> playlistSongIds = resolvePlaylistSongIds(
                    request,
                    recommendedSongs
            );

            PlaylistDto playlist = playlistService.createPlaylistWithSongs(
                    userId,
                    request.getTitle(),
                    request.getMood(),
                    playlistSongIds
            );

            playlistId = playlist.getId();
        }

        TtsResponseDto tts = ttsService.synthesize(djMent);

        return RadioCreateResponseDto.builder()
                .radioSessionId(sessionId)
                .userId(userId)
                .mood(request.getMood())
                .story(request.getStory())
                .era(request.getEra())
                .genre(request.getGenre())
                .situation(request.getSituation())
                .desiredMood(request.getDesiredMood())
                .videoType(request.getVideoType())
                .preferredArtist(request.getPreferredArtist())
                .excludedKeywords(request.getExcludedKeywords())
                .djMent(djMent)
                .playlistId(playlistId)
                .title(request.getTitle())
                .recommendationSource(recommendationResult.source())
                .tts(TtsFallbackResponseDto.from(tts))
                .recommendedSongs(recommendedSongs)
                .build();
    }

    public String createSession(String userId, RadioRequestDto dto) {
        RadioCreateRequestDto request = new RadioCreateRequestDto();
        request.setMood(dto.getMood());
        request.setStory(dto.getStory());
        request.setEra(firstNonBlank(dto.getEra(), dto.getGeneration(), "2세대"));
        request.setGenre(firstNonBlank(dto.getGenre(), "댄스"));

        return createRadio(userId, request).getRadioSessionId();
    }

    public RadioResponseDto getSession(String id, String userId) {
        RadioResponseDto session = radioMapper.selectRadioSessionByIdAndUserId(
                id,
                userId
        );

        if (session == null) {
            throw new RuntimeException(
                    "존재하지 않는 세션이거나 접근 권한이 없습니다."
            );
        }

        List<RadioResponseDto.RadioSongDto> songs =
                radioMapper.selectRecommendationBySessionId(id);

        session.setSongs(songs);
        return session;
    }

    public List<RadioResponseDto> getSessionByUser(String userId) {
        List<RadioResponseDto> sessions =
                radioMapper.selectRadioSessionByUserId(userId);

        for (RadioResponseDto session : sessions) {
            List<RadioResponseDto.RadioSongDto> songs =
                    radioMapper.selectRecommendationBySessionId(session.getId());

            session.setSongs(songs);
        }

        return sessions;
    }

    private List<String> resolvePlaylistSongIds(
            RadioCreateRequestDto request,
            List<RecommendedSongResponseDto> recommendedSongs
    ) {
        List<RadioSelectedSongDto> selectedSongs = request.getSelectedSongs();

        if (selectedSongs != null && !selectedSongs.isEmpty()) {
            return selectedSongs.stream()
                    .filter(song -> song != null)
                    .map(RadioSelectedSongDto::getSongId)
                    .toList();
        }

        return recommendedSongs.stream()
                .map(RecommendedSongResponseDto::getSongId)
                .toList();
    }

    private RecommendationResult recommendSongs(
            String mood,
            String era,
            String generation,
            String genre,
            UserPreferenceDto preference,
            String excludedKeywords,
            int limit
    ) {
        List<SongDto> songs = safeFindByMoodEraGenre(
                mood,
                era,
                generation,
                genre,
                excludedKeywords,
                limit
        );

        if (!songs.isEmpty()) {
            return new RecommendationResult("DB_MOOD_ERA_GENRE", songs);
        }

        songs = safeFindByMoodEra(
                mood,
                era,
                generation,
                excludedKeywords,
                limit
        );

        if (!songs.isEmpty()) {
            return new RecommendationResult("DB_MOOD_ERA_FALLBACK", songs);
        }

        songs = safeFindByMoodGenre(mood, genre, excludedKeywords, limit);

        if (!songs.isEmpty()) {
            return new RecommendationResult("DB_MOOD_GENRE_FALLBACK", songs);
        }

        songs = safeFindByMood(mood, excludedKeywords, limit);

        if (!songs.isEmpty()) {
            return new RecommendationResult("DB_MOOD_FALLBACK", songs);
        }

        songs = safeFindByEraAndGenre(
                era,
                generation,
                genre,
                excludedKeywords,
                limit
        );

        if (!songs.isEmpty()) {
            return new RecommendationResult("DB_ERA_GENRE", songs);
        }

        songs = safeFindByEra(era, generation, excludedKeywords, limit);

        if (!songs.isEmpty()) {
            return new RecommendationResult("DB_ERA_FALLBACK", songs);
        }

        songs = safeFindByGenre(genre, excludedKeywords, limit);

        if (!songs.isEmpty()) {
            return new RecommendationResult("DB_GENRE_FALLBACK", songs);
        }

        songs = safeFindByPreference(preference, excludedKeywords, limit);

        if (!songs.isEmpty()) {
            return new RecommendationResult(
                    "DB_USER_PREFERENCE_FALLBACK",
                    songs
            );
        }

        songs = safeFindTopScore(limit);

        if (!songs.isEmpty()) {
            return new RecommendationResult("DB_SCORE_FALLBACK", songs);
        }

        return new RecommendationResult("DB_EMPTY", List.of());
    }

    private List<SongDto> safeFindByMoodEraGenre(
            String mood,
            String era,
            String generation,
            String genre,
            String excludedKeywords,
            int limit
    ) {
        if (!StringUtils.hasText(mood)
                || !StringUtils.hasText(era)
                || !StringUtils.hasText(generation)
                || !StringUtils.hasText(genre)) {
            return List.of();
        }

        try {
            return songDao.findRecommendedSongsByMoodEraGenre(
                    mood,
                    era,
                    generation,
                    genre,
                    excludedKeywords,
                    limit
            );
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<SongDto> safeFindByMoodEra(
            String mood,
            String era,
            String generation,
            String excludedKeywords,
            int limit
    ) {
        if (!StringUtils.hasText(mood)
                || !StringUtils.hasText(era)
                || !StringUtils.hasText(generation)) {
            return List.of();
        }

        try {
            return songDao.findRecommendedSongsByMoodEra(
                    mood,
                    era,
                    generation,
                    excludedKeywords,
                    limit
            );
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<SongDto> safeFindByMoodGenre(
            String mood,
            String genre,
            String excludedKeywords,
            int limit
    ) {
        if (!StringUtils.hasText(mood) || !StringUtils.hasText(genre)) {
            return List.of();
        }

        try {
            return songDao.findRecommendedSongsByMoodGenre(
                    mood,
                    genre,
                    excludedKeywords,
                    limit
            );
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<SongDto> safeFindByMood(
            String mood,
            String excludedKeywords,
            int limit
    ) {
        if (!StringUtils.hasText(mood)) {
            return List.of();
        }

        try {
            return songDao.findRecommendedSongsByMood(
                    mood,
                    excludedKeywords,
                    limit
            );
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<SongDto> safeFindByEraAndGenre(
            String era,
            String generation,
            String genre,
            String excludedKeywords,
            int limit
    ) {
        if (!StringUtils.hasText(era)
                || !StringUtils.hasText(generation)
                || !StringUtils.hasText(genre)) {
            return List.of();
        }

        try {
            return songDao.findRecommendedSongsByEraAndGenre(
                    era,
                    generation,
                    genre,
                    excludedKeywords,
                    limit
            );
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<SongDto> safeFindByEra(
            String era,
            String generation,
            String excludedKeywords,
            int limit
    ) {
        if (!StringUtils.hasText(era)
                || !StringUtils.hasText(generation)) {
            return List.of();
        }

        try {
            return songDao.findRecommendedSongsByEra(
                    era,
                    generation,
                    excludedKeywords,
                    limit
            );
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<SongDto> safeFindByGenre(
            String genre,
            String excludedKeywords,
            int limit
    ) {
        if (!StringUtils.hasText(genre)) {
            return List.of();
        }

        try {
            return songDao.findRecommendedSongsByGenre(
                    genre,
                    excludedKeywords,
                    limit
            );
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<SongDto> safeFindByPreference(
            UserPreferenceDto preference,
            String excludedKeywords,
            int limit
    ) {
        if (preference == null || !hasAnyPreference(preference)) {
            return List.of();
        }

        try {
            return songDao.findRecommendedSongsByPreference(
                    preference.getPreferredMoods(),
                    preference.getPreferredGenerations(),
                    preference.getPreferredGenerations().stream()
                            .map(this::normalizeEraForDb)
                            .toList(),
                    preference.getPreferredGenres(),
                    preference.getPreferredArtists(),
                    excludedKeywords,
                    limit
            );
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<SongDto> safeFindTopScore(int limit) {
        try {
            return songDao.findTopScoreSongs(limit);
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<RecommendedSongResponseDto> toRecommendedSongs(
            List<SongDto> songs,
            RadioCreateRequestDto request
    ) {
        List<RecommendedSongResponseDto> responses = new ArrayList<>();

        for (SongDto song : songs) {
            responses.add(
                    RecommendedSongResponseDto.builder()
                            .songId(song.getId())
                            .title(song.getTitle())
                            .artist(song.getArtist())
                            .era(toGenerationLabel(song.getEra()))
                            .genre(song.getGenre())
                            .youtubeUrl(resolveYoutubeUrl(song))
                            .youtubeId(song.getYoutubeId())
                            .score(song.getScore())
                            .reason(buildReason(song, request))
                            .build()
            );
        }

        return responses;
    }

    private String buildReason(
            SongDto song,
            RadioCreateRequestDto request
    ) {
        String eraLabel = toGenerationLabel(song.getEra());
        String mood = request.getMood();

        String genre = StringUtils.hasText(song.getGenre())
                ? song.getGenre()
                : request.getGenre();

        String situation = StringUtils.hasText(request.getSituation())
                ? " " + request.getSituation() + " 상황에 맞춰"
                : "";

        String videoType = StringUtils.hasText(request.getVideoType())
                ? " 요청한 " + request.getVideoType() + " 감상 흐름에도 어울립니다."
                : "";

        if ("2세대".equals(eraLabel)) {
            return "2004년~2011년 전후 2세대 K-POP의 강한 후렴과 "
                    + "무대 감성이 있어 "
                    + mood
                    + " 마음을"
                    + situation
                    + " 환기해줄 곡입니다."
                    + videoType;
        }

        if ("3세대".equals(eraLabel)) {
            return "2012년~2017년 전후 3세대 K-POP의 감정선과 "
                    + "청춘 서사가 있어 "
                    + mood
                    + " 기분에"
                    + situation
                    + " 어울리는 곡입니다."
                    + videoType;
        }

        return genre
                + " 장르의 분위기와 높은 추천 점수를 바탕으로"
                + situation
                + " 선곡했습니다."
                + videoType;
    }

    private String resolveYoutubeUrl(SongDto song) {
        if (StringUtils.hasText(song.getYoutubeUrl())) {
            return song.getYoutubeUrl();
        }

        if (StringUtils.hasText(song.getYoutubeId())) {
            return "https://www.youtube.com/watch?v=" + song.getYoutubeId();
        }

        return null;
    }

    private void validateUserId(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("로그인 사용자 정보가 필요합니다.");
        }
    }

    private void normalizeRequest(
            RadioCreateRequestDto request,
            UserPreferenceDto preference
    ) {
        request.setMood(firstNonBlank(
                request.getMood(),
                preferenceService.firstPreferredMood(preference),
                "감성"
        ));

        request.setStory(firstNonBlank(request.getStory(), ""));

        request.setEra(toGenerationLabel(firstNonBlank(
                request.getEra(),
                preferenceService.firstPreferredGeneration(preference),
                "2세대"
        )));

        request.setGenre(firstNonBlank(
                request.getGenre(),
                preferenceService.firstPreferredGenre(preference),
                ""
        ));

        request.setSituation(firstNonBlank(request.getSituation(), ""));
        request.setDesiredMood(firstNonBlank(request.getDesiredMood(), ""));

        request.setVideoType(firstNonBlank(
                request.getVideoType(),
                preferenceService.firstPreferredVideoType(preference),
                ""
        ));

        request.setPreferredArtist(firstNonBlank(
                request.getPreferredArtist(),
                preferenceService.firstPreferredArtist(preference),
                ""
        ));

        request.setExcludedKeywords(mergeKeywords(
                request.getExcludedKeywords(),
                preferenceService.joinedExcludedKeywords(preference)
        ));
    }

    private String normalizeEraForDb(String era) {
        String normalized = toGenerationLabel(era);

        if ("2세대".equals(normalized)) {
            return "00s";
        }

        if ("3세대".equals(normalized)) {
            return "10s";
        }

        return normalized;
    }

    private String toGenerationLabel(String era) {
        if (!StringUtils.hasText(era)) {
            return "미지정";
        }

        String value = era.trim();

        if ("2".equals(value)
                || "2세대".equals(value)
                || "00s".equalsIgnoreCase(value)
                || "2000년대".equals(value)
                || value.contains("2000")) {
            return "2세대";
        }

        if ("3".equals(value)
                || "3세대".equals(value)
                || "10s".equalsIgnoreCase(value)
                || "2010년대".equals(value)
                || value.contains("2010")) {
            return "3세대";
        }

        return value;
    }

    private String effectiveMoodForRecommendation(
            RadioCreateRequestDto request
    ) {
        return firstNonBlank(
                request.getDesiredMood(),
                request.getMood(),
                ""
        );
    }

    private String mergeKeywords(
            String requestKeywords,
            String preferenceKeywords
    ) {
        if (StringUtils.hasText(requestKeywords)
                && StringUtils.hasText(preferenceKeywords)) {
            return requestKeywords.trim() + "," + preferenceKeywords.trim();
        }

        return firstNonBlank(requestKeywords, preferenceKeywords, "");
    }

    private boolean hasAnyPreference(UserPreferenceDto preference) {
        return !preference.getPreferredMoods().isEmpty()
                || !preference.getPreferredGenerations().isEmpty()
                || !preference.getPreferredGenres().isEmpty()
                || !preference.getPreferredArtists().isEmpty();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }

        return "";
    }

    private record RecommendationResult(
            String source,
            List<SongDto> songs
    ) {
    }
}
```

### `src/main/java/com/ssafy/revibek/radio/controller/RadioController.java`

```java
package com.ssafy.revibek.radio.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ssafy.revibek.radio.dto.RadioCreateRequestDto;
import com.ssafy.revibek.radio.dto.RadioCreateResponseDto;
import com.ssafy.revibek.radio.dto.RadioResponseDto;
import com.ssafy.revibek.radio.service.RadioService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/radio")
@RequiredArgsConstructor
public class RadioController {

    private final RadioService radioService;

    @PostMapping
    public ResponseEntity<RadioCreateResponseDto> createRadio(
        Authentication authentication,
        @Valid @RequestBody RadioCreateRequestDto request
    ) {
        String userId = getAuthenticatedUserId(authentication);
        return ResponseEntity.ok(radioService.createRadio(userId, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RadioResponseDto> getSession(
        Authentication authentication,
        @PathVariable String id
    ) {
        String userId = getAuthenticatedUserId(authentication);
        return ResponseEntity.ok(radioService.getSession(id, userId));
    }

    @GetMapping("/me")
    public ResponseEntity<List<RadioResponseDto>> getSessionsByUser(
        Authentication authentication
    ) {
        String userId = getAuthenticatedUserId(authentication);
        return ResponseEntity.ok(radioService.getSessionByUser(userId));
    }

    private String getAuthenticatedUserId(Authentication authentication) {
        if (authentication == null
            || !authentication.isAuthenticated()
            || !StringUtils.hasText(authentication.getName())
            || "anonymousUser".equals(authentication.getName())) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "인증된 사용자 정보가 필요합니다."
            );
        }

        String userId = authentication.getName();
        return userId.trim();
    }
}
```

### `src/main/java/com/ssafy/revibek/preference/controller/PreferenceController.java`

```java
package com.ssafy.revibek.preference.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ssafy.revibek.common.dto.ApiResponseDto;
import com.ssafy.revibek.preference.dto.UserPreferenceDto;
import com.ssafy.revibek.preference.dto.UserPreferenceRequestDto;
import com.ssafy.revibek.preference.service.PreferenceService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/preferences")
@RequiredArgsConstructor
public class PreferenceController {

    private final PreferenceService preferenceService;

    @PostMapping
    public ResponseEntity<ApiResponseDto<UserPreferenceDto>> savePreference(
        Authentication authentication,
        @RequestBody UserPreferenceRequestDto request
    ) {
        String userId = getAuthenticatedUserId(authentication);
        UserPreferenceDto data =
            preferenceService.savePreference(userId, request);

        return ResponseEntity.ok(
            ApiResponseDto.success("사용자 취향이 저장되었습니다.", data)
        );
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponseDto<UserPreferenceDto>> getMyPreference(
        Authentication authentication
    ) {
        String userId = getAuthenticatedUserId(authentication);
        UserPreferenceDto data = preferenceService.getPreference(userId);

        return ResponseEntity.ok(
            ApiResponseDto.success("사용자 취향 조회 완료", data)
        );
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponseDto<UserPreferenceDto>> updateMyPreference(
        Authentication authentication,
        @RequestBody UserPreferenceRequestDto request
    ) {
        String userId = getAuthenticatedUserId(authentication);
        UserPreferenceDto data =
            preferenceService.savePreference(userId, request);

        return ResponseEntity.ok(
            ApiResponseDto.success("사용자 취향을 수정했습니다.", data)
        );
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponseDto<Void>> deleteMyPreference(
        Authentication authentication
    ) {
        String userId = getAuthenticatedUserId(authentication);
        preferenceService.deletePreference(userId);

        return ResponseEntity.ok(
            ApiResponseDto.success("사용자 취향을 삭제했습니다.", null)
        );
    }

    private String getAuthenticatedUserId(Authentication authentication) {
        if (authentication == null
            || !authentication.isAuthenticated()
            || !StringUtils.hasText(authentication.getName())
            || "anonymousUser".equals(authentication.getName())) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "인증된 사용자 정보가 필요합니다."
            );
        }

        String userId = authentication.getName();
        return userId.trim();
    }
}
```

### `src/main/java/com/ssafy/revibek/config/SecurityConfig.java`

```java
package com.ssafy.revibek.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.ssafy.revibek.auth.JwtAuthenticationFilter;
import com.ssafy.revibek.auth.OAuth2SuccessHandler;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    @Value("${app.oauth.google.enabled:false}")
    private boolean googleOAuthEnabled;

    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret:}")
    private String googleClientSecret;

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(
                SessionCreationPolicy.IF_REQUIRED
            ))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/auth/**",
                    "/oauth2/**",
                    "/login/oauth2/**",
                    "/auth/google/callback",
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                ).permitAll()

                .requestMatchers(HttpMethod.GET, "/api/songs/**").permitAll()

                .requestMatchers("/api/preferences/**").authenticated()
                .requestMatchers("/api/radio/**").authenticated()
                .requestMatchers("/api/playlists/**").authenticated()
                .requestMatchers("/api/likes/**").authenticated()
                .requestMatchers("/api/users/me").authenticated()
                .requestMatchers("/api/usersongs/**").authenticated()

                .requestMatchers(HttpMethod.POST, "/api/songs/**")
                    .authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/songs/**")
                    .authenticated()
                .requestMatchers(HttpMethod.PATCH, "/api/songs/**")
                    .authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/songs/**")
                    .authenticated()

                .requestMatchers("/api/analysis/**").authenticated()
                .requestMatchers("/api/qdrant/**").authenticated()
                .requestMatchers("/api/youtube/**").authenticated()

                .anyRequest().authenticated()
            )
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        if (isGoogleOAuthReady()) {
            http.oauth2Login(oauth2 -> oauth2
                .redirectionEndpoint(redirection ->
                    redirection.baseUri("/auth/google/callback")
                )
                .successHandler(oAuth2SuccessHandler)
            );
        }

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(parseAllowedOrigins());
        configuration.setAllowedMethods(
            List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        );
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private boolean isGoogleOAuthReady() {
        return googleOAuthEnabled
            && StringUtils.hasText(googleClientId)
            && StringUtils.hasText(googleClientSecret);
    }

    private List<String> parseAllowedOrigins() {
        return Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(StringUtils::hasText)
            .toList();
    }
}
```

## 3. 수정 불필요 파일

### `RadioCreateRequestDto.java`

현재 이미 다음 필드를 포함하며 Lombok `@Data`, `@NoArgsConstructor`가 적용되어 있습니다.

```java
private String title;
private Boolean saveAsPlaylist;
private List<RadioSelectedSongDto> selectedSongs = new ArrayList<>();
```

따라서 수정 불필요입니다.

### `RadioCreateResponseDto.java`

현재 이미 다음 필드를 포함합니다.

```java
private String playlistId;
```

`RadioService`에서 빌더의 `.playlistId(playlistId)`를 호출할 수 있으므로 수정 불필요입니다.

### `PlaylistMapper.java`

현재 메서드를 그대로 재사용할 수 있습니다.

```java
int insertPlaylist(PlaylistDto playlist);

int insertPlaylistItem(
    @Param("playlistId") String playlistId,
    @Param("songId") String songId,
    @Param("orderNum") int orderNum
);
```

수정 불필요입니다.

### `PlaylistMapper.xml`

현재 namespace가 Java Mapper와 일치합니다.

```xml
<mapper namespace="com.ssafy.revibek.playlist.mapper.PlaylistMapper">
```

`insertPlaylist`, `insertPlaylistItem` statement id가 Java 메서드명과 일치하며 `playlist_songs.order_num`을 사용하고 있으므로 수정 불필요입니다.

### `SongDao.java`, `SongMapper.xml`

기존 `selectSongById(String id)`를 통해 곡 존재 여부를 확인할 수 있으므로 수정 불필요입니다.

### JWT 인증 관련 클래스

`JwtAuthenticationFilter`는 JWT subject의 UUID 사용자 ID를 문자열로 가져와 Authentication name으로 설정합니다.

```java
String userId = jwtTokenProvider.getUserId(token);
```

`Long.parseLong()`을 사용하지 않으므로 다음 파일은 수정 불필요입니다.

- `JwtAuthenticationFilter.java`
- `JwtTokenProvider.java`
- `OAuth2SuccessHandler.java`

## 4. 수동 적용 순서

1. `RadioSelectedSongDto.java` 수정
2. `PlaylistService.java` 수정
3. `RadioService.java` 수정
4. `RadioController.java` 수정
5. `PreferenceController.java` 수정
6. `SecurityConfig.java` 수정
7. `mvn clean compile` 실행
8. `mvn test` 실행
9. 애플리케이션 실행 및 API 테스트

## 5. 실행 명령어

```powershell
mvn clean compile
mvn test
mvn spring-boot:run
```

## 6. 테스트 순서

1. 로그인 후 JWT 발급
2. JWT 없이 `/api/preferences`, `/api/radio`, `/api/playlists`, `/api/likes` 호출 시 401 또는 403 확인
3. JWT로 `/api/users/me` 호출 확인
4. JWT로 preference 저장 및 조회
5. `saveAsPlaylist=true`, `selectedSongs` 포함 라디오 생성
6. `selectedSongs`에 포함된 곡만 `playlist_songs`에 저장됐는지 확인
7. `selectedSongs` 없이 라디오 생성
8. 추천곡 전체가 `playlist_songs`에 저장됐는지 확인
9. `radio_sessions`, `radio_recommendations` 저장 여부 확인
10. `/api/playlists`에서 생성된 플레이리스트 확인
11. 존재하지 않는 `songId`가 `playlist_songs`에 저장되지 않는지 확인
12. 중복·공백 포함 `songId`가 정리되고 `order_num`이 1부터 순서대로 저장되는지 확인