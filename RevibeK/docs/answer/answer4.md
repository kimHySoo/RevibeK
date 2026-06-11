## 1. 전체 결론

현재 백엔드는 **핵심 CRUD, JWT 인증, DB 기반 라디오 추천, 외부 API fallback 구조까지 구현된 상태**입니다.

정적 분석 결과 Java Mapper interface와 XML의 namespace/id는 모두 일치하며, 주요 SQL 컬럼도 `kpop_radio_schema.sql`과 일치합니다.

그러나 현재 상태를 완성된 백엔드로 보기는 어렵습니다.

- 라디오 생성과 `selectedSongs`/Playlist 저장이 연결되지 않음
- DB 스키마가 자동 실행되지 않음
- 기본 OAuth 설정에서 Bean 생성 실패 가능성
- Preference 등 일부 API의 인증/권한 검증 취약
- YouTube → FastAPI → songs/Qdrant 처리 흐름이 단절됨
- 외부 API와 추천 SQL 오류가 fallback으로 숨겨짐
- 자동 테스트가 context load 테스트 하나뿐임

---

## 2. 백엔드 전체 구조 요약

전체 흐름은 다음과 같습니다.

```text
Controller
  → Service
    → MyBatis Mapper
      → MySQL

Radio
  → Preference 조회
  → SongDao DB 추천
  → GMS DJ 멘트 또는 fallback
  → Google TTS 또는 브라우저 TTS
  → radio_sessions / radio_recommendations 저장

Explore
  → YouTube ID 추출
  → FastAPI 분석 또는 mock
  → songs 저장
  → Qdrant 검색 또는 DB 점수 fallback
```

주요 테이블은 `users`, `songs`, `user_preferences`, `radio_sessions`, `radio_recommendations`, `playlists`, `playlist_songs`, `song_likes`, `youtube_videos_raw`입니다.

---

## 3. 실행 전 반드시 확인할 문제

1. [`application.properties`](C:/Users/jaewo/OneDrive/Desktop/RevibeK2/RevibeK/src/main/resources/application.properties:2)에 기본적으로 `secret` 프로필을 포함하지만 `application-secret.properties`는 없습니다.
2. Google OAuth `client-id`, `client-secret` 속성이 빈 값으로 등록됩니다. OAuth 자동 설정 과정에서 Bean 생성 오류가 발생할 가능성이 높습니다.
3. 실제 테스트에서는 OAuth 값을 강제로 설정하고 있습니다: [`RevibeKApplicationTests.java`](C:/Users/jaewo/OneDrive/Desktop/RevibeK2/RevibeK/src/test/java/com/ssafy/revibek/RevibeKApplicationTests.java:7).
4. 스키마 파일 이름이 `schema.sql`이 아니며 `spring.sql.init.*` 설정도 없습니다. 애플리케이션 실행만으로 DB가 생성되지 않습니다.
5. MySQL에 `kpop_radio` DB와 `SSAFY/SSAFY` 계정이 먼저 준비되어야 합니다.

읽기 전용 환경 정책 때문에 Maven 컴파일 및 context 테스트는 실행하지 못했습니다. 정적 분석에서는 명확한 Java 문법 오류나 Mapper method 누락은 발견되지 않았습니다.

---

## 4. 기능 흐름 연결 상태

| 기능 | 상태 | 판단 |
|---|---|---|
| 회원가입/로그인/JWT | 연결됨 | 로컬 로그인과 JWT 발급 가능 |
| Google OAuth | 부분 연결 | 기본 설정에서 시작 실패 가능 |
| 사용자 취향 | 연결됨 | 인증 없이 다른 사용자 ID 지정 가능 |
| Radio 추천 | 연결됨 | DB 추천과 fallback 구현 |
| Radio 저장 | 연결됨 | 세션과 추천곡 저장 |
| `selectedSongs` 처리 | 미구현 | DTO에만 존재 |
| Radio → Playlist 저장 | 미구현 | `saveAsPlaylist`, `title` 미사용 |
| Like API | 연결됨 | 좋아요 테이블 및 songs 카운트 갱신 |
| YouTube 수집 | 부분 연결 | raw 영상 저장까지만 연결 |
| FastAPI 분석 | 부분 연결 | songs 분석과 raw 영상 흐름 분리 |
| Qdrant | 부분 연결 | disabled fallback은 구현됨 |

---

## 5. DB / Mapper 디버깅 결과

모든 Mapper의 namespace와 statement ID가 interface와 일치합니다.

- `UserMapper`
- `SongDao`
- `UserSongMapper`
- `PreferenceMapper`
- `RadioMapper`
- `PlaylistMapper`
- `LikeMapper`
- `YoutubeMapper`
- `RawVideoMapper`

DDL에도 Mapper가 사용하는 주요 테이블과 컬럼이 정의되어 있습니다.

주의할 문제:

- [`SongMapper.xml`](C:/Users/jaewo/OneDrive/Desktop/RevibeK2/RevibeK/src/main/resources/mapper/song/SongMapper.xml:93)의 `selectSongByTitle`은 `LIKE` 검색이지만 반환 타입은 단일 `SongDto`입니다. 여러 곡이 검색되면 `TooManyResultsException`이 발생합니다.
- Radio 추천 SQL 오류는 [`RadioService.java`](C:/Users/jaewo/OneDrive/Desktop/RevibeK2/RevibeK/src/main/java/com/ssafy/revibek/radio/service/RadioService.java:212) 등에서 모두 무시됩니다. SQL 오류가 발생해도 단순히 추천 결과가 없는 것처럼 처리됩니다.
- Like 개수는 `song_likes` 실제 행 수와 `songs.like_count` 양쪽에 저장됩니다. 초기 mock 데이터에는 `songs.like_count`만 있고 `song_likes` 데이터가 없어 API별 개수가 다르게 보일 수 있습니다.

---

## 6. 인증 / OAuth / JWT 디버깅 결과

JWT subject에 `users.id`를 저장하고, 필터에서 `Authentication.getName()`으로 복원하는 흐름은 일관적입니다.

문제점:

- Refresh Token은 [`RefreshTokenStore.java`](C:/Users/jaewo/OneDrive/Desktop/RevibeK2/RevibeK/src/main/java/com/ssafy/revibek/auth/RefreshTokenStore.java:11)의 메모리에만 저장됩니다. 서버 재시작, 다중 서버 환경에서 모든 refresh token이 무효화됩니다.
- [`PreferenceController.java`](C:/Users/jaewo/OneDrive/Desktop/RevibeK2/RevibeK/src/main/java/com/ssafy/revibek/preference/controller/PreferenceController.java:76)는 JWT보다 `X-USER-ID`와 query `userId`를 우선합니다.
- Preference API는 [`SecurityConfig.java`](C:/Users/jaewo/OneDrive/Desktop/RevibeK2/RevibeK/src/main/java/com/ssafy/revibek/config/SecurityConfig.java:66)의 `anyRequest().permitAll()` 대상입니다. 다른 사용자의 취향 조회·수정·삭제가 가능합니다.
- Song 등록/수정/삭제, Analysis batch, Qdrant embed, YouTube 수집 API도 공개 상태입니다.
- Radio의 `X-USER-ID` fallback은 `/api/radio/**`가 인증 필수이므로 일반 요청에서는 사실상 사용되지 않습니다.

---

## 7. 외부 API 디버깅 결과

- GMS 비활성화 또는 실패 시 DJ 멘트 fallback을 반환합니다.
- TTS 비활성화 시 브라우저 TTS 정보를 반환합니다.
- FastAPI 비활성화 또는 장애 시 mock 분석 결과를 반환합니다.
- Qdrant 비활성화 또는 장애 시 DB 점수 추천으로 fallback합니다.
- YouTube 비활성화 또는 장애 시 sample 영상 응답을 반환합니다.

문제는 실패가 정상 응답처럼 보일 수 있다는 점입니다.

또한 YouTube가 저장한 `youtube_videos_raw`를 조회하는 `RawVideoMapper`와 `YoutubeMapper.findPendingVideos()`는 실제 분석 서비스에서 사용되지 않습니다. 따라서 **YouTube 수집 → FastAPI 분석 → songs 생성 → Qdrant 저장** 자동 파이프라인은 연결되어 있지 않습니다.

Qdrant는 [`QdrantService.java`](C:/Users/jaewo/OneDrive/Desktop/RevibeK2/RevibeK/src/main/java/com/ssafy/revibek/qdrant/QdrantService.java:99)에서 song ID를 실제 UUID로 변환합니다. 스키마 mock ID인 `s001-...` 형식은 UUID가 아니므로 batch embed가 실패합니다.

---

## 8. 예상 오류 TOP 10

1. 빈 Google OAuth client 설정으로 인한 OAuth Bean 생성 실패
2. DB/schema 미생성으로 인한 테이블 없음 오류
3. `selectSongByTitle` 다중 결과로 `TooManyResultsException`
4. `selectedSongs`와 `saveAsPlaylist`가 무시되는 기능 오류
5. Qdrant batch embed에서 mock song ID의 `UUID.fromString()` 실패
6. Preference API의 임의 사용자 ID 접근
7. Radio 추천 SQL 오류가 숨겨져 `DB_EMPTY`로 반환
8. Like API와 `songs.like_count` 데이터 불일치
9. YouTube raw 영상이 FastAPI 분석으로 전달되지 않음
10. Refresh Token이 서버 재시작 후 전부 무효화됨

---

## 9. 지금 바로 디버깅해야 할 파일 TOP 5

1. [`SecurityConfig.java`](C:/Users/jaewo/OneDrive/Desktop/RevibeK2/RevibeK/src/main/java/com/ssafy/revibek/config/SecurityConfig.java:43)
2. [`application.properties`](C:/Users/jaewo/OneDrive/Desktop/RevibeK2/RevibeK/src/main/resources/application.properties:1)
3. [`RadioService.java`](C:/Users/jaewo/OneDrive/Desktop/RevibeK2/RevibeK/src/main/java/com/ssafy/revibek/radio/service/RadioService.java:41)
4. [`QdrantService.java`](C:/Users/jaewo/OneDrive/Desktop/RevibeK2/RevibeK/src/main/java/com/ssafy/revibek/qdrant/QdrantService.java:88)
5. [`kpop_radio_schema.sql`](C:/Users/jaewo/OneDrive/Desktop/RevibeK2/RevibeK/src/main/resources/sql/kpop_radio_schema.sql:1)

---

## 10. 실제 테스트 순서

1. `mvn clean compile`
2. MySQL에 `kpop_radio_schema.sql` 실행
3. `mvn test`로 context load 확인
4. 이메일 인증 mock → 회원가입 → 로그인 → JWT 발급
5. JWT로 `/api/users/me` 확인
6. Preference 저장 및 조회
7. Radio 생성 후 `radio_sessions`, `radio_recommendations` 확인
8. Like 추가/삭제 후 `song_likes`, `songs.like_count` 비교
9. Playlist 생성 및 곡 추가
10. YouTube, FastAPI, Qdrant를 각각 disabled/enabled 상태로 테스트

---

## 11. 다음 작업 순서

1. 기본 애플리케이션 context와 DB 초기화를 먼저 안정화
2. 인증이 필요한 API 범위를 재정의
3. Radio의 `selectedSongs`와 Playlist 저장 연결
4. Radio 추천 예외 로깅 추가
5. Like 개수의 단일 기준 결정
6. YouTube → FastAPI → songs → Qdrant 파이프라인 연결
7. Refresh Token 영속화
8. 서비스 및 Mapper 통합 테스트 추가

---

## 12. 최종 판단

현재 백엔드는 **부분 기능 시연과 fallback 기반 실행은 가능한 구조**이지만, 실제 서비스 흐름이 완전히 연결된 상태는 아닙니다.

가장 먼저 확인해야 할 것은 **OAuth Bean 생성 여부와 DB 스키마 초기화**입니다. 이후 핵심 미완성 기능은 **Radio의 `selectedSongs` 처리 및 Playlist 저장 연결**, 그리고 **YouTube/FastAPI/Qdrant 자동 파이프라인**입니다.
