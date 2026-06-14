package com.ssafy.revibek.radio.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.ssafy.revibek.playlist.dto.PlaylistDto;
import com.ssafy.revibek.playlist.service.PlaylistService;
import com.ssafy.revibek.qdrant.QdrantService;
import com.ssafy.revibek.radio.ai.AiDjMentService;
import com.ssafy.revibek.radio.dto.RadioCreateRequestDto;
import com.ssafy.revibek.radio.dto.RadioCreateResponseDto;
import com.ssafy.revibek.radio.dto.RadioRequestDto;
import com.ssafy.revibek.radio.dto.RadioResponseDto;
import com.ssafy.revibek.radio.dto.RecommendedSongResponseDto;
import com.ssafy.revibek.radio.dto.TtsFallbackResponseDto;
import com.ssafy.revibek.radio.mapper.RadioMapper;
import com.ssafy.revibek.preference.dto.UserPreferenceDto;
import com.ssafy.revibek.preference.service.PreferenceService;
import com.ssafy.revibek.song.dto.SongDto;
import com.ssafy.revibek.song.mapper.SongDao;
import com.ssafy.revibek.song.service.SongService;
import com.ssafy.revibek.tts.TtsResponseDto;
import com.ssafy.revibek.tts.TtsService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RadioService {

    private static final int DEFAULT_RECOMMENDATION_LIMIT = 5;
    private static final int EXPANDED_RECOMMENDATION_LIMIT = 8;

    private final RadioMapper radioMapper;
    private final SongDao songDao;
    private final AiDjMentService aiDjMentService;
    private final TtsService ttsService;
    private final PreferenceService preferenceService;
    private final QdrantService qdrantService;
    private final PlaylistService playlistService;
    private final SongService songService;

    @Transactional
    public RadioCreateResponseDto createRadio(String userId, RadioCreateRequestDto request) {
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
        List<SongDto> expandedSongs = expandWithQdrant(recommendationResult.songs(), EXPANDED_RECOMMENDATION_LIMIT);
        List<RecommendedSongResponseDto> recommendedSongs = toRecommendedSongs(
            expandedSongs,
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
                radioMapper.insertRecommendation(sessionId, song.getSongId(), i + 1, song.getReason());
            }
        }

        String playlistId = createRadioPlaylist(userId, request, recommendedSongs);

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
            .recommendationSource(recommendationResult.source())
            .playlistId(playlistId)
            .tts(TtsFallbackResponseDto.from(tts))
            .recommendedSongs(recommendedSongs)
            .build();
    }

	//라디오 세션 생성
	public String createSession(String userId, RadioRequestDto dto) {
        RadioCreateRequestDto request = new RadioCreateRequestDto();
        request.setMood(dto.getMood());
        request.setStory(dto.getStory());
        request.setEra(firstNonBlank(dto.getEra(), dto.getGeneration(), "2세대"));
        request.setGenre(firstNonBlank(dto.getGenre(), "댄스"));
		return createRadio(userId, request).getRadioSessionId();
	}
	
	
	//세션 단건 조회
	public RadioResponseDto getSession(String id, String userId) {
		// TODO: radioMapper.selectRadioSessionByIdAndUserId()
        // TODO: radioMapper.selectRecommendationBySessionId()
		RadioResponseDto session = radioMapper.selectRadioSessionByIdAndUserId(id, userId);
		if(session == null) {
			throw new RuntimeException("존재하지 않는 세션이거나 접근 권한이 없습니다.");
		}
		List<RadioResponseDto.RadioSongDto> songs = 
				radioMapper.selectRecommendationBySessionId(id);
		session.setSongs(songs);
		return session; 
	}
	
	//유저 세션 목록 조회
	public List<RadioResponseDto> getSessionByUser(String userId){
		List<RadioResponseDto> sessions = radioMapper.selectRadioSessionByUserId(userId);
		for (RadioResponseDto session : sessions) {
			List<RadioResponseDto.RadioSongDto> songs =
					radioMapper.selectRecommendationBySessionId(session.getId());
			session.setSongs(songs);
		}
		return sessions;
	}

    private List<SongDto> expandWithQdrant(List<SongDto> seedSongs, int totalLimit) {
        if (seedSongs.isEmpty()) {
            return seedSongs;
        }

        List<SongDto> result = new ArrayList<>(seedSongs);
        Set<String> seenIds = result.stream()
            .map(SongDto::getId)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        String seedId = seedSongs.get(0).getId();
        List<String> similarIds = qdrantService.searchSimilar(seedId, totalLimit);

        for (String id : similarIds) {
            if (result.size() >= totalLimit) {
                break;
            }
            if (seenIds.contains(id)) {
                continue;
            }
            SongDto song = songService.getSongById(id);
            if (song != null) {
                result.add(song);
                seenIds.add(id);
            }
        }
        return result;
    }

    private String createRadioPlaylist(String userId, RadioCreateRequestDto request, List<RecommendedSongResponseDto> recommendedSongs) {
        List<String> songIds = recommendedSongs.stream()
            .map(RecommendedSongResponseDto::getSongId)
            .filter(StringUtils::hasText)
            .toList();

        if (songIds.isEmpty()) {
            return null;
        }

        PlaylistDto playlist = playlistService.createPlaylist(userId, PlaylistDto.builder()
            .name(buildPlaylistName(request))
            .moodTag(request.getMood())
            .isPublic(false)
            .build());

        playlistService.addItems(userId, playlist.getId(), songIds);
        return playlist.getId();
    }

    private String buildPlaylistName(RadioCreateRequestDto request) {
        String mood = StringUtils.hasText(request.getMood()) ? request.getMood() : "감성";
        String era = StringUtils.hasText(request.getEra()) ? request.getEra() : "";
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        return String.format("%s %s 라디오 - %s", mood, era, date).replaceAll("\\s+", " ").trim();
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
        List<SongDto> songs = safeFindByMoodEraGenre(mood, era, generation, genre, excludedKeywords, limit);
        if (!songs.isEmpty()) {
            return new RecommendationResult("DB_MOOD_ERA_GENRE", songs);
        }

        songs = safeFindByMoodEra(mood, era, generation, excludedKeywords, limit);
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

        songs = safeFindByEraAndGenre(era, generation, genre, excludedKeywords, limit);
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
            return new RecommendationResult("DB_USER_PREFERENCE_FALLBACK", songs);
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
        if (!StringUtils.hasText(mood) || !StringUtils.hasText(era) || !StringUtils.hasText(generation)
            || !StringUtils.hasText(genre)) {
            return List.of();
        }
        try {
            return songDao.findRecommendedSongsByMoodEraGenre(mood, era, generation, genre, excludedKeywords, limit);
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
        if (!StringUtils.hasText(mood) || !StringUtils.hasText(era) || !StringUtils.hasText(generation)) {
            return List.of();
        }
        try {
            return songDao.findRecommendedSongsByMoodEra(mood, era, generation, excludedKeywords, limit);
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<SongDto> safeFindByMoodGenre(String mood, String genre, String excludedKeywords, int limit) {
        if (!StringUtils.hasText(mood) || !StringUtils.hasText(genre)) {
            return List.of();
        }
        try {
            return songDao.findRecommendedSongsByMoodGenre(mood, genre, excludedKeywords, limit);
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<SongDto> safeFindByMood(String mood, String excludedKeywords, int limit) {
        if (!StringUtils.hasText(mood)) {
            return List.of();
        }
        try {
            return songDao.findRecommendedSongsByMood(mood, excludedKeywords, limit);
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
        if (!StringUtils.hasText(era) || !StringUtils.hasText(generation) || !StringUtils.hasText(genre)) {
            return List.of();
        }
        try {
            return songDao.findRecommendedSongsByEraAndGenre(era, generation, genre, excludedKeywords, limit);
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<SongDto> safeFindByEra(String era, String generation, String excludedKeywords, int limit) {
        if (!StringUtils.hasText(era) || !StringUtils.hasText(generation)) {
            return List.of();
        }
        try {
            return songDao.findRecommendedSongsByEra(era, generation, excludedKeywords, limit);
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<SongDto> safeFindByGenre(String genre, String excludedKeywords, int limit) {
        if (!StringUtils.hasText(genre)) {
            return List.of();
        }
        try {
            return songDao.findRecommendedSongsByGenre(genre, excludedKeywords, limit);
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<SongDto> safeFindByPreference(UserPreferenceDto preference, String excludedKeywords, int limit) {
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
            responses.add(RecommendedSongResponseDto.builder()
                .songId(song.getId())
                .title(song.getTitle())
                .artist(song.getArtist())
                .era(toGenerationLabel(song.getEra()))
                .genre(song.getGenre())
                .youtubeUrl(resolveYoutubeUrl(song))
                .youtubeId(song.getYoutubeId())
                .score(song.getScore())
                .reason(buildReason(song, request))
                .build());
        }
        return responses;
    }

    private String buildReason(SongDto song, RadioCreateRequestDto request) {
        String eraLabel = toGenerationLabel(song.getEra());
        String mood = request.getMood();
        String genre = StringUtils.hasText(song.getGenre()) ? song.getGenre() : request.getGenre();
        String situation = StringUtils.hasText(request.getSituation()) ? " " + request.getSituation() + " 상황에 맞춰" : "";
        String videoType = StringUtils.hasText(request.getVideoType()) ? " 요청한 " + request.getVideoType() + " 감상 흐름에도 어울립니다." : "";
        if ("2세대".equals(eraLabel)) {
            return "2004년~2011년 전후 2세대 K-POP의 강한 후렴과 무대 감성이 있어 " + mood
                + " 마음을" + situation + " 환기해줄 곡입니다." + videoType;
        }
        if ("3세대".equals(eraLabel)) {
            return "2012년~2017년 전후 3세대 K-POP의 감정선과 청춘 서사가 있어 " + mood
                + " 기분에" + situation + " 어울리는 곡입니다." + videoType;
        }
        return genre + " 장르의 분위기와 높은 추천 점수를 바탕으로" + situation + " 선곡했습니다." + videoType;
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

    private void normalizeRequest(RadioCreateRequestDto request, UserPreferenceDto preference) {
        request.setMood(firstNonBlank(request.getMood(), preferenceService.firstPreferredMood(preference), "감성"));
        request.setStory(firstNonBlank(request.getStory(), ""));
        request.setEra(toGenerationLabel(firstNonBlank(request.getEra(), preferenceService.firstPreferredGeneration(preference), "2세대")));
        request.setGenre(firstNonBlank(request.getGenre(), preferenceService.firstPreferredGenre(preference), ""));
        request.setSituation(firstNonBlank(request.getSituation(), ""));
        request.setDesiredMood(firstNonBlank(request.getDesiredMood(), ""));
        request.setVideoType(firstNonBlank(request.getVideoType(), preferenceService.firstPreferredVideoType(preference), ""));
        request.setPreferredArtist(firstNonBlank(request.getPreferredArtist(), preferenceService.firstPreferredArtist(preference), ""));
        request.setExcludedKeywords(mergeKeywords(request.getExcludedKeywords(), preferenceService.joinedExcludedKeywords(preference)));
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
        if ("2".equals(value) || "2세대".equals(value) || "00s".equalsIgnoreCase(value)
            || "2000년대".equals(value) || value.contains("2000")) {
            return "2세대";
        }
        if ("3".equals(value) || "3세대".equals(value) || "10s".equalsIgnoreCase(value)
            || "2010년대".equals(value) || value.contains("2010")) {
            return "3세대";
        }
        return value;
    }

    private String trimOrThrow(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + "는 필수입니다.");
        }
        return value.trim();
    }

    private String effectiveMoodForRecommendation(RadioCreateRequestDto request) {
        return firstNonBlank(request.getDesiredMood(), request.getMood(), "");
    }

    private String mergeKeywords(String requestKeywords, String preferenceKeywords) {
        if (StringUtils.hasText(requestKeywords) && StringUtils.hasText(preferenceKeywords)) {
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

    private record RecommendationResult(String source, List<SongDto> songs) {
    }
}
