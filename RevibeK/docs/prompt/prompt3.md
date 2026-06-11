# Step 2. 라디오 API selectedSongs + 플레이리스트 저장 전체 코드 작성 요청

이전 분석 결과를 바탕으로 RevibeK 프로젝트의 라디오 API를 수정하려고 합니다.

현재 분석 결과는 아래와 같습니다.

## 현재 구조 판단

1. selectedSongs 수신은 현재 불가능하지만 RadioCreateRequestDto 필드 추가로 구현 가능
2. RadioCreateRequestDto 확장 가능
3. radio_sessions는 대부분 저장 중이나 title, playlist_id 컬럼이 없음
4. radio_recommendations는 저장 중이나 source, 곡 스냅샷 정보가 없음
5. 기존 PlaylistService가 구현되어 있어 라디오 결과를 플레이리스트로 저장하는 연동 가능
6. 선택곡과 추천곡 병합 구현 가능
7. 기존 플레이리스트 API와 충돌 없이 연결 가능
8. 단, playlist_items를 새로 만들지 말고 기존 playlist_songs 테이블을 사용해야 함
9. 프로젝트에는 SongMapper.java가 없고 실제 인터페이스 이름은 SongDao.java이므로 SongDao를 유지해야 함
10. RadioController.resolveUserId()는 현재 X-USER-ID를 인증 사용자보다 우선하므로 보안상 문제가 있음. 인증된 사용자 ID를 우선하도록 수정 필요

---

# 이번 작업 목표

이번에는 실제 프로젝트 구조에 맞춰 수정/생성해야 할 파일의 **전체 코드**를 작성해주세요.

단, 현재 세션이 read-only라면 파일을 직접 수정하지 말고, 아래 파일별 전체 코드를 답변으로 제공해주세요.

## 구현 목표

1. RadioCreateRequestDto에 selectedSongs, saveAsPlaylist, title, situation, desiredMood, videoType, excludedKeywords 추가
2. RadioSelectedSongDto 생성
3. RadioSongResponseDto 생성
4. RadioCreateResponseDto 수정
5. RadioController에서 인증 사용자 ID를 X-USER-ID보다 우선하도록 수정
6. RadioService에서 selectedSongs를 우선 반영
7. selectedSongs를 songs 테이블에 upsert
8. selectedSongs + 추천곡 병합
9. songId, youtubeUrl, title + artist 기준 중복 제거
10. 최종 목록 최대 10곡 유지
11. 부족한 곡만 fallback 추천으로 보충
12. radio_sessions에 title, playlist_id 저장
13. radio_recommendations에 title, artist, youtube_url, thumbnail_url, source, sort_order 저장
14. saveAsPlaylist == true 또는 null이면 기존 playlist_songs를 사용해 플레이리스트 저장
15. saveAsPlaylist == false이면 플레이리스트 저장 없이 radio_session만 생성
16. 전체 라디오 생성 작업은 @Transactional로 처리

---

# 반드시 지켜야 할 기존 구조

## 실제 파일명

프로젝트에는 SongMapper.java가 없습니다.

따라서 아래 이름을 사용하지 마세요.

```text
SongMapper.java
```

대신 실제 파일명인 아래를 사용하세요.

```text
SongDao.java
SongMapper.xml
```

## 플레이리스트 연결 테이블

새로 아래 테이블을 만들지 마세요.

```text
playlist_items
```

기존 테이블인 아래를 사용하세요.

```text
playlist_songs
```

## selectedSongs.songId 처리

selectedSongs.songId는 radio_recommendations와 playlist_songs의 외래키 대상입니다.

따라서 selectedSongs에 들어온 곡은 반드시 songs 테이블에 존재해야 합니다.

DB에 없는 선택곡도 허용하기 위해 아래 로직을 구현해주세요.

```text
selectedSongs 검증
→ songId가 있으면 songs에서 조회
→ 없으면 selectedSongs 정보로 songs upsert 또는 insert
→ 최종적으로 songs.id를 확보
→ radio_recommendations와 playlist_songs에 저장
```

---

# DB 수정안

아래 ALTER TABLE 기준으로 schema 수정 코드 또는 migration SQL을 작성해주세요.

```sql
ALTER TABLE radio_sessions
  ADD COLUMN title VARCHAR(150) NULL AFTER user_id,
  ADD COLUMN playlist_id CHAR(36) NULL AFTER video_type,
  ADD INDEX idx_radio_playlist_id (playlist_id),
  ADD CONSTRAINT fk_radio_session_playlist
    FOREIGN KEY (playlist_id) REFERENCES playlists(id)
    ON DELETE SET NULL;

ALTER TABLE radio_recommendations
  ADD COLUMN title VARCHAR(200) NULL AFTER song_id,
  ADD COLUMN artist VARCHAR(100) NULL AFTER title,
  ADD COLUMN youtube_url VARCHAR(300) NULL AFTER artist,
  ADD COLUMN thumbnail_url VARCHAR(500) NULL AFTER youtube_url,
  ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'recommended'
    AFTER thumbnail_url,
  ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE playlists
  ADD COLUMN description VARCHAR(500) NULL AFTER name,
  ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP;
```

단, 기존 schema 파일 `kpop_radio_schema.sql`이 있다면 전체 수정본도 함께 제공해주세요.

---

# fallback 추천 순서

추천곡이 부족할 때 아래 순서로 누적 조회해주세요.

중요: 첫 번째 성공 결과만 반환하지 말고, 부족한 수만큼 각 단계 결과를 누적하면서 이미 포함된 곡은 제외해야 합니다.

```text
1. desiredMood + era + genre
2. desiredMood + era
3. desiredMood + genre
4. desiredMood
5. mood + era + genre
6. mood + era
7. mood + genre
8. mood
9. era + genre
10. era
11. genre
12. user preference
13. score fallback
```

---

# 수정/생성해야 할 파일

아래 파일들의 전체 코드를 작성해주세요.

## 생성

```java
// src/main/java/com/ssafy/revibek/radio/dto/RadioSelectedSongDto.java
전체 코드
```

```java
// src/main/java/com/ssafy/revibek/radio/dto/RadioSongResponseDto.java
전체 코드
```

## 수정

```java
// src/main/java/com/ssafy/revibek/radio/dto/RadioCreateRequestDto.java
전체 코드
```

```java
// src/main/java/com/ssafy/revibek/radio/dto/RadioCreateResponseDto.java
전체 코드
```

```java
// src/main/java/com/ssafy/revibek/radio/controller/RadioController.java
전체 코드
```

```java
// src/main/java/com/ssafy/revibek/radio/service/RadioService.java
전체 코드
```

```java
// src/main/java/com/ssafy/revibek/radio/mapper/RadioMapper.java
전체 코드
```

```xml
<!-- src/main/resources/mapper/radio/RadioMapper.xml -->
전체 코드
```

```java
// src/main/java/com/ssafy/revibek/song/dao/SongDao.java
전체 코드
```

```xml
<!-- src/main/resources/mapper/song/SongMapper.xml -->
전체 코드
```

```java
// src/main/java/com/ssafy/revibek/playlist/service/PlaylistService.java
전체 코드
```

```java
// src/main/java/com/ssafy/revibek/playlist/mapper/PlaylistMapper.java
전체 코드
```

```xml
<!-- src/main/resources/mapper/playlist/PlaylistMapper.xml -->
전체 코드
```

```sql
-- src/main/resources/sql/kpop_radio_schema.sql
전체 코드 또는 ALTER TABLE migration SQL
```

---

# RadioCreateRequestDto 필수 필드

```java
private String title;
private String mood;
private String situation;
private String desiredMood;
private String story;
private String era;
private String genre;
private String videoType;
private String excludedKeywords;
private Boolean saveAsPlaylist;
private List<RadioSelectedSongDto> selectedSongs;
```

---

# RadioSelectedSongDto 필수 필드

```java
private String songId;
private String title;
private String artist;
private String youtubeUrl;
private String thumbnailUrl;
private String generation;
private String genre;
private String mood;
```

---

# RadioSongResponseDto 필수 필드

```java
private String songId;
private String title;
private String artist;
private String youtubeUrl;
private String thumbnailUrl;
private String source;
private Integer sortOrder;
```

---

# RadioCreateResponseDto 필수 필드

```java
private String radioSessionId;
private String playlistId;
private String title;
private String story;
private String djComment;
private List<RadioSongResponseDto> songs;
```

---

# RadioService 처리 흐름

RadioService는 아래 흐름으로 구현해주세요.

```text
1. userId 확인
2. RadioCreateRequestDto 수신
3. saveAsPlaylist null이면 true 처리
4. selectedSongs 검증
5. selectedSongs를 songs에 upsert해서 songId 확보
6. selectedSongs를 RadioSongResponseDto로 변환하고 source = selected 설정
7. 부족한 곡 수 계산
8. fallback 순서대로 추천곡 조회
9. 이미 포함된 곡 제외
10. selectedSongs + recommendedSongs 병합
11. songId, youtubeUrl, title + artist 기준 중복 제거
12. sortOrder 부여
13. DJ 멘트 생성
14. radio_sessions insert
15. radio_recommendations batch insert
16. saveAsPlaylist가 true이면 PlaylistService.createPlaylistFromRadio() 호출
17. playlist_songs에 곡 저장
18. radio_sessions에 playlist_id 업데이트
19. 최종 응답 반환
```

---

# RadioController userId 처리 규칙

현재 RadioController.resolveUserId()는 X-USER-ID를 인증 사용자보다 우선한다고 분석되었습니다.

아래 순서로 수정해주세요.

```text
1. 인증된 사용자 ID가 있으면 인증 사용자 ID 사용
2. 인증 정보가 없을 때만 X-USER-ID 사용
3. 둘 다 없으면 401 또는 적절한 에러 응답
```

---

# PlaylistService 추가 메서드

기존 PlaylistService를 재사용하되, 라디오 저장용 메서드가 없다면 아래 메서드를 추가해주세요.

```java
createPlaylistFromRadio(String userId, String title, String description, List<RadioSongResponseDto> songs)
```

반환값은 생성된 playlistId로 해주세요.

반드시 기존 playlist_songs 테이블을 사용해주세요.

---

# API 테스트 예시

아래 테스트 예시를 작성해주세요.

## 1. selectedSongs 포함 라디오 생성

```http
POST /api/radio
Authorization: Bearer {token}
Content-Type: application/json
```

```json
{
  "title": "오늘 나를 위로하는 2세대 라디오",
  "mood": "지친",
  "desiredMood": "위로",
  "story": "다시 힘내고 싶어요.",
  "era": "2세대",
  "genre": "댄스",
  "saveAsPlaylist": true,
  "selectedSongs": [
    {
      "songId": "song-uuid-1",
      "title": "다시 만난 세계",
      "artist": "소녀시대",
      "youtubeUrl": "https://www.youtube.com/watch?v=example"
    }
  ]
}
```

## 2. 저장 결과 확인

```http
GET /api/playlists
Authorization: Bearer {token}
```

```http
GET /api/radio/me
Authorization: Bearer {token}
```

```http
GET /api/radio/{radioSessionId}
Authorization: Bearer {token}
```

---

# 최종 답변 순서

반드시 아래 순서로 답변해주세요.

1. 구현 가능 여부 최종 판단
2. 수정/생성 파일 목록
3. DB migration SQL
4. RadioSelectedSongDto 전체 코드
5. RadioSongResponseDto 전체 코드
6. RadioCreateRequestDto 전체 코드
7. RadioCreateResponseDto 전체 코드
8. RadioController 전체 코드
9. RadioService 전체 코드
10. RadioMapper 전체 코드
11. RadioMapper.xml 전체 코드
12. SongDao 전체 코드
13. SongMapper.xml 전체 코드
14. PlaylistService 전체 코드
15. PlaylistMapper 전체 코드
16. PlaylistMapper.xml 전체 코드
17. API 테스트 예시
18. 구현 후 체크리스트

---

# 최종 답변 규칙

* 코드 일부만 주지 마세요.
* 수정 또는 생성되는 파일은 반드시 전체 코드로 주세요.
* `...`, `생략`, `기존 코드 유지`, `나머지는 동일` 같은 표현을 쓰지 마세요.
* 실제 존재하는 파일명과 패키지명을 기준으로 작성해주세요.
* SongMapper.java를 만들지 말고 기존 SongDao.java를 수정해주세요.
* playlist_items를 만들지 말고 기존 playlist_songs를 사용해주세요.
* selectedSongs는 추천곡보다 우선순위가 높아야 합니다.
* saveAsPlaylist가 true 또는 null이면 라디오 결과가 사용자 플레이리스트로 저장되어야 합니다.
* saveAsPlaylist가 false이면 플레이리스트 저장 없이 radio_session만 생성되어야 합니다.
* 중복 곡은 반드시 제거해주세요.
* RadioController는 인증 사용자 ID를 X-USER-ID보다 우선해야 합니다.
* 프론트엔드에서 바로 사용할 수 있는 요청/응답 JSON 예시를 포함해주세요.
