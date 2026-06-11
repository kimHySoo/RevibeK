## 1. 현재 구조에서 가능한지 판단

1. `selectedSongs` 수신: 현재 불가능. DTO 필드 추가로 가능.
2. `RadioCreateRequestDto` 확장: 가능.
3. 라디오 입력값 저장: 대부분 저장 중이나 `title`, `playlist_id` 없음.
4. 곡 목록 저장: `radio_recommendations`에 저장 중이나 `source`, 곡 스냅샷 정보 없음.
5. 플레이리스트 저장: 기존 `PlaylistService`가 구현되어 있어 연동 가능.
6. 선택곡과 추천곡 병합: 구현 가능.
7. 기존 플레이리스트 API와 충돌 없이 연결: 가능. 단, `playlist_items`를 새로 만들지 말고 기존 `playlist_songs`를 사용해야 함.

관련 코드:

- [RadioCreateRequestDto.java](C:/Users/jaewo/OneDrive/Desktop/RevibeK2/RevibeK/src/main/java/com/ssafy/revibek/radio/dto/RadioCreateRequestDto.java:8)
- [RadioService.java](C:/Users/jaewo/OneDrive/Desktop/RevibeK2/RevibeK/src/main/java/com/ssafy/revibek/radio/service/RadioService.java:41)
- [PlaylistService.java](C:/Users/jaewo/OneDrive/Desktop/RevibeK2/RevibeK/src/main/java/com/ssafy/revibek/playlist/service/PlaylistService.java:25)
- [kpop_radio_schema.sql](C:/Users/jaewo/OneDrive/Desktop/RevibeK2/RevibeK/src/main/resources/sql/kpop_radio_schema.sql:118)

중요한 문제도 있습니다. 현재 `RadioController.resolveUserId()`는 인증 사용자보다 `X-USER-ID`를 우선하므로 다른 사용자 ID를 전달할 수 있습니다. 인증된 사용자 ID를 우선하도록 수정해야 합니다.

## 2. 구현 우선순위

1. DB 컬럼 확장
2. DTO 생성 및 수정
3. 선택곡을 `songs`에 upsert
4. 선택곡 우선 병합 및 중복 제거
5. 부족한 곡만 fallback 추천
6. 라디오 세션과 곡 목록 저장
7. `PlaylistService.createPlaylistFromRadio()` 연동
8. 인증 사용자 ID 우선 처리
9. API 통합 테스트

## 3. 수정/생성 파일 목록

생성:

- `RadioSelectedSongDto.java`
- `RadioSongResponseDto.java`

수정:

- `RadioCreateRequestDto.java`
- `RadioCreateResponseDto.java`
- `RadioController.java`
- `RadioService.java`
- `RadioMapper.java`
- `RadioMapper.xml`
- `SongDao.java`
- `SongMapper.xml`
- `PlaylistService.java`
- `PlaylistMapper.java`
- `PlaylistMapper.xml`
- `kpop_radio_schema.sql`

프로젝트에는 `SongMapper.java`가 없고 실제 인터페이스 이름은 `SongDao.java`입니다. 충돌을 피하려면 `SongDao`를 유지해야 합니다.

## 4. 운영 DB 수정안

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

`selectedSongs.songId`는 `radio_recommendations`와 `playlist_songs`의 외래키 대상이므로 반드시 `songs`에 존재해야 합니다. DB에 없는 선택곡을 허용하려면 먼저 `songs`에 upsert해야 합니다.

## 5. 최종 처리 규칙

- 목표 곡 수: `10`
- `selectedSongs`를 먼저 검증하고 DB에 upsert
- 선택곡을 최종 목록 앞부분에 배치
- `songId`, `youtubeUrl`, `title + artist` 기준 중복 제거
- 부족한 수만큼 fallback 추천
- `saveAsPlaylist == null`은 `true`
- `saveAsPlaylist == false`이면 `playlistId`는 `null`
- 플레이리스트 연결 테이블은 기존 `playlist_songs` 사용
- 전체 작업은 하나의 `@Transactional` 트랜잭션에서 실행

## 6. 권장 fallback 순서

```text
desiredMood + era + genre
desiredMood + era
desiredMood + genre
desiredMood
mood + era + genre
mood + era
mood + genre
mood
era + genre
era
genre
user preference
score fallback
```

현재 서비스는 일부 fallback을 지원하지만, 첫 번째 성공 결과만 반환합니다. 각 단계 결과를 누적하면서 이미 포함된 곡을 제외하도록 변경해야 합니다.

## 7. API 테스트 예시

```http
POST /api/radio
Authorization: Bearer {token}
Content-Type: application/json

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

저장 결과 확인:

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

## 8. 구현 후 확인 체크리스트

- 선택곡이 추천곡보다 먼저 배치되는지
- 최종 목록이 최대 10곡인지
- 세 가지 중복 기준이 모두 적용되는지
- 선택곡이 `songs`에 정상 등록되는지
- `source`가 `selected` 또는 `recommended`로 저장되는지
- `saveAsPlaylist=false`에서 플레이리스트가 생성되지 않는지
- 라디오와 플레이리스트 저장이 하나의 트랜잭션인지
- 인증 사용자 ID가 `X-USER-ID`보다 우선하는지

현재 세션은 파일 시스템이 `read-only`이고 승인 정책이 `never`라서 요청하신 전체 코드 파일 생성·수정과 Maven 검증을 수행할 수 없습니다. 따라서 실제 적용되지 않은 전체 코드를 적용 완료된 것처럼 제공하지 않았습니다. 쓰기 가능한 세션에서 위 파일들을 수정하고 컴파일 검증해야 합니다.
