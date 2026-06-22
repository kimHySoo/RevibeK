# RevibeK 백엔드 중심 수정 요청: 라디오-플레이리스트 연결 + 감정상태별 선호 입력값 연동 준비

이번 작업은 Vue/프론트엔드 코드를 수정하지 않습니다.

또한 이번 작업에서는 `Song search` 관련 수정은 하지 않습니다.

아래 항목은 이번 작업 범위에서 제외합니다.

```text id="2b7jgs"
제외 1. Song search를 SongDto 단건 → List<SongDto> 다건 반환으로 수정
제외 2. SongController 자체 try/catch 정리
```

작업 대상은 Spring Boot 백엔드 프로젝트입니다.

```text id="l2kgir"
RevibeK/RevibeK
```

## 1. 작업 목표

현재 RevibeK 백엔드에서 라디오 생성 후 자동 생성되는 플레이리스트 ID를 라디오 세션과 연결하고, 이후 라디오 조회 API에서도 `playlistId`를 받을 수 있도록 수정해주세요.

또한 추후 프론트에서 “유저가 감정상태별로 어떤 노래를 좋아하는지 입력받는 창”을 만들었을 때, 백엔드의 `RadioCreateRequestDto`와 `RadioService`가 해당 입력값을 받을 준비가 되어 있는지 점검하고 문서화해주세요.

이번 작업의 핵심 수정 항목은 아래입니다.

```text id="nuzaew"
1. radio_sessions에 playlist_id 컬럼 추가
2. RadioResponseDto에 playlistId 추가
3. RadioMapper.xml SELECT에 playlist_id 추가
4. RadioMapper.java에 playlistId 업데이트 메서드 추가
5. RadioService.createRadio()에서 playlistId를 radio_sessions에 저장
6. GET /api/radio/{id}, GET /api/radio/me 응답에 playlistId 포함
7. RadioService.getSession() 404 처리 개선 가능 여부 확인
8. RadioCreateRequestDto가 감정상태별 선호 입력값을 받을 수 있는지 확인
9. 감정상태별 선호 입력창에서 보낼 payload 예시 문서화
10. 수정한 파일의 전체 코드 또는 핵심 코드 전체를 결과 문서에 포함
```

프론트엔드 파일은 수정하지 마세요.

---

## 2. 현재 문제 요약

현재 `POST /api/radio` 응답에는 `playlistId`가 포함됩니다.

하지만 아래 조회 API에서는 `playlistId`가 없습니다.

```text id="es8xtr"
GET /api/radio/{id}
GET /api/radio/me
```

문제 흐름:

```text id="wt9gth"
POST /api/radio
→ 추천곡 기반 플레이리스트 자동 생성
→ playlistId 응답 반환
→ 생성 직후에는 프론트가 플레이리스트로 이동 가능

하지만 나중에 라디오 히스토리에서 다시 조회하면
GET /api/radio/{id}
→ playlistId 없음
→ 연결된 플레이리스트 상세보기 불가
```

따라서 라디오 세션과 자동 생성된 플레이리스트를 DB 레벨에서 연결해야 합니다.

---

## 3. radio_sessions playlist_id 컬럼 추가

`src/main/resources/sql/kpop_radio_schema.sql` 또는 실제 스키마 SQL 파일을 확인하고, `radio_sessions` 테이블에 `playlist_id` 컬럼을 추가해주세요.

권장 ALTER 문:

```sql id="k33sdy"
ALTER TABLE radio_sessions
  ADD COLUMN playlist_id VARCHAR(36) NULL;
```

외래키를 안전하게 추가할 수 있으면 아래도 추가해주세요.

```sql id="vymf7w"
ALTER TABLE radio_sessions
  ADD CONSTRAINT fk_radio_sessions_playlist
  FOREIGN KEY (playlist_id) REFERENCES playlists(id);
```

단, 기존 테스트 데이터나 스키마 순서 때문에 외래키 추가가 위험하다면 `playlist_id` 컬럼만 추가하고, 이유를 결과 문서에 적어주세요.

신규 생성용 DDL에도 `playlist_id`를 반영해주세요.

예시:

```sql id="uwiwqo"
CREATE TABLE radio_sessions (
  id VARCHAR(36) PRIMARY KEY,
  user_id VARCHAR(36) NOT NULL,
  mood VARCHAR(50),
  story TEXT,
  era VARCHAR(50),
  genre VARCHAR(50),
  situation VARCHAR(255),
  desired_mood VARCHAR(100),
  video_type VARCHAR(50),
  preferred_artist VARCHAR(100),
  excluded_keywords VARCHAR(255),
  recommendation_source VARCHAR(100),
  dj_ment TEXT,
  comfort_text TEXT,
  novel_excerpt TEXT,
  playlist_id VARCHAR(36) NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

기존 컬럼명, 타입, 순서는 실제 프로젝트 스키마에 맞춰 조정해주세요.

---

## 4. RadioResponseDto에 playlistId 추가

`RadioResponseDto.java`에 아래 필드를 추가해주세요.

```java id="rxr4jr"
private String playlistId;
```

Lombok을 사용 중이면 기존 스타일에 맞춰 필드만 추가하면 됩니다.

수정 후 아래 API 응답에 `playlistId`가 포함되어야 합니다.

```text id="ql0aff"
GET /api/radio/{id}
GET /api/radio/me
```

---

## 5. RadioMapper.xml SELECT에 playlist_id 추가

아래 조회 쿼리들을 찾아 `playlist_id`를 SELECT 목록에 추가해주세요.

```text id="7qnn07"
selectRadioSessionByIdAndUserId
selectRadioSessionByUserId
```

예시:

```xml id="4lny61"
SELECT
  id,
  mood,
  story,
  era,
  genre,
  situation,
  desired_mood,
  video_type,
  preferred_artist,
  excluded_keywords,
  recommendation_source,
  dj_ment,
  comfort_text,
  novel_excerpt,
  playlist_id,
  created_at
FROM radio_sessions
WHERE id = #{id}
  AND user_id = #{userId}
```

MyBatis 설정에 `map-underscore-to-camel-case=true`가 있으면 `playlist_id`는 `playlistId`로 자동 매핑됩니다.

---

## 6. RadioMapper.java에 playlistId 업데이트 메서드 추가

`RadioMapper.java`에 아래 메서드를 추가해주세요.

```java id="bw0u8h"
void updateRadioSessionPlaylistId(@Param("id") String id,
                                  @Param("userId") String userId,
                                  @Param("playlistId") String playlistId);
```

`RadioMapper.xml`에는 아래 update SQL을 추가해주세요.

```xml id="7rfbsm"
<update id="updateRadioSessionPlaylistId">
  UPDATE radio_sessions
  SET playlist_id = #{playlistId}
  WHERE id = #{id}
    AND user_id = #{userId}
</update>
```

---

## 7. RadioService.createRadio()에서 playlistId 저장

현재 라디오 생성 중 플레이리스트를 자동 생성하는 흐름이 있을 것입니다.

예상 코드:

```java id="gniimb"
String playlistId = createRadioPlaylist(userId, request, recommendedSongs);
```

이후 `playlistId`가 null이 아니면 `radio_sessions.playlist_id`에 저장해주세요.

예시:

```java id="jn69cg"
String playlistId = createRadioPlaylist(userId, request, recommendedSongs);

if (playlistId != null) {
    radioMapper.updateRadioSessionPlaylistId(sessionId, userId, playlistId);
}
```

주의:

```text id="dxznsk"
recommendedSongs가 비어 있으면 playlistId는 null일 수 있습니다.
이 경우는 오류가 아니라 DB_EMPTY 케이스이므로 업데이트하지 않아도 됩니다.
```

---

## 8. GET /api/radio/{id}, GET /api/radio/me 응답 확인

수정 후 아래 API 응답에 `playlistId`가 포함되어야 합니다.

```text id="d7hq5v"
GET /api/radio/{id}
GET /api/radio/me
```

예상 응답:

```json id="6masrw"
{
  "id": "radio-session-id",
  "playlistId": "playlist-id",
  "mood": "회상",
  "story": "오늘은 옛날 노래가 듣고 싶어요.",
  "era": "2세대",
  "genre": "댄스",
  "djMent": "오늘은 그 시절 감성을 다시 꺼내볼게요.",
  "songs": []
}
```

`GET /api/radio/me`가 배열을 반환한다면 각 요소에 `playlistId`가 포함되어야 합니다.

```json id="2a60so"
[
  {
    "id": "radio-session-id",
    "playlistId": "playlist-id",
    "mood": "회상",
    "era": "2세대",
    "createdAt": "2026-06-15T10:00:00"
  }
]
```

---

## 9. RadioService.getSession() 404 처리 개선

현재 `GET /api/radio/{id}`에서 존재하지 않는 세션이나 권한 없는 세션을 조회할 때 `RuntimeException`으로 500이 발생할 가능성이 있습니다.

가능하면 아래 방향으로 개선해주세요.

권장:

```text id="f7w15j"
존재하지 않는 라디오 세션 → 404
다른 사용자의 라디오 세션 접근 → 404 또는 403
```

현재 예외 구조가 커스텀 예외를 쓰지 않는다면 아래 중 하나로 처리해주세요.

방법 A:

```text id="8lnkvv"
ResourceNotFoundException 같은 커스텀 예외 추가
GlobalExceptionHandler에서 404로 매핑
```

방법 B:

```text id="3v4mbu"
RadioNotFoundException 추가
GlobalExceptionHandler에서 404로 매핑
```

방법 C:

```text id="m31swp"
큰 변경이 위험하면 이번 작업에서는 코드 수정하지 말고 개선안만 문서화
```

가능하면 실제 코드 수정까지 해주세요.

---

## 10. 감정상태별 선호 입력값 백엔드 연동 준비

프론트에서 “유저가 감정상태별로 어떤 노래를 좋아하는지 입력받는 창”을 만들 예정입니다.

이번 작업에서는 프론트 화면을 만들지 말고, 백엔드가 이 입력값을 받을 준비가 되어 있는지 확인해주세요.

### 10-1. RadioCreateRequestDto 확인

`RadioCreateRequestDto.java`에 아래 필드들이 있는지 확인해주세요.

```java id="xc1yyv"
private String mood;
private String situation;
private String desiredMood;
private String story;
private String era;
private String genre;
private String videoType;
private String preferredArtist;
private String excludedKeywords;
```

이 필드들이 이미 있다면 새 필드를 추가하지 않아도 됩니다.

없는 필드가 있다면 백엔드 라디오 생성 API에서 받을 수 있도록 추가해주세요.

단, 기존 API 호환성을 깨지 않도록 nullable 필드로 처리해주세요.

### 10-2. 감정상태별 선호 입력창이 보낼 payload 예시 문서화

프론트에서 추후 아래 형태로 요청을 보낼 수 있도록 문서화해주세요.

```json id="ng2f7m"
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

### 10-3. RadioService에서 필드 사용 여부 점검

`RadioService.createRadio()` 또는 내부 추천 메서드에서 아래 필드가 실제로 사용되는지 확인해주세요.

```text id="8xrv81"
mood
situation
desiredMood
story
era
genre
videoType
preferredArtist
excludedKeywords
```

확인 결과를 문서화해주세요.

아래 분류로 정리해주세요.

```text id="fn6nnm"
추천 필터에 직접 사용됨
AI DJ 멘트 프롬프트에 사용됨
DB 저장만 됨
현재 미사용
```

### 10-4. preferredArtist / excludedKeywords 반영 여부 확인

특히 아래 두 필드는 사용자가 감정상태별 선호 입력창에서 넣을 가능성이 큽니다.

```text id="aa3i61"
preferredArtist
excludedKeywords
```

이 값들이 추천곡 필터나 AI DJ 멘트에 반영되는지 확인해주세요.

반영되지 않는다면 이번 작업에서는 무리하게 추천 로직을 크게 바꾸지 말고, 개선안으로 문서화해주세요.

권장 개선안:

```text id="sq8h5g"
preferredArtist가 있으면 추천 후보에서 artist 우선순위를 높이거나, AI DJ 멘트 프롬프트에 반영
excludedKeywords가 있으면 title/artist/genre/youtubeUrl 기준 제외 필터로 사용
```

---

## 11. 전체 코드 요청

이번 작업 결과 문서에는 수정한 파일의 전체 코드 또는 핵심 코드 전체를 반드시 포함해주세요.

아래 파일은 가능하면 전체 코드를 보여주세요.

```text id="c9lf3p"
RadioResponseDto.java
RadioCreateRequestDto.java
RadioMapper.java
RadioMapper.xml
RadioService.java
kpop_radio_schema.sql
GlobalExceptionHandler.java
```

아래 파일은 확인만 했다면 핵심 메서드 전체를 보여주세요.

```text id="8gp8ld"
RadioController.java
PlaylistService.java
PlaylistController.java
AiDjPromptBuilder.java
AiDjMentService.java
```

정리 기준:

```text id="z6kd1w"
짧은 DTO/Mapper interface: 전체 코드
긴 Service/XML/SQL: 수정한 메서드 또는 수정된 테이블 DDL 전체
확인만 한 파일: 관련 메서드 전체
```

---

## 12. 테스트

수정 후 아래 명령을 실행해주세요.

Windows PowerShell 기준:

```powershell id="z9dzd9"
.\mvnw.cmd test
```

가능하면 전체 테스트:

```powershell id="7yxrfn"
.\mvnw.cmd clean test
```

테스트가 실패하면 실패 로그를 요약하고 원인을 적어주세요.

---

## 13. 결과 문서 작성

수정 결과를 아래 파일에 저장해주세요.

```text id="jqvtb8"
docs/answer/answer19_BE_radio_only.md
```

반드시 UTF-8 인코딩으로 저장해주세요.

## 14. answer19_BE_radio_only.md 형식

아래 형식으로 작성해주세요.

```markdown id="bcyydh"
# RevibeK 백엔드 수정 결과: 라디오 playlistId 저장/조회 및 감정 선호 입력 연동 준비

## 1. 전체 결론

## 2. 이번 작업에서 제외한 항목

## 3. 수정한 파일 목록

## 4. radio_sessions playlist_id 추가 내용

## 5. RadioResponseDto playlistId 추가 내용

## 6. RadioMapper 수정 내용

## 7. RadioService playlistId 저장 로직

## 8. GET /api/radio/{id} 응답 변경 예시

## 9. GET /api/radio/me 응답 변경 예시

## 10. RadioService.getSession 404 처리 개선 내용

## 11. 감정상태별 선호 입력값 DTO 확인

## 12. 감정 선호 입력 payload 예시

## 13. RadioService에서 감정 선호 필드 사용 여부

## 14. preferredArtist / excludedKeywords 반영 여부

## 15. 주요 파일 전체 코드 또는 핵심 코드

## 16. 테스트 결과

## 17. 아직 남은 백엔드 이슈

## 18. 프론트엔드에 전달할 변경 사항

## 19. 발표에서 말할 수 있는 표현
```

---

## 15. 프론트엔드 전달 사항 정리

백엔드 수정 후 프론트엔드 담당자에게 알려야 할 변경 사항을 정리해주세요.

예상 전달 사항:

```text id="hivp7l"
1. GET /api/radio/{id} 응답에 playlistId 추가됨
2. GET /api/radio/me 응답에 playlistId 추가됨
3. 감정상태별 선호 입력창은 RadioCreateRequestDto의 mood, situation, desiredMood, story, era, genre, videoType, preferredArtist, excludedKeywords로 보내면 됨
4. Song search API는 이번 작업에서 변경하지 않았음
```

---

## 16. 최종 지시

이번 작업에서는 Vue/프론트엔드 코드를 수정하지 마세요.

이번 작업에서는 Song search 관련 코드도 수정하지 마세요.

백엔드의 라디오-플레이리스트 연결, 라디오 조회 응답 playlistId 포함, 감정상태별 선호 입력값 DTO 확인만 처리하세요.

수정한 파일의 전체 코드 또는 핵심 코드 전체를 결과 문서에 포함하세요.

추측하지 말고 실제 파일명과 실제 코드 구조를 확인한 뒤 수정하세요.

수정 후 테스트를 실행하고, 결과를 `docs/answer/answer19_BE_radio_only.md`에 정리하세요.
