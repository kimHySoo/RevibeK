# RevibeK 백엔드 수정 결과: 라디오 playlistId 저장/조회 및 감정 선호 입력 연동 준비

## 1. 전체 결론

- 라디오 생성 시 추천곡이 있으면 비공개 플레이리스트를 자동 생성하고 추천곡을 추가한다.
- 생성된 `playlistId`를 `radio_sessions.playlist_id`에 저장하고 `POST /api/radio` 응답에도 반환한다.
- `GET /api/radio/{id}`, `GET /api/radio/me` 조회 결과에 `playlistId`가 포함된다.
- 라디오 단건 조회 실패는 `RadioNotFoundException`을 통해 HTTP 404로 응답한다.
- `RadioCreateRequestDto`에는 감정 선호 입력창에 필요한 9개 필드가 이미 모두 존재한다.
- 프론트엔드와 Song search 관련 코드는 수정하지 않았다.

## 2. 이번 작업에서 제외한 항목

- Vue/프론트엔드 코드 수정
- Song search 단건 반환을 다건 반환으로 변경
- `SongController` try/catch 정리
- 추천 알고리즘의 대규모 변경

## 3. 수정한 파일 목록

| 파일 | 변경 내용 |
|---|---|
| `RadioResponseDto.java` | 조회 응답용 `playlistId` 추가 |
| `RadioCreateResponseDto.java` | 생성 응답용 `playlistId` 추가 |
| `RadioMapper.java` | `updateRadioSessionPlaylistId` 추가 |
| `RadioMapper.xml` | playlist ID update 및 조회 SELECT 추가 |
| `RadioService.java` | 플레이리스트 자동 생성, 세션 연결, 404 예외 사용 |
| `RadioNotFoundException.java` | 라디오 조회 실패 전용 예외 추가 |
| `GlobalExceptionHandler.java` | 라디오 조회 실패를 404로 매핑 |
| `kpop_radio_schema.sql` | 신규 생성용 `playlist_id` 컬럼과 FK 추가 |
| `migration_add_radio_session_playlist_id.sql` | 기존 DB 반영용 ALTER SQL 추가 |

`RadioCreateRequestDto.java`, `RadioController.java`, `PlaylistService.java`, `PlaylistController.java`,
`AiDjPromptBuilder.java`, `AiDjMentService.java`, `SongMapper.xml`은 동작 확인만 했으며 수정하지 않았다.

## 4. radio_sessions playlist_id 추가 내용

신규 DB 생성용 DDL에 nullable `playlist_id CHAR(36)`를 추가했다. `radio_sessions`가 `playlists`보다 먼저 생성되므로 외래키는 `playlists`와 `playlist_songs` 생성 이후 추가한다.

플레이리스트 삭제 시 과거 라디오 세션까지 삭제되거나 삭제가 막히지 않도록 `ON DELETE SET NULL`을 사용했다.

```sql
CREATE TABLE radio_sessions (
  id              CHAR(36)    NOT NULL DEFAULT (UUID()),
  user_id         CHAR(36)    NOT NULL,
  mood            VARCHAR(50) NOT NULL,
  story           TEXT        NULL,
  era             VARCHAR(20) NULL,
  genre           VARCHAR(50) NULL,
  situation       VARCHAR(200) NULL,
  desired_mood    VARCHAR(50) NULL,
  video_type      VARCHAR(50) NULL,
  preferred_artist VARCHAR(100) NULL,
  excluded_keywords VARCHAR(500) NULL,
  recommendation_source VARCHAR(50) NULL,
  dj_ment         TEXT        NULL,
  comfort_text    TEXT        NULL,
  novel_excerpt   TEXT        NULL,
  playlist_id     CHAR(36)    NULL,
  created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_user_session (user_id, created_at DESC),
  INDEX idx_mood (mood),
  INDEX idx_radio_era_genre (era, genre),
  INDEX idx_radio_desired_mood (desired_mood)
) ENGINE=InnoDB COMMENT='AI 라디오 세션 이력';

ALTER TABLE radio_sessions
  ADD CONSTRAINT fk_radio_sessions_playlist
  FOREIGN KEY (playlist_id) REFERENCES playlists(id) ON DELETE SET NULL;
```

기존 DB에는 아래 파일을 1회 적용해야 한다.

```sql
ALTER TABLE radio_sessions
  ADD COLUMN playlist_id CHAR(36) NULL;

ALTER TABLE radio_sessions
  ADD CONSTRAINT fk_radio_sessions_playlist
  FOREIGN KEY (playlist_id) REFERENCES playlists(id) ON DELETE SET NULL;
```

## 5. RadioResponseDto playlistId 추가 내용

MyBatis의 `map-underscore-to-camel-case=true` 설정에 의해 `playlist_id`가 `playlistId`로 자동 매핑된다.

```java
private String id;
private String playlistId;
private String mood;
```

생성 응답도 실제 생성된 ID를 반환할 수 있도록 `RadioCreateResponseDto`에 동일 필드를 추가했다.

## 6. RadioMapper 수정 내용

```java
void updateRadioSessionPlaylistId(@Param("id") String id,
                                  @Param("userId") String userId,
                                  @Param("playlistId") String playlistId);
```

```xml
<update id="updateRadioSessionPlaylistId">
    UPDATE radio_sessions
    SET playlist_id = #{playlistId}
    WHERE id = #{id}
    AND user_id = #{userId}
</update>

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
```

## 7. RadioService playlistId 저장 로직

현재 코드에는 기존 요청 설명과 달리 플레이리스트 자동 생성 호출 자체가 없었다. 따라서 기존 `PlaylistService`를 재사용해 추천곡이 있을 때만 비공개 플레이리스트를 만들고 추천곡을 추가했다.

```java
String playlistId = createRadioPlaylist(userId, request, recommendedSongs);
if (playlistId != null) {
    radioMapper.updateRadioSessionPlaylistId(sessionId, userId, playlistId);
}
```

`DB_EMPTY`로 추천곡 목록이 비어 있으면 `createRadioPlaylist`가 `null`을 반환하므로 플레이리스트 생성과 세션 update를 수행하지 않는다.

## 8. GET /api/radio/{id} 응답 변경 예시

```json
{
  "id": "radio-session-id",
  "playlistId": "playlist-id",
  "mood": "회상",
  "story": "오늘은 옛날 노래가 듣고 싶어요.",
  "era": "2세대",
  "genre": "댄스",
  "songs": []
}
```

연결된 플레이리스트가 없으면 `playlistId`는 `null`이다.

## 9. GET /api/radio/me 응답 변경 예시

```json
[
  {
    "id": "radio-session-id",
    "playlistId": "playlist-id",
    "mood": "회상",
    "era": "2세대",
    "createdAt": "2026-06-15T10:00:00",
    "songs": []
  }
]
```

## 10. RadioService.getSession 404 처리 개선 내용

기존에는 조회 결과가 없으면 일반 `RuntimeException`이 발생해 `GlobalExceptionHandler`에서 500으로 처리됐다.

```java
public RadioResponseDto getSession(String id, String userId) {
    RadioResponseDto session = radioMapper.selectRadioSessionByIdAndUserId(id, userId);
    if (session == null) {
        throw new RadioNotFoundException("존재하지 않는 라디오 세션이거나 접근 권한이 없습니다.");
    }
    List<RadioResponseDto.RadioSongDto> songs =
        radioMapper.selectRecommendationBySessionId(id);
    session.setSongs(songs);
    return session;
}
```

`id`와 `userId`를 함께 조회하므로 존재하지 않는 세션과 다른 사용자의 세션 접근 모두 정보 노출을 줄이기 위해 404로 응답한다.

## 11. 감정상태별 선호 입력값 DTO 확인

`RadioCreateRequestDto`에 아래 필드가 이미 모두 존재하고 별도 validation annotation이 없어 nullable 입력이 가능하다. 기존 API 호환성을 깨는 DTO 변경은 하지 않았다.

```java
private String mood;
private String story;
private String era;
private String genre;
private String situation;
private String desiredMood;
private String videoType;
private String preferredArtist;
private String excludedKeywords;
```

## 12. 감정 선호 입력 payload 예시

```json
{
  "mood": "지침",
  "situation": "프로젝트 마감 전 새벽",
  "desiredMood": "위로받고 싶음",
  "story": "오늘은 프로젝트 때문에 지쳤는데 예전 K-POP을 들으면서 힘을 얻고 싶어요.",
  "era": "2세대",
  "genre": "댄스",
  "videoType": "AI cover",
  "preferredArtist": "BIGBANG",
  "excludedKeywords": "발라드, 너무 잔잔한 곡"
}
```

## 13. RadioService에서 감정 선호 필드 사용 여부

모든 필드는 `radio_sessions`에 저장되고 생성 응답에도 포함된다.

| 필드 | 추천 필터에 직접 사용 | AI DJ 멘트 프롬프트 | 기타 실제 사용 |
|---|---|---|---|
| `mood` | 사용. `desiredMood`가 없을 때 추천 mood로 사용 | 사용 | 플레이리스트 이름/mood tag, 추천 이유 |
| `situation` | 미사용 | 미사용 | 추천 이유 문구에 사용 |
| `desiredMood` | 사용. 입력 시 `mood`보다 우선 | 미사용 | 플레이리스트 mood tag |
| `story` | 미사용 | 사용 | DB 저장 |
| `era` | 사용 | 사용 | 추천 이유 |
| `genre` | 사용 | 사용 | 추천 이유 |
| `videoType` | 미사용 | 미사용 | 추천 이유 문구에 사용 |
| `preferredArtist` | 현재 요청값은 미사용 | 미사용 | DB 저장만 됨 |
| `excludedKeywords` | 사용 | 미사용 | 제목/아티스트 제외 조건 |

## 14. preferredArtist / excludedKeywords 반영 여부

### preferredArtist

- 요청 DTO 값은 정규화되고 DB에 저장되지만 현재 추천 후보 우선순위나 AI DJ 프롬프트에는 직접 사용되지 않는다.
- 유저의 별도 장기 선호 설정에 있는 `preferredArtists`는 앞선 mood/era/genre 필터가 모두 실패한 `DB_USER_PREFERENCE_FALLBACK` 단계에서만 사용된다.
- 개선안: 요청의 `preferredArtist`를 추천 SQL의 artist 우선순위에 반영하거나 AI DJ 프롬프트에 전달한다.

### excludedKeywords

- 요청값과 유저 장기 선호의 제외 키워드를 합친 뒤 모든 주요 추천 SQL에 전달한다.
- 현재 SQL은 `LOWER(CONCAT(title, ' ', artist)) NOT REGEXP ...` 조건이므로 제목과 아티스트만 제외한다.
- genre와 youtube URL은 현재 제외 기준에 포함되지 않는다.
- 개선안: 요구가 확정되면 genre를 제외 대상에 추가하고 URL 기준 제외가 실제로 필요한지 별도 검토한다.

## 15. 주요 파일 전체 코드 또는 핵심 코드

### RadioResponseDto.java 전체

```java
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
    private String novelExcerpt;
    private LocalDateTime createdAt;
    private List<RadioSongDto> songs;

    @Data
    @NoArgsConstructor
    public static class RadioSongDto {
        private String songId;
        private String title;
        private String artist;
        private int orderNum;
        private String reason;
    }
}
```

### RadioCreateRequestDto.java 전체

```java
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

### RadioMapper.java 전체 메서드

```java
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
                              @Param("songId") String songId,
                              @Param("orderNum") int orderNum,
                              @Param("reason") String reason);
    List<RadioResponseDto.RadioSongDto> selectRecommendationBySessionId(
        @Param("sessionId") String sessionId
    );
}
```

### RadioService.java 수정 핵심 메서드

```java
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
    List<RecommendedSongResponseDto> recommendedSongs =
        toRecommendedSongs(recommendationResult.songs(), request);

    String djMent = aiDjMentService.createDjMent(request, recommendedSongs);
    String sessionId = UUID.randomUUID().toString();
    radioMapper.insertRadioSessionWithMent(
        sessionId, userId, request.getMood(), request.getStory(),
        request.getEra(), request.getGenre(), request.getSituation(),
        request.getDesiredMood(), request.getVideoType(),
        request.getPreferredArtist(), request.getExcludedKeywords(),
        recommendationResult.source(), djMent
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
```

### GlobalExceptionHandler.java 및 예외 핵심 코드

```java
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

public class RadioNotFoundException extends RuntimeException {
    public RadioNotFoundException(String message) {
        super(message);
    }
}
```

### 확인한 관련 코드

`RadioController`의 조회 메서드는 `RadioResponseDto`를 그대로 반환하므로 DTO와 Mapper SELECT 변경만으로 `playlistId`가 응답에 포함된다.

```java
@GetMapping("/{id}")
public ResponseEntity<RadioResponseDto> getSession(...) {
    return ResponseEntity.ok(radioService.getSession(id, resolveUserId(...)));
}

@GetMapping("/me")
public ResponseEntity<List<RadioResponseDto>> getSessionByUser(...) {
    return ResponseEntity.ok(radioService.getSessionByUser(resolveUserId(...)));
}
```

`PlaylistService`의 기존 `createPlaylist`와 `addItem`을 라디오 생성 로직에서 재사용했다. `PlaylistController`는 변경하지 않았다.

`AiDjPromptBuilder.build(RadioCreateRequestDto, ...)`는 현재 `mood`, `story`, `era`, `genre`, 추천곡 목록만 프롬프트에 포함한다. `AiDjMentService`의 fallback 멘트도 `mood`, `era`, `genre`, 첫 추천곡만 사용한다.

## 16. 테스트 결과

실행 시도:

```powershell
.\mvnw.cmd clean test
```

결과: 테스트 시작 전 Maven Wrapper 자체 오류로 실패했다.

```text
Cannot index into a null array.
Cannot start maven from wrapper
```

로컬 Maven Wrapper 캐시에 있는 Maven 실행 파일을 직접 사용해 `clean test`를 다시 시도했으나, 네트워크 권한 제한으로 부모 POM을 내려받지 못했다.

```text
Non-resolvable parent POM
Could not transfer artifact org.springframework.boot:spring-boot-starter-parent:pom:4.0.6
Permission denied: connect
```

대체 정적 검증 결과:

- `git diff --check`: 통과
- `RadioMapper.xml`, `PlaylistMapper.xml`, `SongMapper.xml` XML 파싱: 통과
- MyBatis camel-case 설정 확인: `mybatis.configuration.map-underscore-to-camel-case=true`

따라서 테스트 미실행 원인은 코드 테스트 실패가 아니라 Wrapper 스크립트와 제한된 Maven 의존성 다운로드 환경이다.

## 17. 아직 남은 백엔드 이슈

- 기존 운영/개발 DB에는 `migration_add_radio_session_playlist_id.sql`을 1회 적용해야 한다.
- `preferredArtist` 요청값은 현재 추천 우선순위와 AI DJ 멘트에 직접 반영되지 않는다.
- `excludedKeywords`는 현재 title/artist만 검사하며 genre/youtube URL은 검사하지 않는다.
- Maven Wrapper의 Windows PowerShell 처리 오류를 별도 정리해야 정상적인 `.\mvnw.cmd test` 실행이 가능하다.
- 의존성 다운로드 가능한 환경에서 `clean test`와 실제 DB 통합 테스트가 필요하다.

## 18. 프론트엔드에 전달할 변경 사항

1. `POST /api/radio` 응답에 실제 자동 생성 플레이리스트의 `playlistId`가 포함된다. 추천곡이 없으면 `null`이다.
2. `GET /api/radio/{id}` 응답에 `playlistId`가 추가됐다.
3. `GET /api/radio/me` 배열의 각 요소에 `playlistId`가 추가됐다.
4. 감정 선호 입력창은 `mood`, `situation`, `desiredMood`, `story`, `era`, `genre`, `videoType`, `preferredArtist`, `excludedKeywords`를 `POST /api/radio` JSON body로 보내면 된다.
5. 존재하지 않거나 접근 권한이 없는 라디오 단건 조회는 이제 500이 아니라 404를 반환한다.
6. Song search API는 이번 작업에서 변경하지 않았다.

## 19. 발표에서 말할 수 있는 표현

“라디오 생성 결과와 자동 생성 플레이리스트를 `radio_sessions.playlist_id`로 연결해, 생성 직후뿐 아니라 히스토리 조회에서도 같은 플레이리스트로 이동할 수 있게 했습니다. 조회 실패는 404로 명확히 처리했고, 감정상태별 선호 입력에 필요한 DTO 필드와 현재 추천·AI 멘트 반영 범위도 실제 코드 기준으로 점검했습니다.”
