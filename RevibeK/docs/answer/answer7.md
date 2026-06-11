## 1. 최종 판단

**DB 수정 필요**

전체 프로젝트를 정적 분석한 결과, 대부분의 테이블·컬럼·타입·Mapper 계약은 일치합니다.

다만 최종 스키마에서 `radio_recommendations.order_num`을 `sort_order`로 변경하지만, 현재 `RadioMapper.xml`은 계속 `order_num`을 사용합니다.

- 스키마 변경: [kpop_radio_schema.sql:436](C:/Users/jaewo/OneDrive/Desktop/RevibeK2/RevibeK/src/main/resources/sql/kpop_radio_schema.sql:436)
- 코드 사용: [RadioMapper.xml:103](C:/Users/jaewo/OneDrive/Desktop/RevibeK2/RevibeK/src/main/resources/mapper/radio/RadioMapper.xml:103)

따라서 라디오 추천곡 저장·조회 API 실행 시 `Unknown column 'order_num'` 오류가 발생할 수 있습니다.

또한 실제 실행 중인 로컬 DB에는 접근하지 못했으므로, 판단은 현재 저장소의 최종 SQL 스키마 기준입니다.

---

## 2. 코드가 사용하는 테이블 목록

| 테이블명 | 사용 위치 | 스키마 존재 여부 | 상태 |
|---|---|---|---|
| `users` | UserMapper | 존재 | 정상 |
| `songs` | SongDao, LikeMapper, PlaylistMapper, RadioMapper, UserSongMapper | 존재 | 정상 |
| `user_songs` | UserSongMapper | 존재 | 정상 |
| `user_preferences` | PreferenceMapper | 존재 | 정상 |
| `radio_sessions` | RadioMapper | 존재 | 정상 |
| `radio_recommendations` | RadioMapper | 존재 | `order_num` 불일치 |
| `playlists` | PlaylistMapper | 존재 | 정상 |
| `playlist_songs` | PlaylistMapper | 존재 | 정상 |
| `youtube_channels` | YoutubeMapper | 존재 | 정상 |
| `youtube_videos_raw` | YoutubeMapper, RawVideoMapper | 존재 | 정상 |
| `song_likes` | LikeMapper | 존재 | 정상 |
| `score_logs` | 코드에서 사용하지 않음 | 존재 | 스키마 전용 |

코드에서 사용하지만 스키마에 없는 테이블은 없습니다.

---

## 3. 테이블별 컬럼 매칭 결과

### `users`

코드에서 사용하는 컬럼:
- `id`, `nickname`, `email`, `provider`, `provider_id`, `password_hash`

스키마에 존재하는 컬럼:
- 코드 사용 컬럼 전체와 `created_at`, `updated_at`

판단:
- 문제 없음
- Java `String` ID와 DB `CHAR(36)` 호환

### `songs`

코드에서 사용하는 컬럼:
- `id`, `title`, `artist`, `genre`, `era`, `generation`, `mood`, `type`
- `youtube_url`, `youtube_id`, `thumbnail_url`
- `view_count`, `like_count`, `trend_score`, `score`, `score_updated_at`
- `released_at`, `duration_seconds`, `bpm`, `energy`, `danceability`, `loudness`
- `musical_key`, `musical_scale`, `beats_count`, `beats_confidence`
- `key_strength`, `spectral_centroid`, `zero_crossing_rate`, `is_analyzed`
- `created_at`, `updated_at`

스키마에 존재하는 컬럼:
- 코드 사용 컬럼 전체 존재

판단:
- 문제 없음
- `String/CHAR(36)`, `int/INT`, `float/FLOAT`, `Double/DOUBLE`, `LocalDate/DATE`, `LocalDateTime/DATETIME` 호환

### `user_songs`

코드에서 사용하는 컬럼:
- `id`, `user_id`, `song_id`, `is_saved`, `rating`, `play_count`, `last_played_at`

스키마에 존재하는 컬럼:
- 코드 사용 컬럼 전체와 `created_at`

판단:
- 문제 없음

### `user_preferences`

코드에서 사용하는 컬럼:
- `id`, `user_id`
- `preferred_generations`, `preferred_moods`, `preferred_artists`
- `preferred_genres`, `preferred_video_types`
- `excluded_genres`, `excluded_keywords`
- `created_at`, `updated_at`

스키마에 존재하는 컬럼:
- 코드 사용 컬럼 전체 존재

판단:
- 문제 없음
- JSON 컬럼을 직렬화된 Java `String`으로 처리하며 현재 Service 구현과 호환

### `radio_sessions`

코드에서 사용하는 컬럼:
- `id`, `user_id`, `mood`, `story`, `era`, `genre`, `situation`
- `desired_mood`, `video_type`, `preferred_artist`, `excluded_keywords`
- `recommendation_source`, `dj_ment`, `comfort_text`, `novel_excerpt`, `created_at`

스키마에 존재하는 컬럼:
- 코드 사용 컬럼 전체와 `title`, `playlist_id`

판단:
- 컬럼 누락 없음
- `title`, `playlist_id`는 스키마에 있지만 현재 RadioMapper에서 저장하지 않음

### `radio_recommendations`

코드에서 사용하는 컬럼:
- `session_id`, `song_id`, `order_num`, `reason`

최종 스키마에 존재하는 컬럼:
- `id`, `session_id`, `song_id`, `sort_order`, `reason`
- `title`, `artist`, `youtube_url`, `thumbnail_url`, `source`, `created_at`

판단:
- **컬럼명 불일치**
- 코드의 `order_num`이 최종 스키마에는 없음

### `playlists`

코드에서 사용하는 컬럼:
- `id`, `user_id`, `name`, `mood_tag`, `is_public`, `created_at`

스키마에 존재하는 컬럼:
- 코드 사용 컬럼 전체와 `description`, `updated_at`

판단:
- 문제 없음

### `playlist_songs`

코드에서 사용하는 컬럼:
- `id`, `playlist_id`, `song_id`, `order_num`, `added_at`

스키마에 존재하는 컬럼:
- 코드 사용 컬럼 전체 존재

판단:
- 문제 없음

### `youtube_channels`

코드에서 사용하는 컬럼:
- `id`, `url`, `channel_id`, `channel_name`, `uploads_playlist`
- `subscriber_count`, `last_checked_at`, `created_at`, `updated_at`

스키마에 존재하는 컬럼:
- 코드 사용 컬럼 전체 존재

판단:
- 문제 없음
- DTO의 DATETIME 필드가 `String`이므로 동작은 가능하지만 `LocalDateTime` 사용이 더 명확함

### `youtube_videos_raw`

코드에서 사용하는 컬럼:
- `id`, `channel_id`, `video_id`, `video_url`, `title`, `duration_seconds`
- `published_at`, `is_imported`, `is_analyzed`, `collect_status`
- `fetched_at`, `updated_at`

스키마에 존재하는 컬럼:
- 코드 사용 컬럼 전체 존재

판단:
- 문제 없음

### `song_likes`

코드에서 사용하는 컬럼:
- `id`, `user_id`, `song_id`, `created_at`

스키마에 존재하는 컬럼:
- 코드 사용 컬럼 전체 존재

판단:
- 문제 없음

### `score_logs`

코드에서 사용하는 컬럼:
- 없음

스키마에 존재하는 컬럼:
- `id`, `song_id`, `score_before`, `score_after`, `view_count`
- `like_count`, `trend_score`, `logged_at`

판단:
- 문제 없음
- 현재 Java 코드에서는 사용하지 않음

---

## 4. Mapper 매칭 결과

| Mapper | XML namespace 일치 | statement id 일치 | @Param 일치 | 상태 |
|---|---|---|---|---|
| RawVideoMapper | 일치 | 일치 | 일치 | 정상 |
| LikeMapper | 일치 | 일치 | 일치 | 정상 |
| PlaylistMapper | 일치 | 일치 | 일치 | 정상 |
| PreferenceMapper | 일치 | 일치 | 일치 | 정상 |
| RadioMapper | 일치 | 일치 | 일치 | DB 컬럼 불일치 |
| SongDao | 일치 | 일치 | 일치 | 정상 |
| UserMapper | 일치 | 일치 | 일치 | 정상 |
| UserSongMapper | 일치 | 일치 | 일치 | 정상 |
| YoutubeMapper | 일치 | 일치 | 일치 | 정상 |

Mapper interface 메서드와 XML statement는 총 72개 모두 매칭됩니다.

---

## 5. 누락 또는 불일치 항목

- 누락 테이블: 문제 없음
- 누락 컬럼: `radio_recommendations.order_num`
- 컬럼명 불일치: 코드 `order_num` / 최종 스키마 `sort_order`
- 타입 불일치: 기능을 막는 불일치 없음
- Mapper namespace 불일치: 문제 없음
- Mapper statement id 불일치: 문제 없음
- `@Param` 이름 불일치: 문제 없음
- SQL 자동 실행 설정 누락: 있음

### 외래키/관계

필요한 주요 관계에는 외래키가 존재합니다.

- 사용자 → 선호도·라디오·플레이리스트·좋아요·저장곡
- 곡 → 추천곡·플레이리스트 곡·좋아요·저장곡
- 라디오 세션 → 추천곡
- 유튜브 채널 → 원본 영상

`youtube_videos_raw.channel_id`에는 `ON DELETE CASCADE`가 없으므로 영상이 존재하는 채널 삭제는 실패할 수 있습니다. 데이터 무결성에는 유리하며 필수 DB 수정 사항은 아닙니다.

### 초기 데이터

초기 데이터가 포함된 테이블:

- `users`, `songs`, `user_songs`
- `radio_sessions`, `radio_recommendations`
- `score_logs`, `playlists`, `playlist_songs`

초기 데이터가 없는 테이블:

- `user_preferences`
- `youtube_channels`
- `youtube_videos_raw`
- `song_likes`

따라서 초기 실행 시 유튜브 영상·좋아요·사용자 선호도 관련 API 결과가 비어 보일 수 있습니다. 또한 기본 곡 데이터의 `mood`, `generation` 값이 비어 있어 해당 조건 기반 추천 결과가 적거나 없을 수 있습니다.

### 자동 실행 여부

- SQL 파일명: `kpop_radio_schema.sql`
- `schema.sql` 또는 `data.sql`: 아님
- `spring.sql.init.*` 설정: 없음
- 애플리케이션 실행만으로 DB 자동 생성: **되지 않음**
- 스키마 적용 방식: 수동 실행 필요

---

## 6. DB 수정이 필요하다면 SQL 작성

현재 Mapper 코드를 기준으로 DB 컬럼명을 `order_num`으로 맞추는 최소 수정입니다.

```sql
ALTER TABLE radio_recommendations
    CHANGE COLUMN sort_order order_num TINYINT NOT NULL DEFAULT 1;
```

MySQL에서는 컬럼 이름 변경 시 해당 컬럼을 사용하는 `idx_session` 인덱스도 함께 갱신됩니다.

파일 및 DB는 수정하지 않았습니다.