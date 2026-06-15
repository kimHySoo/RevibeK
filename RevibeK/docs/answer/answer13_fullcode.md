# RevibeK 백엔드 수정 파일 전체 코드 확인 결과

## 1. 전체 결론

- 현재 작업 트리의 최종 파일 내용을 읽어 문서화했다. 백엔드 소스, 프론트엔드, Song search 코드는 수정하지 않았다.
- `RadioService.createRadioPlaylist()`는 실제로 존재하며, `PlaylistService.createPlaylist()` 및 `PlaylistService.addItem()` 호출 시그니처와 일치한다.
- `RadioMapper.java`와 `RadioMapper.xml`의 `updateRadioSessionPlaylistId` 이름이 정확히 일치한다.
- 라디오 조회 SELECT에 `playlist_id`가 포함되고, `RadioResponseDto`에 `playlistId`가 존재한다.
- `RadioNotFoundException.java`의 실제 위치는 `src/main/java/com/ssafy/revibek/radio/exception/RadioNotFoundException.java`이다.
- 요청에 적힌 `common/GlobalExceptionHandler.java`가 아니라 실제 위치는 `src/main/java/com/ssafy/revibek/common/exception/GlobalExceptionHandler.java`이다.
- 요청에 적힌 `mapper/RadioMapper.xml`이 아니라 실제 위치는 `src/main/resources/mapper/radio/RadioMapper.xml`이다.

## 2. 실제 수정된 파일 목록

현재 `git status --short`와 기존 작업 결과를 기준으로 확인한 백엔드 수정/추가 파일이다.

- `src/main/java/com/ssafy/revibek/radio/dto/RadioResponseDto.java`
- `src/main/java/com/ssafy/revibek/radio/dto/RadioCreateResponseDto.java`
- `src/main/java/com/ssafy/revibek/radio/mapper/RadioMapper.java`
- `src/main/resources/mapper/radio/RadioMapper.xml`
- `src/main/java/com/ssafy/revibek/radio/service/RadioService.java`
- `src/main/java/com/ssafy/revibek/radio/exception/RadioNotFoundException.java`
- `src/main/java/com/ssafy/revibek/common/exception/GlobalExceptionHandler.java`
- `src/main/resources/sql/kpop_radio_schema.sql`
- `src/main/resources/sql/migration_add_radio_session_playlist_id.sql`

확인만 했고 현재 수정 파일로 표시되지 않은 파일:

- `src/main/java/com/ssafy/revibek/radio/dto/RadioCreateRequestDto.java`
- `src/main/java/com/ssafy/revibek/radio/controller/RadioController.java`
- `src/main/java/com/ssafy/revibek/playlist/service/PlaylistService.java`
- `src/main/java/com/ssafy/revibek/playlist/controller/PlaylistController.java`

## 3. RadioResponseDto.java 전체 코드

실제 경로: `src/main/java/com/ssafy/revibek/radio/dto/RadioResponseDto.java`

```java
package com.ssafy.revibek.radio.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RadioResponseDto {
	
	private String id;
	private String playlistId;
	private String mood;
	private String story;
	private String era;
	private String genre;
	private String situation;
	private String desiredMood;
	private String videoType;
	private String preferredArtist;
	private String excludedKeywords;
	private String recommendationSource;
	private String djMent;
	private String comfortText;
	// [FIX] DB 컬럼 novel_excerpt 와 camelCase 매핑이 맞도록 필드명 수정.
	private String novelExcerpt;
	private LocalDateTime createdAt;
	private List<RadioSongDto> songs;
	
	@Data
	@NoArgsConstructor
	public static class RadioSongDto{
		private String songId;
		private String title;
		private String artist;
		private int orderNum;
		private String reason;
	}
	
	

}
```

## 4. RadioCreateResponseDto.java 전체 코드

실제 경로: `src/main/java/com/ssafy/revibek/radio/dto/RadioCreateResponseDto.java`

```java
package com.ssafy.revibek.radio.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RadioCreateResponseDto {

    private String radioSessionId;
    private String playlistId;
    private String userId;
    private String mood;
    private String story;
    private String era;
    private String genre;
    private String situation;
    private String desiredMood;
    private String videoType;
    private String preferredArtist;
    private String excludedKeywords;
    private String djMent;
    private String recommendationSource;
    private TtsFallbackResponseDto tts;
    private List<RecommendedSongResponseDto> recommendedSongs;
}
```

## 5. RadioCreateRequestDto.java 전체 코드

실제 경로: `src/main/java/com/ssafy/revibek/radio/dto/RadioCreateRequestDto.java`

```java
package com.ssafy.revibek.radio.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RadioCreateRequestDto {

    private String mood;
    private String story;
    private String era;
    private String genre;
    private String situation;
    private String desiredMood;
    private String videoType;
    private String preferredArtist;
    private String excludedKeywords;
}
```

## 6. RadioMapper.java 전체 코드

실제 경로: `src/main/java/com/ssafy/revibek/radio/mapper/RadioMapper.java`

```java
package com.ssafy.revibek.radio.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.revibek.radio.dto.RadioResponseDto;

@Mapper
public interface RadioMapper {
	
	int insertRadioSession(@Param("id") String id,
						   @Param("userId") String userId,
						   @Param("mood") String mood,
						   @Param("story") String story);
	int insertRadioSessionWithMent(@Param("id") String id,
								   @Param("userId") String userId,
								   @Param("mood") String mood,
								   @Param("story") String story,
								   @Param("era") String era,
								   @Param("genre") String genre,
								   @Param("situation") String situation,
								   @Param("desiredMood") String desiredMood,
								   @Param("videoType") String videoType,
								   @Param("preferredArtist") String preferredArtist,
								   @Param("excludedKeywords") String excludedKeywords,
								   @Param("recommendationSource") String recommendationSource,
								   @Param("djMent") String djMent);
	int updateRadioSessionDjMent(@Param("id") String id,
								 @Param("userId") String userId,
								 @Param("djMent") String djMent);
	void updateRadioSessionPlaylistId(@Param("id") String id,
									  @Param("userId") String userId,
									  @Param("playlistId") String playlistId);
	RadioResponseDto selectRadioSessionByIdAndUserId(@Param("id") String id,
													 @Param("userId") String userId);
	List<RadioResponseDto> selectRadioSessionByUserId(@Param("userId") String userId);
	void insertRecommendation(@Param("sessionId") String sessionId,
							  @Param("songId")	String songId,
							  @Param("orderNum") int orderNum,
							  @Param("reason") String reason);
	List<RadioResponseDto.RadioSongDto> selectRecommendationBySessionId(@Param("sessionId") String sessionId);	
			
			
			
	
	
	

}
```

## 7. RadioMapper.xml 전체 코드

실제 경로: `src/main/resources/mapper/radio/RadioMapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.ssafy.revibek.radio.mapper.RadioMapper">

    <!-- 세션 저장 -->
    <insert id="insertRadioSession">
        INSERT INTO radio_sessions (id, user_id, mood, story)
        VALUES (#{id}, #{userId}, #{mood}, #{story})
    </insert>

    <insert id="insertRadioSessionWithMent">
        INSERT INTO radio_sessions (
        id, user_id, mood, story, era, genre, situation,
        desired_mood, video_type, preferred_artist,
        excluded_keywords, recommendation_source, dj_ment
        ) VALUES (
        #{id}, #{userId}, #{mood}, #{story}, #{era}, #{genre}, #{situation},
        #{desiredMood}, #{videoType}, #{preferredArtist},
        #{excludedKeywords}, #{recommendationSource}, #{djMent}
        )
    </insert>

    <update id="updateRadioSessionDjMent">
        UPDATE radio_sessions
        SET dj_ment = #{djMent}
        WHERE id = #{id}
        AND user_id = #{userId}
    </update>

    <update id="updateRadioSessionPlaylistId">
        UPDATE radio_sessions
        SET playlist_id = #{playlistId}
        WHERE id = #{id}
        AND user_id = #{userId}
    </update>

    <!-- 세션 단건 조회 -->
    <select id="selectRadioSessionByIdAndUserId"
            resultType="com.ssafy.revibek.radio.dto.RadioResponseDto">
        SELECT
        id, playlist_id, mood, story, era, genre, situation,
        desired_mood, video_type, preferred_artist,
        excluded_keywords, recommendation_source,
        dj_ment, comfort_text, novel_excerpt, created_at
        FROM radio_sessions
        WHERE id = #{id}
        AND user_id = #{userId}
    </select>

    <!-- 유저의 세션 목록 -->
    <select id="selectRadioSessionByUserId" parameterType="string"
            resultType="com.ssafy.revibek.radio.dto.RadioResponseDto">
        SELECT
        id, playlist_id, mood, story, era, genre, situation,
        desired_mood, video_type, preferred_artist,
        excluded_keywords, recommendation_source,
        dj_ment, comfort_text, novel_excerpt, created_at
        FROM radio_sessions
        WHERE user_id = #{userId}
        ORDER BY created_at DESC
    </select>

    <!-- 추천곡 저장 -->
    <insert id="insertRecommendation">
        INSERT INTO radio_recommendations (
        session_id, song_id, order_num, reason
        ) VALUES (
        #{sessionId}, #{songId}, #{orderNum}, #{reason}
        )
    </insert>

    <!-- 세션별 추천곡 조회 -->
    <select id="selectRecommendationBySessionId" parameterType="string"
            resultType="com.ssafy.revibek.radio.dto.RadioResponseDto$RadioSongDto">
        SELECT
        rr.song_id,
        s.title,
        s.artist,
        rr.order_num AS order_num,
        rr.reason
        FROM radio_recommendations rr
        JOIN songs s ON s.id = rr.song_id
        WHERE rr.session_id = #{sessionId}
        ORDER BY rr.order_num
    </select>

</mapper>
```

## 8. RadioService.java 전체 코드

실제 경로: `src/main/java/com/ssafy/revibek/radio/service/RadioService.java`

```java
package com.ssafy.revibek.radio.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.ssafy.revibek.radio.ai.AiDjMentService;
import com.ssafy.revibek.playlist.dto.PlaylistDto;
import com.ssafy.revibek.playlist.dto.PlaylistItemDto;
import com.ssafy.revibek.playlist.service.PlaylistService;
import com.ssafy.revibek.radio.dto.RadioCreateRequestDto;
import com.ssafy.revibek.radio.dto.RadioCreateResponseDto;
import com.ssafy.revibek.radio.dto.RadioRequestDto;
import com.ssafy.revibek.radio.dto.RadioResponseDto;
import com.ssafy.revibek.radio.dto.RecommendedSongResponseDto;
import com.ssafy.revibek.radio.dto.TtsFallbackResponseDto;
import com.ssafy.revibek.radio.mapper.RadioMapper;
import com.ssafy.revibek.radio.exception.RadioNotFoundException;
import com.ssafy.revibek.preference.dto.UserPreferenceDto;
import com.ssafy.revibek.preference.service.PreferenceService;
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
    private final PreferenceService preferenceService;
    private final PlaylistService playlistService;

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
                radioMapper.insertRecommendation(sessionId, song.getSongId(), i + 1, song.getReason());
            }
        }

        String playlistId = createRadioPlaylist(userId, request, recommendedSongs);
        if (playlistId != null) {
            radioMapper.updateRadioSessionPlaylistId(sessionId, userId, playlistId);
        }

        TtsResponseDto tts = ttsService.synthesize(djMent);
        return RadioCreateResponseDto.builder()
            .radioSessionId(sessionId)
            .playlistId(playlistId)
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
			throw new RadioNotFoundException("존재하지 않는 라디오 세션이거나 접근 권한이 없습니다.");
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

    private String createRadioPlaylist(
        String userId,
        RadioCreateRequestDto request,
        List<RecommendedSongResponseDto> recommendedSongs
    ) {
        if (recommendedSongs.isEmpty()) {
            return null;
        }

        PlaylistDto playlist = playlistService.createPlaylist(
            userId,
            PlaylistDto.builder()
                .name(firstNonBlank(request.getMood(), "Radio") + " Radio")
                .moodTag(firstNonBlank(request.getDesiredMood(), request.getMood(), ""))
                .isPublic(false)
                .build()
        );

        for (RecommendedSongResponseDto song : recommendedSongs) {
            if (StringUtils.hasText(song.getSongId())) {
                playlistService.addItem(
                    userId,
                    playlist.getId(),
                    PlaylistItemDto.builder().songId(song.getSongId()).build()
                );
            }
        }
        return playlist.getId();
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
```

## 9. RadioNotFoundException.java 전체 코드

검색 결과 실제 경로: `src/main/java/com/ssafy/revibek/radio/exception/RadioNotFoundException.java`

```java
package com.ssafy.revibek.radio.exception;

public class RadioNotFoundException extends RuntimeException {

    public RadioNotFoundException(String message) {
        super(message);
    }
}
```

## 10. GlobalExceptionHandler.java 전체 코드

실제 경로: `src/main/java/com/ssafy/revibek/common/exception/GlobalExceptionHandler.java`

```java
package com.ssafy.revibek.common.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ssafy.revibek.common.dto.ErrorResponse;
import com.ssafy.revibek.radio.exception.RadioNotFoundException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
        MethodArgumentNotValidException exception,
        HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
            fieldErrors.put(error.getField(), error.getDefaultMessage())
        );

        ErrorResponse response = ErrorResponse.of(
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            "요청값 검증에 실패했습니다.",
            request.getRequestURI(),
            fieldErrors
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(
        IllegalArgumentException exception,
        HttpServletRequest request
    ) {
        ErrorResponse response = ErrorResponse.of(
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            exception.getMessage(),
            request.getRequestURI()
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(RadioNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRadioNotFound(
        RadioNotFoundException exception,
        HttpServletRequest request
    ) {
        ErrorResponse response = ErrorResponse.of(
            HttpStatus.NOT_FOUND.value(),
            HttpStatus.NOT_FOUND.getReasonPhrase(),
            exception.getMessage(),
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(
        RuntimeException exception,
        HttpServletRequest request
    ) {
        ErrorResponse response = ErrorResponse.of(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
            exception.getMessage(),
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
        Exception exception,
        HttpServletRequest request
    ) {
        ErrorResponse response = ErrorResponse.of(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
            "서버 내부 오류가 발생했습니다.",
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
```

## 11. kpop_radio_schema.sql 전체 코드 또는 radio_sessions 관련 전체 DDL

요청대로 일부 DDL만 발췌하지 않고 파일 전체를 포함한다.

실제 경로: `src/main/resources/sql/kpop_radio_schema.sql`

```sql
-- ============================================
-- K-POP AI 라디오 서비스 — MySQL Schema + Mock Data
-- 작성일: 2025-05-22
-- 스택: Spring Boot + MyBatis + MySQL 8.0
-- ============================================

-- DB 생성
CREATE DATABASE IF NOT EXISTS kpop_radio
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE kpop_radio;

-- ============================================
-- DDL — 테이블 생성
-- ============================================

-- 1. USERS
CREATE TABLE users (
  id            CHAR(36)        NOT NULL DEFAULT (UUID()),
  nickname      VARCHAR(50)     NOT NULL,
  email         VARCHAR(100)    NOT NULL UNIQUE,
  provider      VARCHAR(20)     NOT NULL COMMENT 'local | google | kakao',
  provider_id   VARCHAR(100)    NULL     COMMENT '소셜 로그인 고유 ID',
  password_hash VARCHAR(255)    NULL     COMMENT 'local 로그인 시 사용',
  created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_email (email),
  INDEX idx_provider (provider, provider_id)
) ENGINE=InnoDB COMMENT='유저 정보';


-- 2. SONGS
CREATE TABLE songs (
  id                CHAR(36)        NOT NULL DEFAULT (UUID()),
  title             VARCHAR(200)    NOT NULL,
  artist            VARCHAR(100)    NOT NULL,
  genre             VARCHAR(50)     NOT NULL COMMENT '발라드 | 댄스 | 힙합 | R&B | 록',
  era               VARCHAR(20)     NOT NULL COMMENT '90s | 00s | 10s | 20s',
  generation        VARCHAR(20)     NULL     COMMENT '2세대 | 3세대 등 서비스 표시용 세대',
  mood              VARCHAR(50)     NULL     COMMENT '추천/라디오용 분위기 태그',
  type              VARCHAR(20)     NOT NULL COMMENT 'original | ai_remix',
  youtube_url       VARCHAR(300)    NOT NULL,
  youtube_id        VARCHAR(50)     NOT NULL COMMENT 'YouTube 영상 ID',
  thumbnail_url     VARCHAR(500)    NULL,
  view_count        INT             NOT NULL DEFAULT 0,
  like_count        INT             NOT NULL DEFAULT 0,
  trend_score       FLOAT           NOT NULL DEFAULT 0.0 COMMENT '최근 7일 증가율 기반',
  score             FLOAT           NOT NULL DEFAULT 0.0 COMMENT '가중 합산 점수 (0~100)',
  score_updated_at  DATETIME        NULL,
  released_at       DATE            NULL,
  duration_seconds  INT             NULL,
  bpm               DOUBLE          NULL,
  energy            DOUBLE          NULL,
  danceability      DOUBLE          NULL,
  loudness          DOUBLE          NULL,
  musical_key       VARCHAR(10)     NULL,
  musical_scale     VARCHAR(20)     NULL,
  beats_count       INT             NULL,
  beats_confidence  DOUBLE          NULL,
  key_strength      DOUBLE          NULL,
  spectral_centroid DOUBLE          NULL,
  zero_crossing_rate DOUBLE         NULL,
  is_analyzed       TINYINT(1)      NOT NULL DEFAULT 0,
  created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_type (type),
  INDEX idx_genre (genre),
  INDEX idx_era (era),
  INDEX idx_generation (generation),
  INDEX idx_analyzed (is_analyzed),
  INDEX idx_score (score DESC),
  INDEX idx_youtube_id (youtube_id)
) ENGINE=InnoDB COMMENT='원곡 및 AI 리믹스 노래 정보';


-- 3. USER_SONGS (저장 / 평가 / 재생 이력)
CREATE TABLE user_songs (
  id              CHAR(36)    NOT NULL DEFAULT (UUID()),
  user_id         CHAR(36)    NOT NULL,
  song_id         CHAR(36)    NOT NULL,
  is_saved        TINYINT(1)  NOT NULL DEFAULT 0,
  rating          TINYINT     NULL     COMMENT '1~5점, NULL이면 미평가',
  play_count      INT         NOT NULL DEFAULT 0,
  last_played_at  DATETIME    NULL,
  created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_user_song (user_id, song_id),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (song_id) REFERENCES songs(id) ON DELETE CASCADE,
  INDEX idx_user_saved (user_id, is_saved),
  INDEX idx_user_rating (user_id, rating)
) ENGINE=InnoDB COMMENT='유저별 노래 저장/평가/재생 이력';

-- 3-1. USER_PREFERENCES (첫 사용/온보딩 음악 취향)
CREATE TABLE user_preferences (
  id                     CHAR(36)    NOT NULL DEFAULT (UUID()),
  user_id                CHAR(36)    NOT NULL,
  preferred_generations  JSON        NULL COMMENT '2세대 | 3세대 | 4세대 | 5세대',
  preferred_moods        JSON        NULL COMMENT '신나는 | 위로 | 감성 | 몽환 | 청량 | 강렬함',
  preferred_artists      JSON        NULL,
  preferred_genres       JSON        NULL,
  preferred_video_types  JSON        NULL COMMENT '원곡 | 라이브 | 커버 | 리믹스 | 무대영상',
  excluded_genres        JSON        NULL,
  excluded_keywords      JSON        NULL,
  created_at             DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at             DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_user_preferences_user (user_id),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_user_preferences_user (user_id)
) ENGINE=InnoDB COMMENT='사용자 음악 취향';


-- 4. RADIO_SESSIONS (라디오 생성 이력)
CREATE TABLE radio_sessions (
  id              CHAR(36)    NOT NULL DEFAULT (UUID()),
  user_id         CHAR(36)    NOT NULL,
  mood            VARCHAR(50) NOT NULL COMMENT '외로운 | 설레는 | 그리운 | 지친 | 행복한 | 슬픈',
  story           TEXT        NULL,
  era             VARCHAR(20) NULL,
  genre           VARCHAR(50) NULL,
  situation       VARCHAR(200) NULL,
  desired_mood    VARCHAR(50) NULL,
  video_type      VARCHAR(50) NULL,
  preferred_artist VARCHAR(100) NULL,
  excluded_keywords VARCHAR(500) NULL,
  recommendation_source VARCHAR(50) NULL,
  dj_ment        TEXT        NULL     COMMENT 'Claude API 생성 DJ 멘트',
  comfort_text    TEXT        NULL     COMMENT 'AI 위로 메시지',
  novel_excerpt   TEXT        NULL     COMMENT '활용된 소설 구절',
  playlist_id     CHAR(36)    NULL,
  created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_user_session (user_id, created_at DESC),
  INDEX idx_mood (mood),
  INDEX idx_radio_era_genre (era, genre),
  INDEX idx_radio_desired_mood (desired_mood)
) ENGINE=InnoDB COMMENT='AI 라디오 세션 이력';


-- 5. RADIO_RECOMMENDATIONS (라디오 추천 곡)
CREATE TABLE radio_recommendations (
  id          CHAR(36)    NOT NULL DEFAULT (UUID()),
  session_id  CHAR(36)    NOT NULL,
  song_id     CHAR(36)    NOT NULL,
  order_num   TINYINT     NOT NULL DEFAULT 1 COMMENT '추천 순서',
  reason      VARCHAR(200) NULL    COMMENT '추천 이유 (DJ 멘트 생성에 재활용)',
  PRIMARY KEY (id),
  FOREIGN KEY (session_id) REFERENCES radio_sessions(id) ON DELETE CASCADE,
  FOREIGN KEY (song_id)    REFERENCES songs(id) ON DELETE CASCADE,
  INDEX idx_session (session_id, order_num)
) ENGINE=InnoDB COMMENT='라디오 세션 추천 곡 목록';


-- 6. SCORE_LOGS (점수 변경 이력)
CREATE TABLE score_logs (
  id            CHAR(36)    NOT NULL DEFAULT (UUID()),
  song_id       CHAR(36)    NOT NULL,
  score_before  FLOAT       NOT NULL,
  score_after   FLOAT       NOT NULL,
  view_count    INT         NOT NULL,
  like_count    INT         NOT NULL,
  trend_score   FLOAT       NOT NULL,
  logged_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  FOREIGN KEY (song_id) REFERENCES songs(id) ON DELETE CASCADE,
  INDEX idx_song_log (song_id, logged_at DESC)
) ENGINE=InnoDB COMMENT='노래 점수 배치 갱신 이력';


-- 7. PLAYLISTS
CREATE TABLE playlists (
  id          CHAR(36)        NOT NULL DEFAULT (UUID()),
  user_id     CHAR(36)        NOT NULL,
  name        VARCHAR(100)    NOT NULL,
  mood_tag    VARCHAR(50)     NULL,
  is_public   TINYINT(1)      NOT NULL DEFAULT 0,
  created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_user_playlist (user_id),
  INDEX idx_public (is_public)
) ENGINE=InnoDB COMMENT='유저 생성 플레이리스트';


-- 8. PLAYLIST_SONGS
CREATE TABLE playlist_songs (
  id           CHAR(36)    NOT NULL DEFAULT (UUID()),
  playlist_id  CHAR(36)    NOT NULL,
  song_id      CHAR(36)    NOT NULL,
  order_num    SMALLINT    NOT NULL DEFAULT 1,
  added_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_playlist_song (playlist_id, song_id),
  FOREIGN KEY (playlist_id) REFERENCES playlists(id) ON DELETE CASCADE,
  FOREIGN KEY (song_id)     REFERENCES songs(id)     ON DELETE CASCADE,
  INDEX idx_playlist_order (playlist_id, order_num)
) ENGINE=InnoDB COMMENT='플레이리스트 구성 곡';

ALTER TABLE radio_sessions
  ADD CONSTRAINT fk_radio_sessions_playlist
  FOREIGN KEY (playlist_id) REFERENCES playlists(id) ON DELETE SET NULL;

-- 9. 채널 테이블
CREATE TABLE youtube_channels (
  id                CHAR(36)      NOT NULL DEFAULT (UUID()),
  url               VARCHAR(255)  NOT NULL,
  channel_id        VARCHAR(50)   UNIQUE,
  channel_name      VARCHAR(255),
  uploads_playlist  VARCHAR(60),
  subscriber_count  BIGINT,
  last_checked_at   DATETIME,
  created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='유튜브 채널 목록';

-- 10. 크롤링된 영상 후보 테이블 (songs 반영 전 대기열)
CREATE TABLE youtube_videos_raw (
  id            CHAR(36)      NOT NULL DEFAULT (UUID()),
  channel_id    VARCHAR(50)   NOT NULL,
  video_id      VARCHAR(20)   NOT NULL UNIQUE,  -- 중복 방지
  video_url     VARCHAR(300),
  title         VARCHAR(500),
  duration_seconds INT        NULL,
  published_at  DATETIME,
  is_imported   TINYINT(1)    NOT NULL DEFAULT 0  COMMENT 'songs 테이블 반영 여부',
  is_analyzed   TINYINT(1)    NOT NULL DEFAULT 0  COMMENT 'FastAPI 분석 여부',
  collect_status VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
  fetched_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  FOREIGN KEY (channel_id) REFERENCES youtube_channels(channel_id),
  INDEX idx_imported (is_imported),
  INDEX idx_analyzed (is_analyzed),
  INDEX idx_collect_status (collect_status),
  INDEX idx_published (published_at DESC)
) ENGINE=InnoDB COMMENT='유튜브 영상 수집 대기열';

-- 11. SONG_LIKES (사용자별 곡 좋아요)
CREATE TABLE song_likes (
  id          CHAR(36)    NOT NULL DEFAULT (UUID()),
  user_id     CHAR(36)    NOT NULL,
  song_id     CHAR(36)    NOT NULL,
  created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_user_song_like (user_id, song_id),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (song_id) REFERENCES songs(id) ON DELETE CASCADE,
  INDEX idx_like_user (user_id),
  INDEX idx_like_song (song_id)
) ENGINE=InnoDB COMMENT='사용자별 곡 좋아요';

-- ============================================
-- MOCK DATA — 목업 데이터 INSERT
-- ============================================

-- ① USERS (5명)
INSERT INTO users (id, nickname, email, provider, provider_id, password_hash) VALUES
  ('u001-0000-0000-0000-000000000001', '감성덕후', 'user1@example.com', 'google',  'g_001', NULL),
  ('u002-0000-0000-0000-000000000002', '새벽세시', 'user2@example.com', 'kakao',   'k_002', NULL),
  ('u003-0000-0000-0000-000000000003', '레트로킹', 'user3@example.com', 'local',   NULL,    '$2a$10$mockHashValue1'),
  ('u004-0000-0000-0000-000000000004', '별빛수집가', 'user4@example.com','google', 'g_004', NULL),
  ('u005-0000-0000-0000-000000000005', '추억여행자', 'user5@example.com','local',  NULL,    '$2a$10$mockHashValue2');
show tables;

-- ② SONGS (원곡 10곡 + AI 리믹스 10곡)
INSERT INTO songs (id, title, artist, genre, era, type, youtube_url, youtube_id, view_count, like_count, trend_score, score, score_updated_at, released_at) VALUES

-- 원곡
('s001-0000-0000-0000-000000000001', '캔디', 'H.O.T', '댄스', '90s', 'original',
 'https://www.youtube.com/watch?v=dummy001', 'dummy001', 4500000, 82000, 55.0, 72.5, NOW(), '1996-09-15'),

('s002-0000-0000-0000-000000000002', 'To Heaven', 'god', '발라드', '00s', 'original',
 'https://www.youtube.com/watch?v=dummy002', 'dummy002', 3800000, 95000, 48.0, 75.2, NOW(), '2001-05-10'),

('s003-0000-0000-0000-000000000003', '여보세요', '핑클', '댄스', '90s', 'original',
 'https://www.youtube.com/watch?v=dummy003', 'dummy003', 2900000, 71000, 42.0, 65.8, NOW(), '1998-10-01'),

('s004-0000-0000-0000-000000000004', '내 사람', 'SG워너비', '발라드', '00s', 'original',
 'https://www.youtube.com/watch?v=dummy004', 'dummy004', 5200000, 98000, 52.0, 78.1, NOW(), '2004-03-22'),

('s005-0000-0000-0000-000000000005', '가시', '버스커버스커', '발라드', '10s', 'original',
 'https://www.youtube.com/watch?v=dummy005', 'dummy005', 8900000, 155000, 70.0, 88.3, NOW(), '2012-03-29'),

('s006-0000-0000-0000-000000000006', '고해', '이소라', '발라드', '00s', 'original',
 'https://www.youtube.com/watch?v=dummy006', 'dummy006', 2100000, 88000, 38.0, 62.4, NOW(), '2003-11-05'),

('s007-0000-0000-0000-000000000007', '쿵따리샤바라', '클론', '댄스', '90s', 'original',
 'https://www.youtube.com/watch?v=dummy007', 'dummy007', 3300000, 67000, 61.0, 69.7, NOW(), '1996-06-01'),

('s008-0000-0000-0000-000000000008', '기사도', '젝스키스', '댄스', '90s', 'original',
 'https://www.youtube.com/watch?v=dummy008', 'dummy008', 2600000, 59000, 45.0, 63.2, NOW(), '1997-04-10'),

('s009-0000-0000-0000-000000000009', '인연', '이선희', '발라드', '00s', 'original',
 'https://www.youtube.com/watch?v=dummy009', 'dummy009', 6100000, 112000, 58.0, 82.6, NOW(), '2006-08-15'),

('s010-0000-0000-0000-000000000010', '여수 밤바다', '버스커버스커', '발라드', '10s', 'original',
 'https://www.youtube.com/watch?v=dummy010', 'dummy010', 7700000, 143000, 66.0, 85.9, NOW(), '2012-03-29'),

-- AI 리믹스
('s011-0000-0000-0000-000000000011', '캔디 (AI 리마스터)', 'H.O.T', '댄스', '90s', 'ai_remix',
 'https://www.youtube.com/watch?v=dummy011', 'dummy011', 1200000, 48000, 88.0, 85.4, NOW(), '2024-01-10'),

('s012-0000-0000-0000-000000000012', 'To Heaven (AI 리마스터)', 'god', '발라드', '00s', 'ai_remix',
 'https://www.youtube.com/watch?v=dummy012', 'dummy012', 980000, 52000, 91.0, 87.2, NOW(), '2024-02-14'),

('s013-0000-0000-0000-000000000013', '여보세요 (AI 리마스터)', '핑클', '댄스', '90s', 'ai_remix',
 'https://www.youtube.com/watch?v=dummy013', 'dummy013', 750000, 38000, 79.0, 78.6, NOW(), '2024-01-25'),

('s014-0000-0000-0000-000000000014', '내 사람 (AI 리마스터)', 'SG워너비', '발라드', '00s', 'ai_remix',
 'https://www.youtube.com/watch?v=dummy014', 'dummy014', 890000, 45000, 82.0, 81.3, NOW(), '2024-03-05'),

('s015-0000-0000-0000-000000000015', '가시 (AI 리마스터)', '버스커버스커', '발라드', '10s', 'ai_remix',
 'https://www.youtube.com/watch?v=dummy015', 'dummy015', 1500000, 71000, 95.0, 92.7, NOW(), '2024-02-28'),

('s016-0000-0000-0000-000000000016', '고해 (AI 리마스터)', '이소라', '발라드', '00s', 'ai_remix',
 'https://www.youtube.com/watch?v=dummy016', 'dummy016', 620000, 41000, 74.0, 73.8, NOW(), '2024-04-01'),

('s017-0000-0000-0000-000000000017', '쿵따리샤바라 (AI 리믹스)', '클론', '댄스', '90s', 'ai_remix',
 'https://www.youtube.com/watch?v=dummy017', 'dummy017', 830000, 36000, 85.0, 79.2, NOW(), '2024-03-20'),

('s018-0000-0000-0000-000000000018', 'Dreams Come True (AI 리마스터)', 'S.E.S', '댄스', '90s', 'ai_remix',
 'https://www.youtube.com/watch?v=dummy018', 'dummy018', 710000, 43000, 80.0, 77.5, NOW(), '2024-01-30'),

('s019-0000-0000-0000-000000000019', '인연 (AI 리마스터)', '이선희', '발라드', '00s', 'ai_remix',
 'https://www.youtube.com/watch?v=dummy019', 'dummy019', 940000, 56000, 87.0, 84.1, NOW(), '2024-04-10'),

('s020-0000-0000-0000-000000000020', '여수 밤바다 (AI 리마스터)', '버스커버스커', '발라드', '10s', 'ai_remix',
 'https://www.youtube.com/watch?v=dummy020', 'dummy020', 1350000, 68000, 93.0, 91.4, NOW(), '2024-03-15');


-- ③ USER_SONGS (저장·평가 데이터)
INSERT INTO user_songs (id, user_id, song_id, is_saved, rating, play_count, last_played_at) VALUES
  ('us01-0000-0000-0000-000000000001', 'u001-0000-0000-0000-000000000001', 's015-0000-0000-0000-000000000015', 1, 5, 12, NOW()),
  ('us02-0000-0000-0000-000000000002', 'u001-0000-0000-0000-000000000001', 's012-0000-0000-0000-000000000012', 1, 4, 8,  NOW()),
  ('us03-0000-0000-0000-000000000003', 'u001-0000-0000-0000-000000000001', 's005-0000-0000-0000-000000000005', 0, 3, 3,  NOW()),
  ('us04-0000-0000-0000-000000000004', 'u002-0000-0000-0000-000000000002', 's020-0000-0000-0000-000000000020', 1, 5, 20, NOW()),
  ('us05-0000-0000-0000-000000000005', 'u002-0000-0000-0000-000000000002', 's011-0000-0000-0000-000000000011', 1, 4, 15, NOW()),
  ('us06-0000-0000-0000-000000000006', 'u002-0000-0000-0000-000000000002', 's009-0000-0000-0000-000000000009', 1, 5, 9,  NOW()),
  ('us07-0000-0000-0000-000000000007', 'u003-0000-0000-0000-000000000003', 's001-0000-0000-0000-000000000001', 1, 5, 25, NOW()),
  ('us08-0000-0000-0000-000000000008', 'u003-0000-0000-0000-000000000003', 's007-0000-0000-0000-000000000007', 1, 4, 18, NOW()),
  ('us09-0000-0000-0000-000000000009', 'u003-0000-0000-0000-000000000003', 's017-0000-0000-0000-000000000017', 1, 4, 11, NOW()),
  ('us10-0000-0000-0000-000000000010', 'u004-0000-0000-0000-000000000004', 's019-0000-0000-0000-000000000019', 1, 5, 7,  NOW()),
  ('us11-0000-0000-0000-000000000011', 'u004-0000-0000-0000-000000000004', 's016-0000-0000-0000-000000000016', 1, 3, 4,  NOW()),
  ('us12-0000-0000-0000-000000000012', 'u005-0000-0000-0000-000000000005', 's004-0000-0000-0000-000000000004', 1, 4, 6,  NOW()),
  ('us13-0000-0000-0000-000000000013', 'u005-0000-0000-0000-000000000005', 's014-0000-0000-0000-000000000014', 1, 5, 10, NOW());


-- ④ RADIO_SESSIONS (라디오 세션 3개)
INSERT INTO radio_sessions (id, user_id, mood, story, dj_ment, comfort_text, novel_excerpt) VALUES
  ('rs01-0000-0000-0000-000000000001',
   'u001-0000-0000-0000-000000000001',
   '그리운',
   '오늘 오래된 사진을 보다가 학창시절이 너무 그리워졌어요.',
   '안녕하세요, DJ 리아예요. 감성덕후님의 사연을 들었어요. 그 시절의 기억이 얼마나 소중한지 느껴져요. 오늘은 그 추억을 음악으로 꺼내볼까요?',
   '그리움은 사랑했던 시간이 남긴 가장 따뜻한 흔적이에요. 오늘 밤, 그 시절의 음악이 당신 곁에 있을게요.',
   '"사람은 누구나 자기만의 시절을 가슴 속에 간직하며 산다." — 박완서, 그 많던 싱아는 누가 다 먹었을까'),

  ('rs02-0000-0000-0000-000000000002',
   'u002-0000-0000-0000-000000000002',
   '지친',
   '요즘 일이 너무 힘들어서 아무것도 하기 싫어요. 그냥 음악이나 듣고 싶어요.',
   '새벽세시님, 오늘 많이 지치셨군요. 아무것도 안 해도 괜찮아요. 지금 이 순간은 그냥 쉬어가도 충분해요.',
   '지쳐도 괜찮아요. 쉬어가는 것도 앞으로 나아가는 용기니까요. 오늘 밤은 음악에 모든 걸 맡겨보세요.',
   '"가끔은 아무것도 하지 않는 것이 가장 용감한 일이다." — 헤르만 헤세, 데미안'),

  ('rs03-0000-0000-0000-000000000003',
   'u004-0000-0000-0000-000000000004',
   '외로운',
   '혼자 있는 밤이 너무 길게 느껴져요. 누군가 옆에 있으면 좋겠어요.',
   '별빛수집가님, 혼자인 밤이 유독 길게 느껴질 때가 있죠. 오늘 밤은 DJ 리아가 함께할게요. 이 음악들이 조용히 옆에 앉아 있을 거예요.',
   '혼자라는 느낌은 때로 마음이 더 넓어지는 시간이기도 해요. 이 노래들이 그 공간을 채워줄 거예요.',
   '"외로움은 혼자 있는 것이 아니라, 이해받지 못한다는 느낌이다." — 프리드리히 니체');


-- ⑤ RADIO_RECOMMENDATIONS (세션별 추천 곡)
INSERT INTO radio_recommendations (id, session_id, song_id, order_num, reason) VALUES
  ('rr01-0000-0000-0000-000000000001', 'rs01-0000-0000-0000-000000000001', 's011-0000-0000-0000-000000000011', 1, '90년대 감성을 AI로 되살린 곡으로 그리움을 자극해요'),
  ('rr02-0000-0000-0000-000000000002', 'rs01-0000-0000-0000-000000000001', 's013-0000-0000-0000-000000000013', 2, '학창시절 누구나 알던 명곡의 AI 리마스터'),
  ('rr03-0000-0000-0000-000000000003', 'rs01-0000-0000-0000-000000000001', 's018-0000-0000-0000-000000000018', 3, '90년대 감성 집약체, 추억 소환에 최적'),
  ('rr04-0000-0000-0000-000000000004', 'rs02-0000-0000-0000-000000000002', 's012-0000-0000-0000-000000000012', 1, '지친 마음을 위로하는 발라드 1순위'),
  ('rr05-0000-0000-0000-000000000005', 'rs02-0000-0000-0000-000000000002', 's016-0000-0000-0000-000000000016', 2, '이소라 특유의 감성이 지친 마음을 어루만져줘요'),
  ('rr06-0000-0000-0000-000000000006', 'rs02-0000-0000-0000-000000000002', 's015-0000-0000-0000-000000000015', 3, '잔잔한 멜로디로 쉬어가기 좋은 곡'),
  ('rr07-0000-0000-0000-000000000007', 'rs03-0000-0000-0000-000000000003', 's020-0000-0000-0000-000000000020', 1, '밤바다 감성으로 외로운 밤을 채워줘요'),
  ('rr08-0000-0000-0000-000000000008', 'rs03-0000-0000-0000-000000000003', 's019-0000-0000-0000-000000000019', 2, '이선희의 따뜻한 목소리가 곁에 있는 느낌'),
  ('rr09-0000-0000-0000-000000000009', 'rs03-0000-0000-0000-000000000003', 's014-0000-0000-0000-000000000014', 3, '포근한 발라드로 외로움을 달래줘요');


-- ⑥ SCORE_LOGS (점수 갱신 이력 샘플)
INSERT INTO score_logs (id, song_id, score_before, score_after, view_count, like_count, trend_score, logged_at) VALUES
  ('sl01-0000-0000-0000-000000000001', 's015-0000-0000-0000-000000000015', 88.1, 92.7, 1500000, 71000, 95.0, NOW()),
  ('sl02-0000-0000-0000-000000000002', 's020-0000-0000-0000-000000000020', 89.2, 91.4, 1350000, 68000, 93.0, NOW()),
  ('sl03-0000-0000-0000-000000000003', 's012-0000-0000-0000-000000000012', 85.0, 87.2, 980000,  52000, 91.0, NOW()),
  ('sl04-0000-0000-0000-000000000004', 's011-0000-0000-0000-000000000011', 82.3, 85.4, 1200000, 48000, 88.0, NOW()),
  ('sl05-0000-0000-0000-000000000005', 's005-0000-0000-0000-000000000005', 87.1, 88.3, 8900000, 155000, 70.0, NOW());


-- ⑦ PLAYLISTS
INSERT INTO playlists (id, user_id, name, mood_tag, is_public) VALUES
  ('pl01-0000-0000-0000-000000000001', 'u001-0000-0000-0000-000000000001', '새벽 감성 모음', '그리운', 1),
  ('pl02-0000-0000-0000-000000000002', 'u002-0000-0000-0000-000000000002', '출퇴근길 위로 플리', '지친', 1),
  ('pl03-0000-0000-0000-000000000003', 'u003-0000-0000-0000-000000000003', '90s 레전드 모음', NULL, 0);


-- ⑧ PLAYLIST_SONGS
INSERT INTO playlist_songs (id, playlist_id, song_id, order_num) VALUES
  ('ps01-0000-0000-0000-000000000001', 'pl01-0000-0000-0000-000000000001', 's012-0000-0000-0000-000000000012', 1),
  ('ps02-0000-0000-0000-000000000002', 'pl01-0000-0000-0000-000000000001', 's016-0000-0000-0000-000000000016', 2),
  ('ps03-0000-0000-0000-000000000003', 'pl01-0000-0000-0000-000000000001', 's019-0000-0000-0000-000000000019', 3),
  ('ps04-0000-0000-0000-000000000004', 'pl01-0000-0000-0000-000000000001', 's020-0000-0000-0000-000000000020', 4),
  ('ps05-0000-0000-0000-000000000005', 'pl02-0000-0000-0000-000000000002', 's015-0000-0000-0000-000000000015', 1),
  ('ps06-0000-0000-0000-000000000006', 'pl02-0000-0000-0000-000000000002', 's014-0000-0000-0000-000000000014', 2),
  ('ps07-0000-0000-0000-000000000007', 'pl02-0000-0000-0000-000000000002', 's012-0000-0000-0000-000000000012', 3),
  ('ps08-0000-0000-0000-000000000008', 'pl03-0000-0000-0000-000000000003', 's001-0000-0000-0000-000000000001', 1),
  ('ps09-0000-0000-0000-000000000009', 'pl03-0000-0000-0000-000000000003', 's007-0000-0000-0000-000000000007', 2),
  ('ps10-0000-0000-0000-000000000010', 'pl03-0000-0000-0000-000000000003', 's008-0000-0000-0000-000000000008', 3),
  ('ps11-0000-0000-0000-000000000011', 'pl03-0000-0000-0000-000000000003', 's003-0000-0000-0000-000000000003', 4),
  ('ps12-0000-0000-0000-000000000012', 'pl03-0000-0000-0000-000000000003', 's011-0000-0000-0000-000000000011', 5);


-- ============================================
-- 확인용 조회 쿼리
-- ============================================

-- 점수 기반 TOP 10 추천 (AI 리믹스 우선)
SELECT title, artist, type, genre, era, score
FROM songs
ORDER BY score DESC
LIMIT 10;

-- 특정 유저의 저장 목록
SELECT u.nickname, s.title, s.artist, us.rating, us.play_count
FROM user_songs us
JOIN users u ON u.id = us.user_id
JOIN songs s ON s.id = us.song_id
WHERE us.user_id = 'u001-0000-0000-0000-000000000001'
  AND us.is_saved = 1
ORDER BY us.play_count DESC;

-- 기분별 추천 (mood 기반 라디오 세션 통계)
SELECT rs.mood, COUNT(*) AS session_count, AVG(s.score) AS avg_song_score
FROM radio_sessions rs
JOIN radio_recommendations rr ON rr.session_id = rs.id
JOIN songs s ON s.id = rr.song_id
GROUP BY rs.mood
ORDER BY session_count DESC;


CREATE USER IF NOT EXISTS 'SSAFY'@'localhost' IDENTIFIED BY 'SSAFY';
CREATE USER IF NOT EXISTS 'SSAFY'@'127.0.0.1' IDENTIFIED BY 'SSAFY';

GRANT ALL PRIVILEGES ON kpop_radio.* TO 'SSAFY'@'localhost';
GRANT ALL PRIVILEGES ON kpop_radio.* TO 'SSAFY'@'127.0.0.1';
FLUSH PRIVILEGES;

USE kpop_radio;
SHOW TABLES;
```

## 12. migration_add_radio_session_playlist_id.sql 전체 코드

실제 경로: `src/main/resources/sql/migration_add_radio_session_playlist_id.sql`

```sql
ALTER TABLE radio_sessions
  ADD COLUMN playlist_id CHAR(36) NULL;

ALTER TABLE radio_sessions
  ADD CONSTRAINT fk_radio_sessions_playlist
  FOREIGN KEY (playlist_id) REFERENCES playlists(id) ON DELETE SET NULL;
```

## 13. RadioController 관련 메서드 전체

관련 메서드의 누락을 피하기 위해 현재 `RadioController.java` 전체를 포함한다.

실제 경로: `src/main/java/com/ssafy/revibek/radio/controller/RadioController.java`

```java
package com.ssafy.revibek.radio.controller;

import com.ssafy.revibek.radio.dto.RadioCreateRequestDto;
import com.ssafy.revibek.radio.dto.RadioCreateResponseDto;
import com.ssafy.revibek.radio.dto.RadioResponseDto;
import com.ssafy.revibek.radio.service.RadioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/radio")
@RequiredArgsConstructor
public class RadioController {

    private final RadioService radioService;

    // 라디오 세션 생성
    @PostMapping
    public ResponseEntity<RadioCreateResponseDto> createRadio(Authentication authentication,
                                                              @RequestHeader(value = "X-USER-ID", required = false) String headerUserId,
                                                              @RequestParam(value = "userId", required = false) String requestUserId,
                                                              @Valid @RequestBody RadioCreateRequestDto dto) {
        return ResponseEntity.ok(radioService.createRadio(resolveUserId(authentication, headerUserId, requestUserId), dto));
    }

    // 세션 단건 조회
    @GetMapping("/{id}")
    public ResponseEntity<RadioResponseDto> getSession(Authentication authentication,
                                                       @RequestHeader(value = "X-USER-ID", required = false) String headerUserId,
                                                       @RequestParam(value = "userId", required = false) String requestUserId,
                                                       @PathVariable String id) {
        return ResponseEntity.ok(radioService.getSession(id, resolveUserId(authentication, headerUserId, requestUserId)));
    }

    // 내 세션 목록 조회
    @GetMapping("/me")
    public ResponseEntity<List<RadioResponseDto>> getSessionByUser(Authentication authentication,
                                                                   @RequestHeader(value = "X-USER-ID", required = false) String headerUserId,
                                                                   @RequestParam(value = "userId", required = false) String requestUserId) {
        return ResponseEntity.ok(radioService.getSessionByUser(resolveUserId(authentication, headerUserId, requestUserId)));
    }

    private String resolveUserId(Authentication authentication, String headerUserId, String requestUserId) {
        if (StringUtils.hasText(headerUserId)) {
            return headerUserId.trim();
        }
        if (StringUtils.hasText(requestUserId)) {
            return requestUserId.trim();
        }
        if (authentication != null && StringUtils.hasText(authentication.getName())) {
            return authentication.getName();
        }
        throw new IllegalArgumentException("사용자 ID가 필요합니다. Authorization 또는 X-USER-ID를 전달해주세요.");
    }
}
```

## 14. PlaylistService createPlaylist/addItem 메서드 전체

`createPlaylist`와 `addItem`의 의존 메서드 및 클래스 문맥까지 정확히 확인할 수 있도록 현재 `PlaylistService.java` 전체를 포함한다.

실제 경로: `src/main/java/com/ssafy/revibek/playlist/service/PlaylistService.java`

```java
package com.ssafy.revibek.playlist.service;

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

    @Transactional(readOnly = true)
    public List<PlaylistDto> getMyPlaylists(String userId) {
        validateUserId(userId);
        return playlistMapper.selectPlaylistsByUserId(userId);
    }

    @Transactional(readOnly = true)
    public PlaylistDto getPlaylist(String userId, String playlistId) {
        validateUserId(userId);
        validatePlaylistId(playlistId);

        PlaylistDto playlist = playlistMapper.selectPlaylistByIdAndUserId(playlistId, userId);
        if (playlist == null) {
            throw new IllegalArgumentException("존재하지 않는 플레이리스트이거나 접근 권한이 없습니다.");
        }
        playlist.setItems(playlistMapper.selectPlaylistItems(playlistId));
        return playlist;
    }

    @Transactional
    public PlaylistItemDto addItem(String userId, String playlistId, PlaylistItemDto request) {
        PlaylistDto playlist = getPlaylist(userId, playlistId);
        String songId = request.getSongId();
        if (!StringUtils.hasText(songId)) {
            throw new IllegalArgumentException("songId는 필수입니다.");
        }
        if (songDao.selectSongById(songId) == null) {
            throw new IllegalArgumentException("존재하지 않는 곡입니다.");
        }
        if (playlistMapper.countPlaylistItem(playlist.getId(), songId) > 0) {
            throw new IllegalArgumentException("이미 플레이리스트에 추가된 곡입니다.");
        }

        int orderNum = playlistMapper.selectNextOrderNum(playlist.getId());
        playlistMapper.insertPlaylistItem(playlist.getId(), songId, orderNum);

        return playlistMapper.selectPlaylistItems(playlist.getId()).stream()
            .filter(item -> songId.equals(item.getSongId()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("플레이리스트 곡 추가 결과를 찾을 수 없습니다."));
    }

    @Transactional
    public void deleteItem(String userId, String playlistId, String itemId) {
        getPlaylist(userId, playlistId);
        if (!StringUtils.hasText(itemId)) {
            throw new IllegalArgumentException("itemId는 필수입니다.");
        }

        int deletedRows = playlistMapper.deletePlaylistItem(playlistId, itemId);
        if (deletedRows == 0) {
            throw new IllegalArgumentException("존재하지 않는 플레이리스트 항목입니다.");
        }
    }

    @Transactional
    public void deletePlaylist(String userId, String playlistId) {
        validateUserId(userId);
        validatePlaylistId(playlistId);

        int deletedRows = playlistMapper.deletePlaylist(playlistId, userId);
        if (deletedRows == 0) {
            throw new IllegalArgumentException("존재하지 않는 플레이리스트이거나 접근 권한이 없습니다.");
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

## 15. PlaylistController 관련 메서드 전체

관련 메서드의 누락을 피하기 위해 현재 `PlaylistController.java` 전체를 포함한다.

실제 경로: `src/main/java/com/ssafy/revibek/playlist/controller/PlaylistController.java`

```java
package com.ssafy.revibek.playlist.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.revibek.playlist.dto.PlaylistDto;
import com.ssafy.revibek.playlist.dto.PlaylistItemDto;
import com.ssafy.revibek.playlist.service.PlaylistService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/playlists")
@RequiredArgsConstructor
public class PlaylistController {

    private final PlaylistService playlistService;

    @PostMapping
    public ResponseEntity<PlaylistDto> createPlaylist(Authentication authentication,
                                                      @Valid @RequestBody PlaylistDto request) {
        return ResponseEntity.ok(playlistService.createPlaylist(authentication.getName(), request));
    }

    @GetMapping
    public ResponseEntity<List<PlaylistDto>> getMyPlaylists(Authentication authentication) {
        return ResponseEntity.ok(playlistService.getMyPlaylists(authentication.getName()));
    }

    @GetMapping("/{playlistId}")
    public ResponseEntity<PlaylistDto> getPlaylist(Authentication authentication,
                                                   @PathVariable String playlistId) {
        return ResponseEntity.ok(playlistService.getPlaylist(authentication.getName(), playlistId));
    }

    @PostMapping("/{playlistId}/items")
    public ResponseEntity<PlaylistItemDto> addItem(Authentication authentication,
                                                   @PathVariable String playlistId,
                                                   @Valid @RequestBody PlaylistItemDto request) {
        return ResponseEntity.ok(playlistService.addItem(authentication.getName(), playlistId, request));
    }

    @DeleteMapping("/{playlistId}/items/{itemId}")
    public ResponseEntity<Map<String, String>> deleteItem(Authentication authentication,
                                                          @PathVariable String playlistId,
                                                          @PathVariable String itemId) {
        playlistService.deleteItem(authentication.getName(), playlistId, itemId);
        return ResponseEntity.ok(Map.of("message", "플레이리스트 항목 삭제 완료"));
    }

    @DeleteMapping("/{playlistId}")
    public ResponseEntity<Map<String, String>> deletePlaylist(Authentication authentication,
                                                              @PathVariable String playlistId) {
        playlistService.deletePlaylist(authentication.getName(), playlistId);
        return ResponseEntity.ok(Map.of("message", "플레이리스트 삭제 완료"));
    }
}
```

## 16. 메서드 시그니처 일치 여부 검증

1. **RadioService에서 createRadioPlaylist()가 실제로 존재하는가?**  
   예. `private String createRadioPlaylist(String userId, RadioCreateRequestDto request, List<RecommendedSongResponseDto> recommendedSongs)`로 존재하며 `createRadio()`에서 호출한다.
2. **PlaylistService.createPlaylist() 호출 방식이 실제 메서드 시그니처와 맞는가?**  
   예. 호출은 `playlistService.createPlaylist(userId, PlaylistDto)`이고 실제 시그니처는 `public PlaylistDto createPlaylist(String userId, PlaylistDto request)`이다. 반환값도 `PlaylistDto playlist`로 받는다.
3. **PlaylistService.addItem() 호출 방식이 실제 메서드 시그니처와 맞는가?**  
   예. 호출은 `playlistService.addItem(userId, playlist.getId(), PlaylistItemDto)`이고 실제 시그니처는 `public PlaylistItemDto addItem(String userId, String playlistId, PlaylistItemDto request)`이다. 반환값은 호출부에서 사용하지 않지만 Java에서 허용된다.
4. **RadioMapper.xml의 updateRadioSessionPlaylistId id가 RadioMapper.java 메서드명과 정확히 일치하는가?**  
   예. 양쪽 모두 `updateRadioSessionPlaylistId`이다. 파라미터 이름 `id`, `userId`, `playlistId`도 일치한다.
5. **RadioMapper.xml SELECT에 playlist_id가 포함되어 있는가?**  
   예. `selectRadioSessionByIdAndUserId`와 `selectRadioSessionByUserId`의 SELECT 목록 모두 `playlist_id`를 포함한다.
6. **RadioResponseDto에 playlistId 필드가 있는가?**  
   예. `private String playlistId;`가 존재한다.
7. **GlobalExceptionHandler에 RadioNotFoundException 처리 메서드가 있는가?**  
   예. `@ExceptionHandler(RadioNotFoundException.class)`가 붙은 `handleRadioNotFound(...)`가 HTTP 404를 반환한다.
8. **Maven compile 또는 test 실행 여부와 미실행/실패 이유**  
   이번 전체 코드 재출력 작업에서는 현재 파일을 그대로 읽는다는 지시와 불필요한 빌드 산출물 생성을 피하기 위해 Maven compile/test를 다시 실행하지 않았다. 이전 작업의 `answer13.md` 기록상 `.\mvnw.cmd clean test`는 Maven Wrapper의 `Cannot index into a null array`, `Cannot start maven from wrapper` 오류로 테스트 시작 전에 실패했다. 로컬 Wrapper 캐시의 Maven 실행 파일을 직접 사용한 재시도는 네트워크 권한 제한으로 Spring Boot 부모 POM을 내려받지 못해 `Non-resolvable parent POM`, `Permission denied: connect`로 실패했다.

추가 정적 검증으로 `git diff --check`를 실행했으며 오류는 없고, Git의 LF/CRLF 변환 경고만 확인됐다.

## 17. 아직 위험한 부분

- 기존 DB에는 `src/main/resources/sql/migration_add_radio_session_playlist_id.sql`을 한 번 적용해야 `radio_sessions.playlist_id` 조회 및 업데이트가 동작한다.
- 마이그레이션 SQL은 동일 컬럼/제약조건 존재 여부를 확인하지 않으므로 중복 실행 시 실패할 수 있다.
- `createRadioPlaylist()`는 추천 목록이 비어 있을 때만 생성을 건너뛴다. 목록은 있으나 모든 `songId`가 비어 있는 비정상 데이터라면 빈 플레이리스트가 생성될 수 있다.
- `updateRadioSessionPlaylistId` 반환형이 `void`이므로 실제 업데이트 행 수가 0이어도 서비스에서 감지하지 못한다.
- `PlaylistService.addItem()`은 중복 곡이면 예외를 던진다. 현재 추천 결과에 같은 곡 ID가 중복되면 `createRadio()` 전체 트랜잭션이 롤백될 수 있다.
- Maven compile/test가 이 작업에서 재실행되지 않았으므로 최종 컴파일 성공 여부는 아직 확인되지 않았다.

## 18. 다음에 내가 직접 확인할 체크리스트

- [ ] 대상 DB에 `migration_add_radio_session_playlist_id.sql`을 정확히 한 번 적용한다.
- [ ] `radio_sessions.playlist_id` 컬럼과 `fk_radio_sessions_playlist` 외래키가 생성됐는지 확인한다.
- [ ] 의존성 다운로드가 가능한 환경에서 `.\mvnw.cmd clean test` 또는 `mvn clean test`를 실행한다.
- [ ] `POST /api/radio` 응답의 `playlistId`가 실제 생성된 플레이리스트 ID와 같은지 확인한다.
- [ ] `GET /api/radio/{id}`와 `GET /api/radio/me`에서 `playlistId`가 반환되는지 확인한다.
- [ ] 존재하지 않거나 다른 사용자 소유의 라디오 세션 조회가 HTTP 404인지 확인한다.
- [ ] 추천곡 중복 및 빈 `songId` 데이터가 발생 가능한지 확인한다.
