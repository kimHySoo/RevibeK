package com.ssafy.revibek.radio.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.ssafy.revibek.analysis.dto.AnalyzeResponseDto;
import com.ssafy.revibek.analysis.service.AnalysisService;
import com.ssafy.revibek.follow.mapper.FollowMapper;
import com.ssafy.revibek.mood.GenerationCode;
import com.ssafy.revibek.mood.GenerationNormalizer;
import com.ssafy.revibek.mood.MoodCode;
import com.ssafy.revibek.mood.MoodNormalizer;
import com.ssafy.revibek.playlist.dto.PlaylistDto;
import com.ssafy.revibek.playlist.dto.PlaylistItemDto;
import com.ssafy.revibek.playlist.service.PlaylistService;
import com.ssafy.revibek.qdrant.QdrantService;
import com.ssafy.revibek.radio.ai.AiDjMentService;
import com.ssafy.revibek.radio.dto.PublicRadioFeedDto;
import com.ssafy.revibek.radio.dto.RadioCreateRequestDto;
import com.ssafy.revibek.radio.dto.RadioCreateResponseDto;
import com.ssafy.revibek.radio.dto.RadioLikeCountResponseDto;
import com.ssafy.revibek.radio.dto.RadioLikeResponseDto;
import com.ssafy.revibek.radio.dto.RadioPublicToggleRequestDto;
import com.ssafy.revibek.radio.dto.RadioPublicToggleResponseDto;
import com.ssafy.revibek.radio.dto.RadioRequestDto;
import com.ssafy.revibek.radio.dto.RadioResponseDto;
import com.ssafy.revibek.radio.dto.RecommendedSongResponseDto;
import com.ssafy.revibek.radio.dto.TtsFallbackResponseDto;
import com.ssafy.revibek.radio.mapper.RadioLikeMapper;
import com.ssafy.revibek.radio.mapper.RadioMapper;
import com.ssafy.revibek.radio.exception.RadioNotFoundException;
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
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RadioService {

    private static final int DEFAULT_RECOMMENDATION_LIMIT = 5;
    private static final int EXPANDED_RECOMMENDATION_LIMIT = 8;
    // era/genre 필터링으로 후보가 줄어드는 것을 보완하기 위해 Qdrant에서 필요한 개수보다 이만큼 더 많이 가져온다.
    private static final int QDRANT_CANDIDATE_POOL_FACTOR = 4;

    private final RadioMapper radioMapper;
    private final RadioLikeMapper radioLikeMapper;
    private final FollowMapper followMapper;
    private final SongDao songDao;
    private final AiDjMentService aiDjMentService;
    private final TtsService ttsService;
    private final PreferenceService preferenceService;
    private final QdrantService qdrantService;
    private final PlaylistService playlistService;
    private final SongService songService;
    private final AnalysisService analysisService;

    @Transactional
    public RadioCreateResponseDto createRadio(String userId, RadioCreateRequestDto request) {
        validateUserId(userId);
        UserPreferenceDto preference = preferenceService.getPreference(userId);
        normalizeRequest(request, preference);

        // YouTube URL이 있으면 임베딩 기반 Qdrant 검색 우선; 실패하면 DB 폴백
        List<SongDto> qdrantSeeds = StringUtils.hasText(request.getYoutubeUrl())
                ? findByYoutubeUrlEmbedding(request.getYoutubeUrl(), DEFAULT_RECOMMENDATION_LIMIT, request)
                : List.of();

        List<SongDto> seedSongs;
        String recommendationSource;
        if (!qdrantSeeds.isEmpty()) {
            seedSongs = qdrantSeeds;
            recommendationSource = "QDRANT_URL_EMBEDDING";
        } else {
            RecommendationResult recommendationResult = recommendSongs(
                    effectiveMoodForRecommendation(request),
                    request.getGeneration(),
                    request.getGenre(),
                    preference,
                    request.getExcludedKeywords(),
                    DEFAULT_RECOMMENDATION_LIMIT
            );
            seedSongs = applyYoutubeUrlSeed(recommendationResult.songs(), request.getYoutubeUrl());
            recommendationSource = recommendationResult.source();
        }

        List<SongDto> expandedSongs = expandWithQdrant(seedSongs, EXPANDED_RECOMMENDATION_LIMIT, request);
        List<RecommendedSongResponseDto> recommendedSongs = toRecommendedSongs(expandedSongs, request);

        String djMent = aiDjMentService.createDjMent(request, recommendedSongs);
        String sessionId = UUID.randomUUID().toString();
        radioMapper.insertRadioSessionWithMent(
                sessionId,
                userId,
                request.getMood(),
                request.getStory(),
                request.getGeneration(),
                request.getGenre(),
                request.getSituation(),
                request.getDesiredMood(),
                request.getVideoType(),
                request.getPreferredArtist(),
                request.getExcludedKeywords(),
                recommendationSource,
                djMent
        );

        for (int i = 0; i < recommendedSongs.size(); i++) {
            RecommendedSongResponseDto song = recommendedSongs.get(i);
            if (StringUtils.hasText(song.getSongId())) {
                radioMapper.insertRecommendation(sessionId, song.getSongId(), i + 1, song.getReason());
            }
        }

        String playlistId = createRadioPlaylist(userId, request, recommendedSongs);
        if (playlistId != null) {
            radioMapper.updateRadioSessionPlaylistId(sessionId, userId, playlistId);
        }

        // TTS 합성 실패는 항상 ttsService 내부에서 브라우저 fallback으로 흡수되므로
        // 여기서 예외가 나더라도 라디오 생성 자체는 실패시키지 않는다.
        TtsResponseDto tts = ttsService.synthesize(djMent);
        try {
            radioMapper.updateRadioSessionTts(sessionId, userId, tts.getMode(), tts.getAudioUrl(), tts.getVoice(), tts.getAudioEncoding());
        } catch (Exception e) {
            // TTS 결과 저장에 실패해도 라디오 생성 응답은 정상적으로 내려준다(재조회 시에만 영향).
            log.warn("라디오 세션 TTS 결과 저장 실패. sessionId={}, reason={}", sessionId, e.getMessage());
        }

        return RadioCreateResponseDto.builder()
                .radioSessionId(sessionId)
                .playlistId(playlistId)
                .userId(userId)
                .mood(request.getMood())
                .story(request.getStory())
                .generation(request.getGeneration())
                .genre(request.getGenre())
                .situation(request.getSituation())
                .desiredMood(request.getDesiredMood())
                .videoType(request.getVideoType())
                .preferredArtist(request.getPreferredArtist())
                .excludedKeywords(request.getExcludedKeywords())
                .djMent(djMent)
                .recommendationSource(recommendationSource)
                .tts(TtsFallbackResponseDto.from(tts))
                .recommendedSongs(recommendedSongs)
                .build();
    }

    public String createSession(String userId, RadioRequestDto dto) {
        RadioCreateRequestDto request = new RadioCreateRequestDto();
        request.setMood(dto.getMood());
        request.setStory(dto.getStory());
        request.setGeneration(firstNonBlank(dto.getGeneration(), dto.getEra(), "2세대"));
        request.setGenre(firstNonBlank(dto.getGenre(), "댄스"));
        return createRadio(userId, request).getRadioSessionId();
    }

    public RadioResponseDto getSession(String id, String userId) {
        validateUserId(userId);
        if (!StringUtils.hasText(id)) {
            throw new IllegalArgumentException("라디오 세션 ID가 필요합니다.");
        }
        RadioResponseDto session = radioMapper.selectRadioSessionByIdAndUserId(id, userId);
        if (session == null) {
            throw new RadioNotFoundException("존재하지 않는 라디오 세션이거나 접근 권한이 없습니다.");
        }
        List<RadioResponseDto.RadioSongDto> songs = radioMapper.selectRecommendationBySessionId(id);
        session.setSongs(songs);
        // DB에 저장된 TTS 결과로 복원한다. Google TTS를 재호출하면 비용이 중복 발생하므로 절대 다시 합성하지 않는다.
        session.setTts(resolveStoredTts(session));
        return session;
    }

    /**
     * 저장된 tts_mode/tts_audio_url 컬럼으로부터 응답용 TTS DTO를 재구성한다.
     * - tts_mode가 없으면(과거 세션 또는 TTS 미생성) djMent가 있을 때만 브라우저 fallback을 만들어준다.
     * - tts_mode가 GOOGLE_TTS인데 audioUrl이 비어 있으면(저장 실패 등) 안전하게 브라우저 fallback으로 내려준다.
     */
    private TtsFallbackResponseDto resolveStoredTts(RadioResponseDto session) {
        TtsFallbackResponseDto stored = session.getTts();
        if (stored == null || !StringUtils.hasText(stored.getMode())) {
            if (!StringUtils.hasText(session.getDjMent())) {
                return null;
            }
            return TtsFallbackResponseDto.builder()
                    .mode("BROWSER_TTS")
                    .text(session.getDjMent())
                    .audioUrl(null)
                    .build();
        }

        boolean isGoogleAudioAvailable = "GOOGLE_TTS".equals(stored.getMode()) && StringUtils.hasText(stored.getAudioUrl());
        return TtsFallbackResponseDto.builder()
                .mode(isGoogleAudioAvailable ? "GOOGLE_TTS" : "BROWSER_TTS")
                .text(session.getDjMent())
                .audioUrl(isGoogleAudioAvailable ? stored.getAudioUrl() : null)
                .build();
    }

    @Transactional
    public List<RadioResponseDto> getSessionByUser(String userId) {
        List<RadioResponseDto> sessions = radioMapper.selectRadioSessionByUserId(userId);
        for (RadioResponseDto session : sessions) {
            List<RadioResponseDto.RadioSongDto> songs =
                    radioMapper.selectRecommendationBySessionId(session.getId());
            session.setSongs(songs);
        }
        return sessions;
    }

    @Transactional
    public RadioPublicToggleResponseDto togglePublic(String userId, String radioSessionId,
                                                     RadioPublicToggleRequestDto request) {
        validateUserId(userId);
        if (!StringUtils.hasText(radioSessionId)) {
            throw new IllegalArgumentException("라디오 세션 ID가 필요합니다.");
        }
        if (request.getIsPublic() == null) {
            throw new IllegalArgumentException("isPublic 값이 필요합니다.");
        }

        int updated = radioMapper.updateRadioSessionPublic(radioSessionId, userId, request.getIsPublic());
        if (updated == 0) {
            throw new IllegalArgumentException("존재하지 않는 라디오 세션이거나 접근 권한이 없습니다.");
        }

        return radioMapper.selectRadioPublicStatus(radioSessionId, userId);
    }

    public List<PublicRadioFeedDto> getPublicSessions(String sort, String currentUserId) {
        String sortParam = "popular".equals(sort) ? "popular" : "latest";
        List<PublicRadioFeedDto> sessions = radioMapper.selectPublicRadioSessions(sortParam);
        for (PublicRadioFeedDto session : sessions) {
            if (StringUtils.hasText(currentUserId)) {
                session.setLiked(radioLikeMapper.existsRadioLike(session.getRadioSessionId(), currentUserId) > 0);
                session.setFollowed(followMapper.countFollow(currentUserId, session.getUserId()) > 0);
            }
            session.setRecommendedSongs(radioMapper.selectPublicRecommendedSongs(session.getRadioSessionId()));
        }
        return sessions;
    }

    public PublicRadioFeedDto getPublicSession(String id, String currentUserId) {
        if (!StringUtils.hasText(id)) {
            throw new IllegalArgumentException("라디오 세션 ID가 필요합니다.");
        }
        PublicRadioFeedDto session = radioMapper.selectPublicRadioSessionById(id);
        if (session == null) {
            throw new RadioNotFoundException("존재하지 않는 공개 라디오 사연입니다.");
        }
        if (StringUtils.hasText(currentUserId)) {
            session.setLiked(radioLikeMapper.existsRadioLike(id, currentUserId) > 0);
            session.setFollowed(followMapper.countFollow(currentUserId, session.getUserId()) > 0);
        }
        session.setRecommendedSongs(radioMapper.selectPublicRecommendedSongs(id));
        return session;
    }

    @Transactional
    public RadioLikeResponseDto addRadioLike(String userId, String radioSessionId) {
        validateUserId(userId);
        radioLikeMapper.insertRadioLike(radioSessionId, userId);
        return new RadioLikeResponseDto(radioSessionId, true);
    }

    @Transactional
    public RadioLikeResponseDto removeRadioLike(String userId, String radioSessionId) {
        validateUserId(userId);
        radioLikeMapper.deleteRadioLike(radioSessionId, userId);
        return new RadioLikeResponseDto(radioSessionId, false);
    }

    public RadioLikeResponseDto getRadioLikeStatus(String userId, String radioSessionId) {
        validateUserId(userId);
        boolean liked = radioLikeMapper.existsRadioLike(radioSessionId, userId) > 0;
        return new RadioLikeResponseDto(radioSessionId, liked);
    }

    public RadioLikeCountResponseDto getRadioLikeCount(String radioSessionId) {
        int count = radioLikeMapper.countRadioLikes(radioSessionId);
        return new RadioLikeCountResponseDto(radioSessionId, count);
    }

    private String createRadioPlaylist(
            String userId,
            RadioCreateRequestDto request,
            List<RecommendedSongResponseDto> recommendedSongs
    ) {
        List<String> songIds = recommendedSongs.stream()
                .map(RecommendedSongResponseDto::getSongId)
                .filter(StringUtils::hasText)
                .toList();

        if (songIds.isEmpty()) {
            return null;
        }

        PlaylistDto playlist = playlistService.createPlaylist(
                userId,
                PlaylistDto.builder()
                        .name(buildPlaylistName(request))
                        .moodTag(firstNonBlank(request.getDesiredMood(), request.getMood(), ""))
                        .isPublic(false)
                        .build()
        );

        playlistService.addItems(userId, playlist.getId(), songIds);
        return playlist.getId();
    }

    private String buildPlaylistName(RadioCreateRequestDto request) {
        String mood = StringUtils.hasText(request.getMood()) ? request.getMood() : "감성";
        String generation = StringUtils.hasText(request.getGeneration()) ? request.getGeneration() : "";
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        return String.format("%s %s 라디오 - %s", mood, generation, date).replaceAll("\\s+", " ").trim();
    }

    /**
     * YouTube URL을 분석해 9D 임베딩을 얻고 Qdrant 벡터 검색으로 유사곡을 반환한다.
     * DB에 등록된 곡이면 UUID로 searchSimilar, 미등록이면 FastAPI 실시간 분석 후 searchByVector.
     */
    private List<SongDto> findByYoutubeUrlEmbedding(String youtubeUrl, int limit, RadioCreateRequestDto request) {
        String ytId = extractYoutubeId(youtubeUrl);

        // DB에 이미 등록된 곡이고 Qdrant에 벡터가 있으면 UUID 기반 검색 (빠름)
        if (StringUtils.hasText(ytId)) {
            SongDto existing = songDao.selectSongByYoutubeId(ytId);
            if (existing != null && StringUtils.hasText(existing.getId())) {
                // generation/genre로 거르고 나면 후보가 줄어들 수 있으므로 넉넉하게 가져온 뒤 필터링한다.
                List<String> similarIds = qdrantService.searchSimilar(existing.getId(), (limit - 1) * QDRANT_CANDIDATE_POOL_FACTOR);
                if (!similarIds.isEmpty()) {
                    List<SongDto> result = new ArrayList<>();
                    result.add(existing); // 사용자가 입력한 곡 자체는 필터 없이 항상 포함
                    for (String id : similarIds) {
                        if (result.size() >= limit) break;
                        if (id.equals(existing.getId())) continue;
                        SongDto s = songService.getSongById(id);
                        if (s != null && matchesEraAndGenre(s, request)) result.add(s);
                    }
                    return result;
                }
            }
        }

        // DB에 없거나 Qdrant 미등록 → FastAPI 실시간 분석 (RestTemplate 4분 타임아웃)
        try {
            AnalyzeResponseDto analysis = analysisService.analyzeByUrl(youtubeUrl);
            if (analysis == null || analysis.getEmbedding() == null || analysis.getEmbedding().isEmpty()) {
                return List.of();
            }
            return qdrantService.searchByVector(analysis.getEmbedding(), limit * QDRANT_CANDIDATE_POOL_FACTOR).stream()
                    .map(songService::getSongById)
                    .filter(s -> s != null && matchesEraAndGenre(s, request))
                    .limit(limit)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("YouTube URL 임베딩 검색 실패: {}", e.getMessage());
            return List.of();
        }
    }

    private List<SongDto> expandWithQdrant(List<SongDto> seedSongs, int totalLimit, RadioCreateRequestDto request) {
        if (seedSongs.isEmpty()) {
            return seedSongs;
        }

        List<SongDto> result = new ArrayList<>(seedSongs);
        Set<String> seenIds = result.stream()
                .map(SongDto::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        String seedId = seedSongs.get(0).getId();
        // era/genre로 거르고 나면 후보가 줄어들 수 있으므로 필요한 개수보다 넉넉하게 가져온 뒤 필터링한다.
        List<String> similarIds = qdrantService.searchSimilar(seedId, totalLimit * QDRANT_CANDIDATE_POOL_FACTOR);

        for (String id : similarIds) {
            if (result.size() >= totalLimit) {
                break;
            }
            if (seenIds.contains(id)) {
                continue;
            }
            SongDto song = songService.getSongById(id);
            if (song != null && matchesEraAndGenre(song, request)) {
                result.add(song);
                seenIds.add(id);
            }
        }
        return result;
    }

    /**
     * Qdrant 유사도 확장 후보가 요청한 generation(세대)/genre와 일치하는지 검사한다.
     * era 컬럼(legacy 짧은 코드)이 아니라 generation 컬럼(예: "2세대"/"3세대" 라벨)로 비교한다.
     * generation이 "전체"(ALL)이거나 미입력이면 세대 조건은 건너뛴다. genre도 미입력이면 건너뛴다.
     */
    private boolean matchesEraAndGenre(SongDto song, RadioCreateRequestDto request) {
        String requestGeneration = request.getGeneration();
        boolean allGenerations = GenerationNormalizer.normalize(requestGeneration)
                .map(code -> code == GenerationCode.ALL)
                .orElse(false);
        if (!allGenerations && StringUtils.hasText(requestGeneration)
                && !requestGeneration.equals(song.getGeneration())) {
            return false;
        }

        String requestGenre = request.getGenre();
        if (StringUtils.hasText(requestGenre) && !requestGenre.equals(song.getGenre())) {
            return false;
        }

        return true;
    }

    /**
     * 추천 폴백 체인. genre/generation은 항상 강제하고, 완화 가능한 건 mood뿐이다
     * (genre를 선택해도 무시되는 일이 없도록 — docs 참고).
     */
    private RecommendationResult recommendSongs(
            String mood, String generation, String genre,
            UserPreferenceDto preference, String excludedKeywords, int limit
    ) {
        // moodCode 우선 단계: song_moods 정규화 테이블 기반. 결과가 없을 때만
        // 바로 아래의 기존(레거시) songs.mood 문자열 기반 단계로 폴백한다.
        String moodCode = MoodNormalizer.normalize(mood).map(MoodCode::name).orElse(null);
        // generation이 "전체"(ALL)면 세대 조건을 적용하지 않는다(곡에 ALL을 저장하지 않으므로
        // 세대 일치 쿼리는 의미가 없다).
        boolean allGenerations = GenerationNormalizer.normalize(generation)
                .map(code -> code == GenerationCode.ALL)
                .orElse(false);
        String generationFilter = allGenerations ? null : generation;

        List<SongDto> songs;
        if (!allGenerations) {
            songs = safeFindByMoodCodeEraGenre(moodCode, generation, genre, excludedKeywords, limit);
            if (!songs.isEmpty()) return new RecommendationResult("SONG_MOODS_MOOD_ERA_GENRE", songs);
        }

        songs = safeFindByMoodEraGenre(mood, generationFilter, genre, excludedKeywords, limit);
        if (!songs.isEmpty()) return new RecommendationResult("DB_MOOD_ERA_GENRE", songs);

        songs = safeFindByEraAndGenre(generationFilter, genre, excludedKeywords, limit);
        if (!songs.isEmpty()) return new RecommendationResult("DB_ERA_GENRE", songs);

        songs = safeFindByPreference(preference, generationFilter, genre, excludedKeywords, limit);
        if (!songs.isEmpty()) return new RecommendationResult("DB_USER_PREFERENCE_FALLBACK", songs);

        songs = safeFindTopScore(generationFilter, genre, limit);
        if (!songs.isEmpty()) return new RecommendationResult("DB_SCORE_FALLBACK", songs);

        return new RecommendationResult("DB_EMPTY", List.of());
    }

    private List<SongDto> safeFindByMoodCodeEraGenre(String moodCode, String generation, String genre,
                                                       String excludedKeywords, int limit) {
        if (moodCode == null || !StringUtils.hasText(generation) || !StringUtils.hasText(genre)) {
            return List.of();
        }
        try {
            return songDao.findRecommendedSongsByMoodCodeEraGenre(moodCode, generation, genre, excludedKeywords, limit);
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<SongDto> safeFindByMoodEraGenre(String mood, String generation, String genre,
                                                   String excludedKeywords, int limit) {
        if (!StringUtils.hasText(mood) || !StringUtils.hasText(generation) || !StringUtils.hasText(genre)) {
            return List.of();
        }
        try {
            return songDao.findRecommendedSongsByMoodEraGenre(mood, generation, genre, excludedKeywords, limit);
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<SongDto> safeFindByEraAndGenre(String generation, String genre,
                                                  String excludedKeywords, int limit) {
        if (!StringUtils.hasText(genre)) return List.of();
        try {
            return songDao.findRecommendedSongsByEraAndGenre(generation, genre, excludedKeywords, limit);
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<SongDto> safeFindByPreference(UserPreferenceDto preference, String generation, String genre,
                                                String excludedKeywords, int limit) {
        if (preference == null || !hasAnyPreference(preference)) return List.of();
        try {
            return songDao.findRecommendedSongsByPreference(
                    preference.getPreferredMoods(),
                    preference.getPreferredArtists(),
                    generation,
                    genre,
                    excludedKeywords,
                    limit
            );
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<SongDto> safeFindTopScore(String generation, String genre, int limit) {
        try {
            return songDao.findTopScoreSongs(generation, genre, limit);
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<RecommendedSongResponseDto> toRecommendedSongs(List<SongDto> songs, RadioCreateRequestDto request) {
        List<RecommendedSongResponseDto> responses = new ArrayList<>();
        for (SongDto song : songs) {
            responses.add(RecommendedSongResponseDto.builder()
                    .songId(song.getId())
                    .title(song.getTitle())
                    .artist(song.getArtist())
                    .generation(song.getGeneration())
                    .genre(song.getGenre())
                    .youtubeUrl(resolveYoutubeUrl(song))
                    .youtubeId(song.getYoutubeId())
                    .reason(buildReason(song, request))
                    .build());
        }
        return responses;
    }

    private String buildReason(SongDto song, RadioCreateRequestDto request) {
        String generationLabel = song.getGeneration();
        String mood = request.getMood();
        String genre = StringUtils.hasText(song.getGenre()) ? song.getGenre() : request.getGenre();
        String situation = StringUtils.hasText(request.getSituation()) ? " " + request.getSituation() + " 상황에 맞춰" : "";
        String videoType = StringUtils.hasText(request.getVideoType()) ? " 요청한 " + request.getVideoType() + " 감상 흐름에도 어울립니다." : "";
        if ("2세대".equals(generationLabel)) {
            return "2004년~2011년 전후 2세대 K-POP의 강한 후렴과 무대 감성이 있어 " + mood
                    + " 마음을" + situation + " 환기해줄 곡입니다." + videoType;
        }
        if ("3세대".equals(generationLabel)) {
            return "2012년~2017년 전후 3세대 K-POP의 감정선과 청춘 서사가 있어 " + mood
                    + " 기분에" + situation + " 어울리는 곡입니다." + videoType;
        }
        return genre + " 장르의 분위기와 높은 추천 점수를 바탕으로" + situation + " 선곡했습니다." + videoType;
    }

    private String resolveYoutubeUrl(SongDto song) {
        if (StringUtils.hasText(song.getYoutubeUrl())) return song.getYoutubeUrl();
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
        request.setGeneration(toGenerationLabel(firstNonBlank(request.getGeneration(), preferenceService.firstPreferredGeneration(preference), "2세대")));
        request.setGenre(firstNonBlank(request.getGenre(), preferenceService.firstPreferredGenre(preference), ""));
        request.setSituation(firstNonBlank(request.getSituation(), ""));
        request.setDesiredMood(firstNonBlank(request.getDesiredMood(), ""));
        request.setVideoType(firstNonBlank(request.getVideoType(), preferenceService.firstPreferredVideoType(preference), ""));
        request.setPreferredArtist(firstNonBlank(request.getPreferredArtist(), preferenceService.firstPreferredArtist(preference), ""));
        request.setExcludedKeywords(mergeKeywords(request.getExcludedKeywords(), preferenceService.joinedExcludedKeywords(preference)));
    }

    private String toGenerationLabel(String era) {
        if (!StringUtils.hasText(era)) return "미지정";
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
            if (StringUtils.hasText(value)) return value.trim();
        }
        return "";
    }

    private List<SongDto> applyYoutubeUrlSeed(List<SongDto> recommended, String youtubeUrl) {
        if (!StringUtils.hasText(youtubeUrl)) return recommended;
        String ytId = extractYoutubeId(youtubeUrl);
        if (!StringUtils.hasText(ytId)) return recommended;
        SongDto userSong = songDao.selectSongByYoutubeId(ytId);
        if (userSong == null) return recommended;

        List<SongDto> result = new ArrayList<>();
        result.add(userSong);
        for (SongDto s : recommended) {
            if (!userSong.getId().equals(s.getId())) result.add(s);
        }
        return result;
    }

    private String extractYoutubeId(String url) {
        if (!StringUtils.hasText(url)) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern
            .compile("(?:youtu\\.be/|youtube\\.com/(?:watch\\?v=|embed/|shorts/))([a-zA-Z0-9_-]{11})")
            .matcher(url);
        if (m.find()) return m.group(1);
        if (url.matches("[a-zA-Z0-9_-]{11}")) return url;
        return null;
    }

    private record RecommendationResult(String source, List<SongDto> songs) {}
}
