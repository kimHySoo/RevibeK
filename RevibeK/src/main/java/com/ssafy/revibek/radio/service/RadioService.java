package com.ssafy.revibek.radio.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.ssafy.revibek.radio.ai.AiDjMentService;
import com.ssafy.revibek.radio.dto.RadioCreateRequestDto;
import com.ssafy.revibek.radio.dto.RadioCreateResponseDto;
import com.ssafy.revibek.radio.dto.RadioRequestDto;
import com.ssafy.revibek.radio.dto.RadioResponseDto;
import com.ssafy.revibek.radio.dto.RecommendedSongResponseDto;
import com.ssafy.revibek.radio.dto.TtsFallbackResponseDto;
import com.ssafy.revibek.radio.mapper.RadioMapper;
import com.ssafy.revibek.song.dto.SongDto;
import com.ssafy.revibek.song.mapper.SongDao;
import com.ssafy.revibek.tts.TtsResponseDto;
import com.ssafy.revibek.tts.TtsService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RadioService {

    private static final int DEFAULT_RECOMMENDATION_LIMIT = 5;

    private final RadioMapper radioMapper;
    private final SongDao songDao;
    private final AiDjMentService aiDjMentService;
    private final TtsService ttsService;

    @Transactional
    public RadioCreateResponseDto createRadio(String userId, RadioCreateRequestDto request) {
        validateUserId(userId);
        normalizeRequest(request);

        RecommendationResult recommendationResult = recommendSongs(
            normalizeEraForDb(request.getEra()),
            request.getGenre(),
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
            djMent
        );

        for (int i = 0; i < recommendedSongs.size(); i++) {
            RecommendedSongResponseDto song = recommendedSongs.get(i);
            if (StringUtils.hasText(song.getSongId())) {
                radioMapper.insertRecommendation(sessionId, song.getSongId(), i + 1, song.getReason());
            }
        }

        TtsResponseDto tts = ttsService.synthesize(djMent);
        return RadioCreateResponseDto.builder()
            .radioSessionId(sessionId)
            .userId(userId)
            .mood(request.getMood())
            .story(request.getStory())
            .era(request.getEra())
            .genre(request.getGenre())
            .djMent(djMent)
            .recommendationSource(recommendationResult.source())
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

    private RecommendationResult recommendSongs(String era, String genre, int limit) {
        List<SongDto> songs = safeFindByEraAndGenre(era, genre, limit);
        if (!songs.isEmpty()) {
            return new RecommendationResult("DB_ERA_GENRE", songs);
        }

        songs = safeFindByEra(era, limit);
        if (!songs.isEmpty()) {
            return new RecommendationResult("DB_ERA_FALLBACK", songs);
        }

        songs = safeFindByGenre(genre, limit);
        if (!songs.isEmpty()) {
            return new RecommendationResult("DB_GENRE_FALLBACK", songs);
        }

        songs = safeFindTopScore(limit);
        if (!songs.isEmpty()) {
            return new RecommendationResult("DB_SCORE_FALLBACK", songs);
        }

        return new RecommendationResult("DB_EMPTY", List.of());
    }

    private List<SongDto> safeFindByEraAndGenre(String era, String genre, int limit) {
        try {
            return songDao.findRecommendedSongsByEraAndGenre(era, genre, limit);
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<SongDto> safeFindByEra(String era, int limit) {
        try {
            return songDao.findRecommendedSongsByEra(era, limit);
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<SongDto> safeFindByGenre(String genre, int limit) {
        try {
            return songDao.findRecommendedSongsByGenre(genre, limit);
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
        if ("2세대".equals(eraLabel)) {
            return "2004년~2011년 전후 2세대 K-POP의 강한 후렴과 무대 감성이 있어 " + mood
                + " 마음을 환기해줄 곡입니다.";
        }
        if ("3세대".equals(eraLabel)) {
            return "2012년~2017년 전후 3세대 K-POP의 감정선과 청춘 서사가 있어 " + mood
                + " 기분에 어울리는 곡입니다.";
        }
        return genre + " 장르의 분위기와 높은 추천 점수를 바탕으로 선곡했습니다.";
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

    private void normalizeRequest(RadioCreateRequestDto request) {
        request.setMood(trimOrThrow(request.getMood(), "mood"));
        request.setStory(trimOrThrow(request.getStory(), "story"));
        request.setEra(toGenerationLabel(trimOrThrow(request.getEra(), "era")));
        request.setGenre(trimOrThrow(request.getGenre(), "genre"));
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
