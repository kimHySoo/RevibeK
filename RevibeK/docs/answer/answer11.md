# RevibeK Vue 프론트엔드 설계를 위한 백엔드 구조/API/JSON 분석 결과

## 1. 전체 결론

- RevibeK는 Spring Boot 4.0.6, Java 21, Spring Security/JWT, MyBatis, MySQL 기반 백엔드이다.
- 현재 저장소에는 `FE` 폴더, `package.json`, `.vue`, 프론트엔드용 HTML/JavaScript/CSS 소스가 없다. Vue 프로젝트를 새로 구성해야 한다.
- 핵심 사용자 흐름은 현재 API로 구현 가능하다.
  - 이메일 인증 -> 회원가입 -> 로그인
  - 취향 저장 또는 라디오 입력
  - 라디오 생성 및 DJ 멘트/TTS/추천곡 확인
  - 곡 좋아요
  - 라디오 생성과 동시에 플레이리스트 자동 저장 또는 기존 플레이리스트에 수동 추가
  - 내 라디오/플레이리스트/좋아요 조회
- `POST /api/radio` 요청에서 `saveAsPlaylist: true`를 보내면 백엔드가 플레이리스트를 자동 생성한다. 응답의 `playlistId`로 상세 화면에 이동할 수 있다.
- 인증 공개 범위는 `/api/auth/**`와 `GET /api/songs/**`이다. 그 외 핵심 API는 JWT가 필요하다.
- 응답 형식은 도메인별로 통일되어 있지 않다. DTO 객체, 문자열, `{message}`, `ApiResponseDto`, `ErrorResponse`가 혼재하므로 axios 모듈에서 그대로 반환하고 화면 계층에서 타입별로 처리해야 한다.
- 중요한 DB 실행 주의사항: 스키마 앞부분은 `radio_recommendations.order_num`을 만들고 후반 `ALTER TABLE`에서 `sort_order`로 변경한다. `RadioMapper.xml`은 최종 컬럼인 `sort_order`를 사용하므로 스키마 파일을 끝까지 순서대로 실행해야 한다.

## 2. 백엔드 파일 구조 분석

### 루트 구조

```text
RevibeK/
  .mvn/                       Maven Wrapper 설정
  bin/                        빌드/복사 산출물로 판단, 분석 기준에서 제외
  docs/
    answer/                   기존 분석 답변
    prompt/                   기존 요청 프롬프트
  src/
    main/
      java/com/ssafy/revibek/ 실제 Java 소스
      resources/              설정, MyBatis XML, SQL
  mvnw
  mvnw.cmd
  pom.xml
```

`answer/`, `prompt/` 루트 폴더도 있으나 실제 백엔드 소스가 아니다. `FE` 폴더는 존재하지 않는다.

### Java 패키지 구조

```text
com.ssafy.revibek
  ai/          Claude GMS, Google TTS API
  analysis/    FastAPI 음악 분석
  auth/        JWT 필터/토큰/refresh token 저장/OAuth 성공 처리
  common/      공통 응답/예외
  config/      Security, CORS, Qdrant, FastAPI launcher 등
  explore/     YouTube URL 기반 유사곡 탐색
  like/        곡 좋아요
  playlist/    플레이리스트
  preference/  사용자 취향
  qdrant/      벡터 저장/유사곡 검색
  radio/       라디오 생성/DJ 멘트/추천
  song/        곡 CRUD/검색/추천
  tts/         라디오용 TTS 및 브라우저 fallback
  user/        회원가입/로그인/내 정보
  usersong/    저장곡/별점/재생 수
  youtube/     YouTube 채널/영상 수집
```

### 주요 소스 목록

- Controller 14개: `AuthController`, `UserController`, `RadioController`, `PlaylistController`, `LikeController`, `SongController`, `UserSongController`, `PreferenceController`, `ExploreController`, `AnalysisController`, `QdrantController`, `YoutubeController`, `AiController`, `GoogleTtsController`
- Service 계열: `AuthService`, `EmailVerificationService`, `UserService`, `RadioService`, `PlaylistService`, `LikeService`, `PreferenceService`, `UserSongService`, `ExploreService`, `SongService/Impl`, `AnalysisService/Impl`, `YoutubeService/Impl`, `QdrantService`, `ClaudeGmsService`, `GoogleTtsService`, `TtsService`, `AiDjMentService` 등
- Mapper interface: `UserMapper`, `RadioMapper`, `PlaylistMapper`, `LikeMapper`, `PreferenceMapper`, `UserSongMapper`, `SongDao`, `YoutubeMapper`, `RawVideoMapper`
- MyBatis XML 위치: `src/main/resources/mapper/{domain}/*Mapper.xml`
- DB schema: `src/main/resources/sql/kpop_radio_schema.sql`
- 설정: `src/main/resources/application.properties`
- 실제 프론트엔드: 없음

### DB 테이블

| 테이블 | 역할 |
|---|---|
| `users` | 회원/로그인 제공자 |
| `songs` | 곡, YouTube, 점수, 음악 분석 특성 |
| `user_songs` | 저장곡, 별점, 재생 수 |
| `user_preferences` | 세대/감정/아티스트/장르/영상 취향 |
| `radio_sessions` | 라디오 입력, DJ 멘트, 추천 출처 |
| `radio_recommendations` | 라디오 세션 추천곡 |
| `score_logs` | 곡 점수 변경 이력 |
| `playlists` | 사용자 플레이리스트 |
| `playlist_songs` | 플레이리스트 곡 |
| `youtube_channels` | 수집 채널 |
| `youtube_videos_raw` | 수집/분석 대기 영상 |
| `song_likes` | 사용자별 좋아요 |

### application.properties 핵심

- 기본 서버 API는 별도 context path 없이 `/api/...`를 사용한다.
- CORS 기본 허용: `localhost:3000`, `localhost:5173`, `127.0.0.1:3000`, `127.0.0.1:5173`
- access token 기본 만료: `3,600,000ms`(1시간)
- refresh token 기본 만료: `1,209,600,000ms`(14일)
- 이메일 인증 기본 모드: `mock`, 기본 코드 `123456`
- YouTube/FastAPI/Qdrant/TTS/GMS는 설정에 따라 비활성 또는 fallback 동작
- GMS, Google TTS 관련 프로퍼티가 중복 선언되어 있어 최종 적용값을 환경별로 확인해야 한다.

## 3. 주요 도메인 구조

### auth

- 역할: 이메일 인증, 회원가입, 로그인, JWT 발급/재발급/로그아웃, Google OAuth 성공 처리
- Controller: `user/controller/AuthController`
- Service: `AuthService`, `EmailVerificationService`
- DTO: `EmailVerificationSendRequestDto`, `EmailVerificationCheckRequestDto`, `UserRegisterRequestDto`, `UserLoginRequestDto`, `AuthTokenResponseDto`, `RefreshTokenRequestDto`, `LogoutRequestDto`
- Mapper/XML Mapper: `UserMapper` / `mapper/user/UserMapper.xml`
- 관련 DB 테이블: `users`; refresh token과 이메일 인증 코드는 현재 인메모리 저장소
- 프론트엔드 기능: 이메일 인증, 회원가입, 로그인, token refresh, 로그아웃

### user

- 역할: 현재 로그인 사용자 조회/수정/탈퇴
- Controller: `UserController`
- Service: `UserService`
- DTO: `UserResponseDto`, `UserUpdateRequestDto`, `UserAuthDto`
- Mapper/XML Mapper: `UserMapper` / `mapper/user/UserMapper.xml`
- 관련 DB 테이블: `users`
- 프론트엔드 기능: My Page, 프로필 수정, 회원 탈퇴

### radio

- 역할: 감정/상황/세대/장르 기반 추천, AI DJ 멘트, TTS, 세션 저장, 선택적 플레이리스트 자동 생성
- Controller: `RadioController`
- Service: `RadioService`, `AiDjMentService`, `TtsService`
- DTO: `RadioCreateRequestDto`, `RadioCreateResponseDto`, `RadioResponseDto`, `RadioSelectedSongDto`, `RecommendedSongResponseDto`, `RadioSongResponseDto`, `TtsFallbackResponseDto`
- Mapper/XML Mapper: `RadioMapper` / `mapper/radio/RadioMapper.xml`
- 관련 DB 테이블: `radio_sessions`, `radio_recommendations`, `songs`, 선택 시 `playlists`, `playlist_songs`
- 프론트엔드 기능: 라디오 생성 폼, DJ 멘트 재생, 추천곡 결과, 라디오 이력, 자동 플레이리스트 저장

### playlist

- 역할: 플레이리스트 생성/조회/삭제, 곡 추가/삭제
- Controller: `PlaylistController`
- Service: `PlaylistService`
- DTO: `PlaylistDto`, `PlaylistItemDto`
- Mapper/XML Mapper: `PlaylistMapper` / `mapper/playlist/PlaylistMapper.xml`
- 관련 DB 테이블: `playlists`, `playlist_songs`, `songs`
- 프론트엔드 기능: 내 플레이리스트 목록/상세, 수동 곡 추가/삭제

### like

- 역할: 곡 좋아요 추가/삭제/상태/목록/개수
- Controller: `LikeController`
- Service: `LikeService`
- DTO: `LikeDto`, `LikeStatusDto`, 응답 일부 `SongDto`
- Mapper/XML Mapper: `LikeMapper` / `mapper/like/LikeMapper.xml`
- 관련 DB 테이블: `song_likes`, `songs`
- 프론트엔드 기능: 추천곡 하트 토글, 좋아요 곡 목록

### song

- 역할: 곡 CRUD, 제목 검색, 장르 조회, 점수 추천
- Controller: `SongController`
- Service: `SongService`, `SongServiceImpl`
- DTO: `SongDto`
- Mapper/XML Mapper: `SongDao` / `mapper/song/SongMapper.xml`
- 관련 DB 테이블: `songs`
- 프론트엔드 기능: 공개 곡 탐색/검색/상세, 추천 목록

### usersong

- 역할: 곡 저장, 저장 목록, 별점, 재생 수
- Controller: `UserSongController`
- Service: `UserSongService`
- DTO: `UserSongRequestDto`, `UserSongResponseDto`
- Mapper/XML Mapper: `UserSongMapper` / `mapper/usersong/UserSongMapper.xml`
- 관련 DB 테이블: `user_songs`, `songs`
- 프론트엔드 기능: 저장곡, 별점, 재생 이력

### youtube

- 역할: YouTube 채널 영상 수집, 비활성/실패 시 fallback 응답
- Controller: `YoutubeController`
- Service: `YoutubeService`, `YoutubeServiceImpl`
- DTO: `YoutubeChannelDto`, `YoutubeVideoDto`, `YoutubeVideoResponseDto`, `YoutubeFallbackResponseDto`
- Mapper/XML Mapper: `YoutubeMapper` / `mapper/youtube/YoutubeMapper.xml`
- 관련 DB 테이블: `youtube_channels`, `youtube_videos_raw`
- 프론트엔드 기능: 일반 사용자 화면보다는 운영/수집 상태 표시용

### analysis

- 역할: 곡을 FastAPI로 분석하고 결과 저장, 비활성/실패 시 mock 분석
- Controller: `AnalysisController`
- Service: `AnalysisService`, `AnalysisServiceImpl`, `FastApiClient`
- DTO: `AnalyzeRequestDto`, `AnalyzeResponseDto`, `RawVideoDto`
- Mapper/XML Mapper: `RawVideoMapper` / `mapper/analysis/RawVideoMapper.xml`
- 관련 DB 테이블: `songs`, `youtube_videos_raw`
- 프론트엔드 기능: 운영용 분석 실행/상태 표시

### explore

- 역할: YouTube URL로 곡을 찾거나 등록한 뒤 유사곡 탐색
- Controller: `ExploreController`
- Service: `ExploreService`
- DTO: `ExploreResponseDto`, 내부적으로 `SongDto`
- Mapper/XML Mapper: 전용 Mapper 없음; song/analysis/qdrant 서비스 사용
- 관련 DB 테이블: `songs`; Qdrant 외부 벡터 저장소
- 프론트엔드 기능: URL 기반 유사곡 탐색

### qdrant

- 역할: 곡 벡터 upsert, 유사곡 검색, 실패 시 DB 점수 추천 fallback
- Controller: `QdrantController`
- Service: `QdrantService`, `SongVectorUtil`
- DTO: `VectorSearchResponseDto`, `SongDto`
- Mapper/XML Mapper: 전용 MyBatis Mapper 없음
- 관련 DB 테이블: `songs`; 외부 Qdrant collection `revibek_songs`
- 프론트엔드 기능: 유사곡 결과와 `source: qdrant|fallback` 표시

### ai

- 역할: Claude GMS 텍스트 생성, 텍스트 생성 후 Google TTS 결합
- Controller: `AiController`, `GoogleTtsController`
- Service: `ClaudeGmsService`, `GoogleTtsService`, `GmsCreditBudgetTracker`
- DTO: `AiChatRequestDto/ResponseDto`, `ChatTtsRequestDto/ResponseDto`, `TtsSynthesizeRequestDto/ResponseDto`, `TtsVoiceResponseDto`
- Mapper/XML Mapper: 없음
- 관련 DB 테이블: 없음
- 프론트엔드 기능: 보조 AI 기능, TTS 디버그/운영 화면

### tts

- 역할: 라디오 DJ 멘트를 Google TTS로 합성하고 실패 시 브라우저 TTS용 텍스트 제공
- Controller: 전용 Controller 없음; radio 및 `/api/ai/tts`에서 사용
- Service: `TtsService`, `GoogleTtsService`
- DTO: `TtsResponseDto`, `TtsFallbackResponseDto`, AI TTS DTO
- Mapper/XML Mapper: 없음
- 관련 DB 테이블: 없음
- 프론트엔드 기능: `tts.audioUrl` 재생 또는 `BROWSER_TTS`일 때 Web Speech API 사용

### preference

- 역할: 사용자 취향 생성/조회/수정/삭제
- Controller: `PreferenceController`
- Service: `PreferenceService`
- DTO: `UserPreferenceRequestDto`, `UserPreferenceDto`
- Mapper/XML Mapper: `PreferenceMapper` / `mapper/preference/PreferenceMapper.xml`
- 관련 DB 테이블: `user_preferences`
- 프론트엔드 기능: 온보딩 및 라디오 기본값

## 4. Controller/API 목록

인증 기준은 `SecurityConfig` 기준이다. `인증`은 `Authorization: Bearer ...`가 필요하다는 의미이다.

### Auth/User

| API | 인증 | Request | Response | 관련 DTO/Service | 화면/주의 |
|---|---|---|---|---|---|
| `POST /api/auth/email/send` | 공개 | `{email}` | 문자열 | `EmailVerificationSendRequestDto` / `EmailVerificationService` | Signup; mock 모드 기본 코드 123456 |
| `POST /api/auth/email/verify` | 공개 | `{email, code}` | 문자열 | `EmailVerificationCheckRequestDto` | Signup |
| `POST /api/auth/signup` | 공개 | `{nickname,email,password}` | 문자열 | `UserRegisterRequestDto` / `AuthService` | 이메일 인증 선행 필요 |
| `POST /api/auth/login` | 공개 | `{email,password}` | `AuthTokenResponseDto` | `UserLoginRequestDto` / `AuthService` | Login |
| `POST /api/auth/refresh` | 공개 | `{refreshToken}` | `AuthTokenResponseDto` | `RefreshTokenRequestDto` / `AuthService` | refresh token 회전 |
| `POST /api/auth/logout` | 공개 | `{refreshToken}` | 문자열 | `LogoutRequestDto` / `AuthService` | 로컬 token도 제거 |
| `GET /api/users/me` | 인증 | 없음 | `UserResponseDto` | `UserService` | My Page |
| `PUT /api/users/me` | 인증 | `{nickname,email}` | 문자열 | `UserUpdateRequestDto` | My Page |
| `DELETE /api/users/me` | 인증 | 없음 | 문자열 | `UserService` | 탈퇴 확인 UI 필요 |

### Radio/Playlist/Like

| API | 인증 | Request | Response | 관련 DTO/Service | 화면/주의 |
|---|---|---|---|---|---|
| `POST /api/radio` | 인증 | `RadioCreateRequestDto` | `RadioCreateResponseDto` | `RadioService` | Radio Create/Result; 자동 playlist 가능 |
| `GET /api/radio/{id}` | 인증 | path `id` | `RadioResponseDto` | `RadioService` | 본인 세션만 조회 |
| `GET /api/radio/me` | 인증 | 없음 | `RadioResponseDto[]` | `RadioService` | My Page 라디오 이력 |
| `POST /api/playlists` | 인증 | `PlaylistDto` 중 `name,moodTag,isPublic` | `PlaylistDto` | `PlaylistService` | Playlist 생성 |
| `GET /api/playlists` | 인증 | 없음 | `PlaylistDto[]` | `PlaylistService` | 목록 응답의 `items`는 보통 null |
| `GET /api/playlists/{playlistId}` | 인증 | path | `PlaylistDto` + `items` | `PlaylistService` | 상세 |
| `POST /api/playlists/{playlistId}/items` | 인증 | `{songId}` | `PlaylistItemDto` | `PlaylistService` | 중복 곡 거부 |
| `DELETE /api/playlists/{playlistId}/items/{itemId}` | 인증 | path | `{message}` | `PlaylistService` | `songId`가 아니라 item `id` 사용 |
| `DELETE /api/playlists/{playlistId}` | 인증 | path | `{message}` | `PlaylistService` | 삭제 |
| `POST /api/likes` | 인증 | `{songId}` | `LikeStatusDto` | `LikeService` | 하트 on |
| `DELETE /api/likes/{songId}` | 인증 | path | `LikeStatusDto` | `LikeService` | 하트 off |
| `GET /api/likes/{songId}/status` | 인증 | path | `LikeStatusDto` | `LikeService` | 카드 초기 상태 |
| `GET /api/likes` | 인증 | 없음 | `LikeDto[]` | `LikeService` | ID 중심 목록 |
| `GET /api/likes/songs` | 인증 | 없음 | `SongDto[]` | `LikeService` | 화면 표시용 목록 |
| `GET /api/likes/{songId}/count` | 인증 | path | `{songId,likeCount}` | `LikeService` | Controller는 auth 미사용이나 보안 설정상 인증 필요 |

### Song/UserSong/Preference

| API | 인증 | Request | Response | 관련 DTO/Service | 화면/주의 |
|---|---|---|---|---|---|
| `POST /api/songs` | 인증 | `SongDto` | 문자열 | `SongService` | 운영용 |
| `GET /api/songs` | 공개 | 없음 | `SongDto[]` | `SongService` | 곡 탐색 |
| `GET /api/songs/{id}` | 공개 | path | `SongDto` 또는 오류 문자열 | `SongService` | 곡 상세 |
| `GET /api/songs/search?title=` | 공개 | query | `SongDto` 또는 오류 문자열 | `SongService` | 제목 정확 검색 형태 |
| `GET /api/songs/genre?genre=` | 공개 | query | `SongDto[]` | `SongService` | 장르 목록 |
| `GET /api/songs/recommend` | 공개 | 없음 | `SongDto[]` | `SongService` | 점수 기반 추천 |
| `PUT /api/songs/{id}` | 인증 | `SongDto` | 문자열 | `SongService` | 운영용 |
| `DELETE /api/songs/{id}` | 인증 | path | 문자열 | `SongService` | 운영용 |
| `POST /api/usersongs` | 인증 | `{songId,rating?}` | 문자열 | `UserSongService` | 저장곡 |
| `GET /api/usersongs/me` | 인증 | 없음 | `UserSongResponseDto[]` | `UserSongService` | 저장곡 목록 |
| `PUT /api/usersongs/rating` | 인증 | `{songId,rating}` | 문자열 | `UserSongService` | rating 1~5 |
| `PUT /api/usersongs/play/{songId}` | 인증 | path | 문자열 | `UserSongService` | 재생 시작 시 호출 |
| `DELETE /api/usersongs/{songId}` | 인증 | path | 문자열 | `UserSongService` | 저장 취소 |
| `POST /api/preferences` | 인증 | `UserPreferenceRequestDto` | `ApiResponseDto<UserPreferenceDto>` | `PreferenceService` | 온보딩 저장 |
| `GET /api/preferences/me` | 인증 | 없음 | `ApiResponseDto<UserPreferenceDto>` | `PreferenceService` | 없으면 `data:null` 가능 |
| `PUT /api/preferences/me` | 인증 | `UserPreferenceRequestDto` | `ApiResponseDto<UserPreferenceDto>` | `PreferenceService` | upsert 방식 |
| `DELETE /api/preferences/me` | 인증 | 없음 | `ApiResponseDto<null>` | `PreferenceService` | 취향 초기화 |

### YouTube/FastAPI/Qdrant/AI fallback 관련

| API | 인증 | Request | Response | fallback/주의 |
|---|---|---|---|---|
| `GET /api/explore?url=&limit=10` | 인증 | query | `ExploreResponseDto` | URL 곡 + 유사곡 |
| `POST /api/analysis/{songId}` | 인증 | path | `AnalyzeResponseDto` | FastAPI 비활성/실패 시 `source:"fallback"`, `status:"MOCK"` |
| `POST /api/analysis/batch` | 인증 | 없음 | 문자열 | 운영용 전체 분석 |
| `POST /api/qdrant/embed` | 인증 | 없음 | 문자열 | 운영용 전체 벡터 저장 |
| `GET /api/qdrant/similar/{songId}?limit=10` | 인증 | path/query | `VectorSearchResponseDto` | 실패/결과 없음 시 `source:"fallback"` 및 DB 점수 추천 |
| `POST /api/youtube/channel` | 인증 | `{url}` | `YoutubeFallbackResponseDto` | 비활성/실패 시 `source:"fallback"` |
| `POST /api/youtube/channels` | 인증 | `{urls:[...]}` | `YoutubeFallbackResponseDto[]` | 채널별 source 확인 |
| `POST /api/ai/chat` | 인증 | `AiChatRequestDto` | `AiChatResponseDto` | GMS 설정 의존 |
| `POST /api/ai/chat-tts` | 인증 | `ChatTtsRequestDto` | `ChatTtsResponseDto` | Google TTS 없으면 `ttsMode:"BROWSER_TTS"` |
| `GET /api/ai/tts/voices?languageCode=` | 인증 | query | `TtsVoiceResponseDto[]` | 운영/설정용 |
| `POST /api/ai/tts/synthesize` | 인증 | `TtsSynthesizeRequestDto` | `TtsSynthesizeResponseDto` | 브라우저 TTS fallback 가능 |
| `POST /api/ai/tts/synthesize/audio?preset=` | 인증 | `TtsSynthesizeRequestDto` | `audio/mpeg` 또는 202 text | 202이면 `X-TTS-Mode` 확인 후 브라우저 TTS |

## 5. 인증/JWT 흐름

### 요청/응답

```json
// 이메일 인증 코드 발송
{ "email": "user@example.com" }
```

```json
// 이메일 인증 확인
{ "email": "user@example.com", "code": "123456" }
```

```json
// 회원가입
{ "nickname": "네온DJ", "email": "user@example.com", "password": "password123" }
```

```json
// 로그인
{ "email": "user@example.com", "password": "password123" }
```

```json
// 로그인/재발급 응답
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresIn": 3600000,
  "user": {
    "id": "uuid-user",
    "nickname": "네온DJ",
    "email": "user@example.com",
    "provider": "local"
  }
}
```

### 프론트 저장 제안

- 권장: access token은 메모리(Pinia)에 유지하고, 새로고침 복구가 꼭 필요하면 `sessionStorage`에 저장한다.
- 현재 백엔드는 refresh token을 JSON body로 받으며 HttpOnly cookie를 발급하지 않는다. 구현 가능한 방식은 `localStorage` 저장이지만 XSS 위험이 있다. 운영 보안을 높이려면 향후 백엔드가 refresh token을 `Secure; HttpOnly; SameSite` cookie로 바꾸는 것이 바람직하다.
- 현재 계약을 그대로 쓸 때 키 예시: `revibek.accessToken`, `revibek.refreshToken`.
- 모든 인증 API 헤더:

```text
Authorization: Bearer ACCESS_TOKEN
```

### `/api/users/me` 호출

```javascript
const { data } = await api.get('/api/users/me')
```

axios interceptor가 access token을 자동 추가해야 한다.

### 인증 실패 처리

1. API가 401이면 기존 요청당 한 번만 `/api/auth/refresh` 호출한다.
2. 새 응답의 access/refresh token을 모두 교체한다. 백엔드는 기존 refresh token을 revoke하는 회전 방식이다.
3. 대기 중인 요청을 새 access token으로 재시도한다.
4. refresh도 실패하면 모든 token/user 상태를 제거하고 `/login?redirect=현재경로`로 이동한다.
5. 403은 재발급하지 말고 권한 오류 메시지를 표시한다.
6. 무한 refresh 반복 방지를 위해 `_retry` 플래그와 단일 refresh Promise를 사용한다.

## 6. Radio API 분석

### 생성 요청 필드

| 필드 | 의미 |
|---|---|
| `title` | 자동 생성 플레이리스트 이름 |
| `mood` | 현재 감정/추천 기준 |
| `situation` | 상황 |
| `desiredMood` | 원하는 감정 |
| `story` | 사용자 사연 |
| `era` | 시대/세대 입력. DB 추천 시 내부 정규화 |
| `genre` | 장르 |
| `videoType` | 선호 영상 타입 |
| `preferredArtist` | 선호 아티스트 |
| `excludedKeywords` | 제외 키워드 문자열 |
| `saveAsPlaylist` | true이면 자동 플레이리스트 생성 |
| `selectedSongs` | 자동 저장할 곡을 사용자가 선택한 경우의 목록 |

DTO에는 `@NotBlank`가 없어 모든 필드가 형식상 optional이다. 다만 좋은 추천과 UI 검증을 위해 `mood`, `story`, `era`, `genre`는 프론트에서 필수로 취급하는 것이 적절하다.

### 생성 처리

1. 사용자 preference를 조회해 빈 요청값을 보완한다.
2. mood/era/generation/genre 및 preference 조합을 단계적으로 완화하며 DB 추천을 찾는다.
3. 기본 추천 개수는 5개다.
4. AI DJ 멘트를 생성하고 radio session/recommendation을 저장한다.
5. `saveAsPlaylist=true`이면 `selectedSongs`가 있을 때 선택곡을, 없으면 추천곡 전체를 플레이리스트에 저장한다.
6. TTS를 생성하고 실패 시 `BROWSER_TTS` fallback을 반환한다.

### 화면 처리

- `recommendationSource`를 결과 화면에 작은 배지로 표시할 수 있다.
- `tts.mode === "BROWSER_TTS"`이고 `audioUrl`이 null이면 `window.speechSynthesis`로 `tts.text`를 읽는다.
- 생성 직후 결과는 `recommendedSongs`; 나중에 `GET /api/radio/{id}`로 조회하면 단순화된 `songs` 구조를 사용한다. 두 구조를 프론트의 공통 `SongCardViewModel`로 정규화하는 것이 좋다.
- 자동 저장 성공은 `playlistId !== null`로 판정한다.

## 7. Playlist API 분석

- 자동 저장: `POST /api/radio`에 `saveAsPlaylist:true`. 별도 API 호출 없이 응답에 `playlistId`가 포함된다.
- 수동 저장: `POST /api/playlists` 후 각 추천곡에 대해 `POST /api/playlists/{id}/items`.
- 플레이리스트 목록 응답은 기본 정보이며 상세 곡 목록은 `GET /api/playlists/{id}`로 조회한다.
- 곡 추가 요청은 전체 `PlaylistItemDto`가 아니라 `{songId}`만 보내면 충분하다.
- 항목 삭제 URL의 `{itemId}`는 상세 응답 `items[].id`이다.
- 중복 곡 추가와 존재하지 않는 곡은 400으로 처리된다.
- radio 자동 생성은 존재하지 않는 `songId`를 건너뛰며, 중복 ID를 제거하고 순서를 유지한다.

## 8. Like API 분석

- 추천 카드 초기화 시 곡마다 상태 API를 호출하면 N+1 요청이 된다. 현재 일괄 상태 API가 없으므로 `GET /api/likes/songs` 한 번으로 liked ID Set을 구성하는 방식을 권장한다.
- 토글 성공 응답은 항상 `{songId, liked, likeCount}`이므로 카드 상태와 개수를 즉시 갱신할 수 있다.
- `GET /api/likes/{songId}/count`도 보안 설정상 인증이 필요하다.
- optimistic update를 적용할 수 있지만 실패 시 이전 상태로 복구해야 한다.

## 9. User/Auth API 분석

- 이메일 인증은 메모리 기반이며 서버 재시작 시 상태가 사라질 수 있다.
- 기본 개발 설정은 mock 인증 코드 `123456`이다.
- 회원가입은 이메일 인증 완료 상태를 소비하므로 같은 인증 상태를 재사용할 수 없다.
- 비밀번호는 최소 8자다.
- 로그인 성공 시 access/refresh token 및 user가 한 번에 반환된다.
- refresh 성공 시 refresh token도 새 값으로 교체해야 한다.
- logout API 자체는 공개 경로지만 유효한 refresh token body가 필요하다.
- `/api/users/me`는 로그인 상태 복구와 사용자 정보 최신화에 사용한다.

## 10. Vue 프론트엔드 핵심 사용자 흐름

```text
Landing
  -> Login 또는 Signup
Signup
  -> 이메일 코드 발송
  -> 코드 검증
  -> 회원가입
  -> Login
Login
  -> token/user 저장
  -> 선택적으로 preference 조회/온보딩
Radio Create
  -> 감정/상황/원하는 감정/세대/장르/사연 입력
  -> 자동 저장 여부 및 선택곡 설정
  -> POST /api/radio
Radio Result
  -> DJ 멘트 및 TTS
  -> 추천곡/YouTube 확인
  -> 좋아요 토글
  -> playlistId가 있으면 자동 저장 완료
  -> 없으면 기존/새 플레이리스트에 수동 추가
Playlist
  -> 내 목록
Playlist Detail
  -> 곡 재생/삭제
My Page
  -> 내 정보, 좋아요 곡, 저장곡, 라디오 이력, 취향
```

## 11. Vue 화면 구성 제안

### Landing Page

- route: `/`
- 사용 목적: 서비스 소개, 공개 추천곡 미리보기
- 필요 API: `GET /api/songs/recommend`
- 필요 상태값: 공개 추천곡, 인증 여부
- 주요 컴포넌트: `HeroRadio`, `NeonWave`, `SongCarousel`, `AppHeader`
- 사용자 액션: 시작하기, 로그인
- 성공 시 이동: 인증 사용자 `/radio/create`, 비인증 `/login`
- 실패 시 처리: mock 추천곡 표시

### Login Page

- route: `/login`
- 사용 목적: 로그인
- 필요 API: `POST /api/auth/login`, 이후 `GET /api/users/me`
- 필요 상태값: email, password, loading, field/server error
- 주요 컴포넌트: `AuthCard`, `TextField`, `PrimaryButton`
- 사용자 액션: 로그인, 회원가입 이동
- 성공 시 이동: redirect query 또는 `/radio/create`
- 실패 시 처리: token 제거 없이 폼 오류 표시

### Signup Page

- route: `/signup`
- 사용 목적: 이메일 인증 및 회원가입
- 필요 API: email send/verify, signup
- 필요 상태값: 단계, nickname/email/password/code, 인증 완료 여부
- 주요 컴포넌트: `SignupStepper`, `VerificationCodeInput`
- 사용자 액션: 코드 발송/검증/가입
- 성공 시 이동: `/login`
- 실패 시 처리: 단계 유지, fieldErrors 표시

### Radio Create Page

- route: `/radio/create`
- 사용 목적: 라디오 조건 입력
- 필요 API: `GET /api/preferences/me`, `POST /api/radio`
- 필요 상태값: radio form 전체, saveAsPlaylist, selectedSongs, loading
- 주요 컴포넌트: `MoodSelector`, `SituationInput`, `EraChips`, `GenreChips`, `StoryTextarea`, `RadioGenerateButton`
- 사용자 액션: 조건 선택, 생성
- 성공 시 이동: `/radio/{radioSessionId}`. 생성 응답은 store에 보관
- 실패 시 처리: 입력 유지, ErrorResponse 표시

### Radio Result Page

- route: `/radio/:id`
- 사용 목적: DJ 멘트, 추천곡, 좋아요, 저장
- 필요 API: 직접 진입 시 `GET /api/radio/{id}`, likes, playlists
- 필요 상태값: session, createResult, likedSongIds, activeSong, TTS 상태
- 주요 컴포넌트: `DjMentCard`, `RadioPlayer`, `RecommendedSongCard`, `LikeButton`, `SavePlaylistModal`
- 사용자 액션: 재생, 좋아요, 수동 저장, 플레이리스트 이동
- 성공 시 이동: `/playlists/{playlistId}` 또는 유지
- 실패 시 처리: TTS 브라우저 fallback, 저장/좋아요 rollback

### Playlist Page

- route: `/playlists`
- 사용 목적: 내 플레이리스트 목록
- 필요 API: `GET /api/playlists`, `POST /api/playlists`
- 필요 상태값: playlists, create modal 상태
- 주요 컴포넌트: `PlaylistGrid`, `PlaylistCard`, `CreatePlaylistModal`
- 사용자 액션: 생성, 상세 이동, 삭제
- 성공 시 이동: `/playlists/:id`
- 실패 시 처리: toast 및 기존 목록 유지

### Playlist Detail Page

- route: `/playlists/:id`
- 사용 목적: 곡 목록 재생/관리
- 필요 API: playlist 상세, item 추가/삭제
- 필요 상태값: selectedPlaylist, activeTrack
- 주요 컴포넌트: `PlaylistHero`, `TrackList`, `TrackRow`, `MusicPlayerBar`
- 사용자 액션: 재생, 항목 삭제, 곡 추가
- 성공 시 이동: 유지
- 실패 시 처리: optimistic 상태 복구

### My Page

- route: `/me`
- 사용 목적: 사용자/취향/좋아요/저장곡/라디오 이력
- 필요 API: users/me, preferences/me, likes/songs, usersongs/me, radio/me
- 필요 상태값: user, preference, likedSongs, savedSongs, radioSessions
- 주요 컴포넌트: `ProfileCard`, `PreferencePanel`, `LibraryTabs`, `RadioHistoryCard`
- 사용자 액션: 프로필/취향 수정, 과거 라디오 열기, 로그아웃
- 성공 시 이동: 탭 또는 해당 상세
- 실패 시 처리: 탭별 독립 오류 표시

## 12. Vue 컴포넌트 구조

```text
FE/
  package.json
  vite.config.js
  .env.development
  src/
    main.js
    App.vue
    router/
      index.js
    api/
      axios.js
      authApi.js
      userApi.js
      radioApi.js
      playlistApi.js
      likeApi.js
      songApi.js
      preferenceApi.js
      userSongApi.js
      exploreApi.js
    pages/
      LandingPage.vue
      LoginPage.vue
      SignupPage.vue
      RadioCreatePage.vue
      RadioResultPage.vue
      PlaylistPage.vue
      PlaylistDetailPage.vue
      MyPage.vue
      NotFoundPage.vue
    components/
      common/
        AppHeader.vue
        AppShell.vue
        BaseButton.vue
        BaseModal.vue
        ErrorNotice.vue
        LoadingOverlay.vue
        ToastHost.vue
      auth/
        AuthCard.vue
        SignupStepper.vue
      radio/
        MoodSelector.vue
        RadioCreateForm.vue
        DjMentCard.vue
        RadioPlayer.vue
        RadioHistoryCard.vue
      song/
        SongCard.vue
        TrackRow.vue
        LikeButton.vue
      playlist/
        PlaylistCard.vue
        PlaylistGrid.vue
        CreatePlaylistModal.vue
        SavePlaylistModal.vue
      preference/
        PreferencePanel.vue
    stores/                     Pinia 사용 시
      auth.js
      radio.js
      playlist.js
      ui.js
    composables/                Pinia 미사용 또는 공통 로직
      useAuth.js
      useRadioPlayer.js
      useToast.js
    mocks/
      auth.js
      radio.js
      playlists.js
      songs.js
    assets/styles/
      tokens.css
      global.css
      utilities.css
```

Vite dev server는 기본 `5173`을 쓰면 현재 CORS 설정과 맞는다. API base URL은 `.env.development`의 `VITE_API_BASE_URL=http://localhost:8080`으로 분리한다.

## 13. axios API 모듈 구조

### `api/axios.js`

역할: base URL, timeout, JSON header, access token 주입, 401 refresh 단일화, 공통 ErrorResponse 정규화.

```javascript
import axios from 'axios'

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080',
  timeout: 30000
})

api.interceptors.request.use((config) => {
  const token = sessionStorage.getItem('revibek.accessToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})
```

### `api/authApi.js`

```javascript
export const sendEmailCode = (payload) => api.post('/api/auth/email/send', payload).then(r => r.data)
export const verifyEmailCode = (payload) => api.post('/api/auth/email/verify', payload).then(r => r.data)
export const signup = (payload) => api.post('/api/auth/signup', payload).then(r => r.data)
export const login = (payload) => api.post('/api/auth/login', payload).then(r => r.data)
export const refresh = (refreshToken) => api.post('/api/auth/refresh', { refreshToken }).then(r => r.data)
export const logout = (refreshToken) => api.post('/api/auth/logout', { refreshToken }).then(r => r.data)
export const getMe = () => api.get('/api/users/me').then(r => r.data)
```

### `api/radioApi.js`

```javascript
export const createRadio = (payload) => api.post('/api/radio', payload).then(r => r.data)
export const getRadio = (id) => api.get(`/api/radio/${id}`).then(r => r.data)
export const getMyRadios = () => api.get('/api/radio/me').then(r => r.data)
```

### `api/playlistApi.js`

```javascript
export const createPlaylist = (payload) => api.post('/api/playlists', payload).then(r => r.data)
export const getPlaylists = () => api.get('/api/playlists').then(r => r.data)
export const getPlaylist = (id) => api.get(`/api/playlists/${id}`).then(r => r.data)
export const addPlaylistItem = (id, songId) => api.post(`/api/playlists/${id}/items`, { songId }).then(r => r.data)
export const deletePlaylistItem = (id, itemId) => api.delete(`/api/playlists/${id}/items/${itemId}`).then(r => r.data)
export const deletePlaylist = (id) => api.delete(`/api/playlists/${id}`).then(r => r.data)
```

### `api/likeApi.js`

```javascript
export const addLike = (songId) => api.post('/api/likes', { songId }).then(r => r.data)
export const removeLike = (songId) => api.delete(`/api/likes/${songId}`).then(r => r.data)
export const getLikeStatus = (songId) => api.get(`/api/likes/${songId}/status`).then(r => r.data)
export const getLikedSongs = () => api.get('/api/likes/songs').then(r => r.data)
export const getLikeCount = (songId) => api.get(`/api/likes/${songId}/count`).then(r => r.data)
```

### `api/songApi.js`

```javascript
export const getSongs = () => api.get('/api/songs').then(r => r.data)
export const getSong = (id) => api.get(`/api/songs/${id}`).then(r => r.data)
export const searchSong = (title) => api.get('/api/songs/search', { params: { title } }).then(r => r.data)
export const getSongsByGenre = (genre) => api.get('/api/songs/genre', { params: { genre } }).then(r => r.data)
export const getRecommendedSongs = () => api.get('/api/songs/recommend').then(r => r.data)
```

추가 모듈에는 `preferenceApi`, `userSongApi`, `exploreApi`를 분리한다. 운영용 analysis/youtube/qdrant/ai API는 일반 사용자 번들에서 직접 노출하지 않는 것이 적절하다.

## 14. API별 요청/응답 JSON 예시

### 회원가입/인증/내 정보

```json
// POST /api/auth/signup
{
  "nickname": "네온DJ",
  "email": "user@example.com",
  "password": "password123"
}
```

성공 응답은 JSON 객체가 아니라 문자열이다.

```json
"회원가입 완료"
```

```json
// GET /api/users/me
{
  "id": "8e3cbba1-0e80-4ab1-a915-9b48e87ec1e2",
  "nickname": "네온DJ",
  "email": "user@example.com",
  "provider": "local"
}
```

### 라디오 생성

```json
// POST /api/radio
{
  "title": "비 오는 밤의 2세대 감성",
  "mood": "외로움",
  "situation": "퇴근 후 비 오는 버스를 타고 있어요",
  "desiredMood": "위로",
  "story": "오늘은 오래된 친구가 생각나요.",
  "era": "2세대",
  "genre": "발라드",
  "videoType": "AI cover",
  "preferredArtist": "태연",
  "excludedKeywords": "댄스,신나는",
  "saveAsPlaylist": true,
  "selectedSongs": []
}
```

```json
{
  "radioSessionId": "radio-uuid",
  "userId": "user-uuid",
  "mood": "외로움",
  "story": "오늘은 오래된 친구가 생각나요.",
  "era": "2세대",
  "genre": "발라드",
  "situation": "퇴근 후 비 오는 버스를 타고 있어요",
  "desiredMood": "위로",
  "videoType": "AI cover",
  "preferredArtist": "태연",
  "excludedKeywords": "댄스,신나는",
  "djMent": "오늘 밤, 조용히 마음을 감싸 줄 노래들을 준비했어요.",
  "playlistId": "playlist-uuid",
  "title": "비 오는 밤의 2세대 감성",
  "djComment": null,
  "recommendationSource": "DB_MOOD_ERA_GENRE",
  "tts": {
    "mode": "BROWSER_TTS",
    "text": "오늘 밤, 조용히 마음을 감싸 줄 노래들을 준비했어요.",
    "audioUrl": null
  },
  "songs": null,
  "recommendedSongs": [
    {
      "songId": "song-001",
      "title": "그리운 밤",
      "artist": "Revibe Artist",
      "era": "00s",
      "genre": "발라드",
      "youtubeUrl": "https://www.youtube.com/watch?v=example",
      "youtubeId": "example",
      "score": 91.2,
      "reason": "현재 감정과 원하는 위로 분위기에 어울리는 곡"
    }
  ]
}
```

### 라디오 상세 조회

```json
// GET /api/radio/radio-uuid
{
  "id": "radio-uuid",
  "mood": "외로움",
  "story": "오늘은 오래된 친구가 생각나요.",
  "era": "2세대",
  "genre": "발라드",
  "situation": "퇴근 후 비 오는 버스를 타고 있어요",
  "desiredMood": "위로",
  "videoType": "AI cover",
  "preferredArtist": "태연",
  "excludedKeywords": "댄스,신나는",
  "recommendationSource": "DB_MOOD_ERA_GENRE",
  "djMent": "오늘 밤, 조용히 마음을 감싸 줄 노래들을 준비했어요.",
  "comfortText": null,
  "novelExcerpt": null,
  "createdAt": "2026-06-13T21:30:00",
  "songs": [
    {
      "songId": "song-001",
      "title": "그리운 밤",
      "artist": "Revibe Artist",
      "orderNum": 1,
      "reason": "현재 감정과 잘 어울리는 곡"
    }
  ]
}
```

### 좋아요

```json
// POST /api/likes
{ "songId": "song-001" }
```

```json
// POST, DELETE, status 공통 형태
{ "songId": "song-001", "liked": true, "likeCount": 42 }
```

```json
// DELETE /api/likes/song-001 응답 예시
{ "songId": "song-001", "liked": false, "likeCount": 41 }
```

```json
// GET /api/likes/song-001/status
{ "songId": "song-001", "liked": true, "likeCount": 42 }
```

### 플레이리스트 생성/목록/상세/곡 추가

```json
// POST /api/playlists
{ "name": "새벽 네온 라디오", "moodTag": "위로", "isPublic": false }
```

```json
{
  "id": "playlist-uuid",
  "userId": "user-uuid",
  "name": "새벽 네온 라디오",
  "moodTag": "위로",
  "isPublic": false,
  "createdAt": "2026-06-13T21:40:00",
  "items": []
}
```

```json
// GET /api/playlists
[
  {
    "id": "playlist-uuid",
    "userId": "user-uuid",
    "name": "새벽 네온 라디오",
    "moodTag": "위로",
    "isPublic": false,
    "createdAt": "2026-06-13T21:40:00",
    "items": null
  }
]
```

```json
// POST /api/playlists/playlist-uuid/items
{ "songId": "song-001" }
```

```json
// GET /api/playlists/playlist-uuid
{
  "id": "playlist-uuid",
  "userId": "user-uuid",
  "name": "새벽 네온 라디오",
  "moodTag": "위로",
  "isPublic": false,
  "createdAt": "2026-06-13T21:40:00",
  "items": [
    {
      "id": "playlist-item-uuid",
      "playlistId": "playlist-uuid",
      "songId": "song-001",
      "title": "그리운 밤",
      "artist": "Revibe Artist",
      "genre": "발라드",
      "era": "00s",
      "youtubeUrl": "https://www.youtube.com/watch?v=example",
      "youtubeId": "example",
      "orderNum": 1,
      "addedAt": "2026-06-13T21:41:00"
    }
  ]
}
```

### 라디오 결과를 플레이리스트로 저장하는 흐름

자동 흐름이 구현되어 있으므로 우선 사용한다.

```javascript
const result = await createRadio({
  ...radioForm,
  saveAsPlaylist: true,
  selectedSongs: selectedRecommendations
})

if (result.playlistId) {
  router.push(`/playlists/${result.playlistId}`)
}
```

수동 흐름은 기존 플레이리스트를 선택할 때 사용한다.

```javascript
const playlist = await createPlaylist({ name, moodTag: radio.mood, isPublic: false })
await Promise.all(recommendedSongs.map(song => addPlaylistItem(playlist.id, song.songId)))
```

`Promise.all`은 일부 실패 시 전체 성공처럼 보일 수 있으므로 실제 UI에서는 `Promise.allSettled`로 성공/중복/실패 개수를 표시하는 편이 좋다.

### fallback 응답 예시

```json
// GET /api/qdrant/similar/song-001
{
  "source": "fallback",
  "message": "Qdrant unavailable or no vector results. Using DB score fallback.",
  "results": []
}
```

```json
// POST /api/analysis/song-001
{
  "youtube_video_id": "example",
  "title": "그리운 밤",
  "status": "MOCK",
  "source": "fallback",
  "message": "FastAPI is disabled. Using mock analysis result.",
  "audio_path": null,
  "duration_seconds": 210,
  "bpm": 120.0,
  "energy": 0.6,
  "danceability": 0.6,
  "loudness": -8.0,
  "musical_key": "C",
  "musical_scale": "major"
}
```

### 공통 오류 예시

```json
{
  "timestamp": "2026-06-13T21:45:00",
  "status": 400,
  "error": "Bad Request",
  "message": "요청값 검증에 실패했습니다.",
  "path": "/api/auth/signup",
  "fieldErrors": {
    "password": "password는 8자 이상이어야 합니다."
  }
}
```

## 15. JavaScript 상태 관리 구조

### Pinia 미사용

작은 초기 프로토타입은 `reactive()` singleton composable로 구성할 수 있다.

```javascript
export const authState = reactive({
  accessToken: null,
  refreshToken: null,
  user: null,
  isAuthenticated: false
})

export const radioState = reactive({
  mood: '',
  story: '',
  era: '',
  genre: '',
  radioSessionId: null,
  djMent: '',
  recommendedSongs: [],
  playlistId: null
})

export const playlistState = reactive({
  playlists: [],
  selectedPlaylist: null
})

export const uiState = reactive({
  loading: false,
  error: null,
  toast: null
})
```

장점은 의존성이 적다는 점이고, 단점은 action/devtools/SSR 규칙이 약하고 상태 변경 위치가 흩어지기 쉽다는 점이다.

### Pinia 사용 권장안

- `authStore`: `login`, `restoreSession`, `refreshSession`, `logout`, `fetchMe`
- `radioStore`: form, 마지막 생성 응답, `createRadio`, `fetchRadio`, view model 정규화
- `playlistStore`: 목록/상세, 생성/곡 추가/삭제
- `uiStore`: 전역 loading count, toast queue, modal
- likes는 카드가 많은 화면을 위해 `likedSongIds: Set` 또는 직렬화 가능한 객체 map으로 관리

서버 상태 캐시가 복잡해지면 Pinia만으로 수동 캐시하기보다 Vue Query 도입을 검토할 수 있다.

## 16. CSS 디자인 방향

### 전체 색감과 배경

- 기본 배경: 거의 검정에 가까운 남색 `#070711`, 표면 `#111124`, 카드 `#18182d`
- 주조색: 네온 마젠타 `#ff3cac`, 보라 `#784ba0`, 청록 `#2bdfdb`
- 보조색: 향수를 표현하는 따뜻한 코랄/앰버 `#ffb86b`
- 배경은 radial gradient와 낮은 opacity의 waveform/grid를 사용하고 텍스트 가독성을 우선한다.

### 버튼/카드

- Primary 버튼: 마젠타-보라 gradient, 12~16px radius, hover 시 약한 glow
- Secondary 버튼: 반투명 dark surface + 1px 보라 border
- 카드: 어두운 반투명 표면, 얇은 gradient border, 16~20px radius
- 좋아요/재생 상태만 강한 네온을 사용해 모든 요소가 빛나는 과도한 UI를 피한다.

### 라디오 생성 화면

- 데스크톱은 좌측 입력 stepper, 우측 실시간 “Now tuning” 미리보기
- mood/era/genre는 chip 선택, story는 큰 textarea
- 생성 버튼 주변에 라디오 주파수 다이얼 또는 파형 애니메이션
- 생성 중에는 단계 문구: “감정을 읽는 중”, “곡을 고르는 중”, “DJ 멘트를 준비하는 중”

### 추천 결과/플레이리스트

- 상단 DJ 멘트 카드와 재생 버튼, 하단 추천곡 카드/track list
- 추천곡 카드에는 thumbnail, title, artist, reason, genre/era tag, YouTube, like, save 버튼
- playlist 상세는 앨범형 hero + 밀도 높은 track row + 하단 고정 player bar
- fallback은 오류처럼 강하게 보이지 않도록 작은 `Browser voice`, `DB recommendation` 배지로 표시

### 모바일 반응형

- 768px 이하에서 1열, bottom navigation 사용
- 음악 player는 하단 sticky mini player
- chip 영역은 가로 스크롤
- modal은 bottom sheet
- 터치 target 최소 44px, 주요 버튼은 full width
- 긴 DJ 멘트와 사연은 접기/펼치기 제공

## 17. mock data 구조

```javascript
export const mockAuth = {
  accessToken: 'mock-access-token',
  refreshToken: 'mock-refresh-token',
  tokenType: 'Bearer',
  expiresIn: 3600000,
  user: {
    id: 'user-001',
    nickname: '네온DJ',
    email: 'user@example.com',
    provider: 'local'
  }
}

export const mockRadio = {
  radioSessionId: 'radio-001',
  mood: '외로움',
  story: '오늘은 오래된 친구가 생각나요.',
  era: '2세대',
  genre: '발라드',
  djMent: '오늘 밤 마음을 감싸 줄 노래들을 준비했어요.',
  playlistId: 'playlist-001',
  recommendationSource: 'DB_MOOD_ERA_GENRE',
  tts: { mode: 'BROWSER_TTS', text: '오늘 밤 마음을 감싸 줄 노래들을 준비했어요.', audioUrl: null },
  recommendedSongs: [
    {
      songId: 'song-001',
      title: '그리운 밤',
      artist: 'Revibe Artist',
      era: '00s',
      genre: '발라드',
      youtubeUrl: 'https://www.youtube.com/watch?v=example',
      youtubeId: 'example',
      score: 91.2,
      reason: '현재 감정과 잘 어울리는 곡'
    }
  ]
}

export const mockPlaylist = {
  id: 'playlist-001',
  userId: 'user-001',
  name: '비 오는 밤의 2세대 감성',
  moodTag: '위로',
  isPublic: false,
  createdAt: '2026-06-13T21:40:00',
  items: []
}
```

MSW(Mock Service Worker)를 사용하면 위 객체를 실제 API 경로의 mock 응답으로 연결해 axios 코드를 변경하지 않고 개발할 수 있다.

## 18. v0 또는 Vue UI 생성용 최종 프롬프트

```text
서비스명은 RevibeK입니다.

감정과 상황을 입력하면 K-POP 및 AI cover 곡을 추천하고 AI DJ 멘트를 들려주는 라디오 큐레이션 서비스의 Vue 3 프론트엔드를 생성하세요. Vite + Vue Router + JavaScript + HTML + CSS 기준이며 TypeScript는 사용하지 않습니다. API 연동 전에는 mock data로 동작하되, 모든 데이터 호출은 분리된 axios API 모듈을 통해 실제 Spring Boot API로 쉽게 교체할 수 있게 구성하세요.

디자인 톤:
- K-POP, AI cover, radio, playlist, nostalgia, neon, dark mode
- 거의 검정에 가까운 남색 배경, 마젠타/보라/청록 gradient
- 반투명 dark card, 얇은 neon border, 음악 파형과 radio tuning motif
- 과도한 glow를 피하고 높은 가독성 유지
- 데스크톱/태블릿/모바일 반응형, 모바일 하단 navigation과 sticky mini player

페이지:
1. Landing `/`: 서비스 hero, 네온 waveform, 공개 추천곡, 시작하기
2. Login `/login`: 이메일/비밀번호 로그인
3. Signup `/signup`: 이메일 코드 발송, 6자리 코드 검증, 닉네임/비밀번호 가입 stepper
4. Radio Create `/radio/create`: 감정, 상황, 원하는 감정, 사연, 세대, 장르, 영상 타입, 선호 아티스트, 제외 키워드, 플레이리스트 자동 저장 여부 입력
5. Radio Result `/radio/:id`: AI DJ 멘트, TTS 재생, 추천 이유가 있는 곡 카드 5개, YouTube 링크, 좋아요, 플레이리스트 저장
6. Playlist `/playlists`: 내 플레이리스트 card grid, 생성 modal
7. Playlist Detail `/playlists/:id`: playlist hero, track list, 곡 삭제, 하단 music player
8. My Page `/me`: 프로필, 취향, 좋아요 곡, 저장곡, 라디오 이력 tabs

핵심 사용자 흐름:
회원가입/로그인 -> 감정/상황/세대/장르 입력 -> POST /api/radio -> DJ 멘트와 추천곡 결과 -> 좋아요 -> 플레이리스트 저장 -> 내 플레이리스트 조회.
POST /api/radio에 saveAsPlaylist:true를 보내면 백엔드가 자동 저장하고 응답 playlistId를 반환합니다. playlistId가 없을 때만 POST /api/playlists 후 POST /api/playlists/{playlistId}/items로 수동 저장합니다.

주요 실제 API:
- POST /api/auth/email/send, /email/verify, /signup, /login, /refresh, /logout
- GET /api/users/me
- POST /api/radio, GET /api/radio/{id}, GET /api/radio/me
- POST/GET /api/playlists, GET /api/playlists/{id}, POST /api/playlists/{id}/items
- POST /api/likes, DELETE /api/likes/{songId}, GET /api/likes/songs
- GET /api/songs, /api/songs/search?title=, /api/songs/recommend

Authorization 헤더는 Bearer access token을 사용합니다. axios request interceptor로 token을 추가하고, 401이면 refresh token으로 한 번 재발급 후 원 요청을 재시도하세요.

컴포넌트:
AppHeader, AppShell, HeroRadio, NeonWave, AuthCard, SignupStepper, MoodSelector, EraChips, GenreChips, StoryTextarea, RadioGenerateButton, DjMentCard, RadioPlayer, SongCard, LikeButton, SavePlaylistModal, PlaylistCard, TrackRow, MusicPlayerBar, ProfileCard, PreferencePanel, ToastHost.

상태:
authState(accessToken, refreshToken, user, isAuthenticated),
radioState(form, radioSessionId, djMent, recommendedSongs, playlistId),
playlistState(playlists, selectedPlaylist),
uiState(loading, error, toast).

mock 데이터는 로그인 응답, RadioCreateResponseDto, SongDto, PlaylistDto, LikeStatusDto 구조를 사용하세요. 라디오 TTS 응답이 mode=BROWSER_TTS이고 audioUrl이 null이면 브라우저 SpeechSynthesis를 사용하는 UI 상태를 표현하세요. recommendationSource/source가 fallback이면 작은 상태 배지를 표시하세요.

Vue Single File Components로 구조화하고, 접근 가능한 label, keyboard focus, 최소 44px touch target, loading/skeleton/empty/error 상태를 모두 포함하세요.
```

## 19. 구현 시 주의사항

1. `kpop_radio_schema.sql`은 앞부분에서 `order_num`을 만든 뒤 후반부에서 `sort_order`로 변경한다. 파일을 중간까지만 실행하면 Radio Mapper가 실패하므로 전체 스키마/마이그레이션 적용 여부를 확인해야 한다.
2. 라디오 생성 응답은 `recommendedSongs`, 상세 응답은 `songs`로 구조가 다르다. 프론트 adapter가 필요하다.
3. `RadioCreateResponseDto`의 `songs`, `djComment`는 현재 service에서 채우지 않아 null이다.
4. 스키마 후반부는 `radio_sessions.title`, `playlist_id`를 추가하지만 현재 Radio Mapper insert/select는 이를 저장하거나 조회하지 않는다. 생성 응답의 `title`, `playlistId`는 받을 수 있어도 과거 라디오 상세 조회에서는 복원되지 않는다.
5. 응답 wrapper가 통일되지 않았다. preference만 `{success,message,data}`, 오류는 `ErrorResponse`, 일부 성공은 문자열이다.
6. Controller 내부에서 예외를 직접 문자열로 반환하는 song/analysis API는 공통 ErrorResponse와 다른 형태일 수 있다.
7. 모든 `/api/likes/**`는 SecurityConfig상 인증이 필요하다.
8. refresh token은 서버 메모리 저장소를 사용하므로 서버 재시작 시 기존 refresh token이 무효가 될 수 있다.
9. email verification도 메모리 기반이다.
10. Google OAuth 성공 흐름은 존재하지만 프론트 callback/token 전달 계약은 `OAuth2SuccessHandler`를 별도로 확인해 연결해야 한다.
11. YouTube, FastAPI, Qdrant, GMS, Google TTS는 기본 설정에서 비활성일 수 있다. UI는 fallback을 정상 상태의 한 종류로 다뤄야 한다.
12. `selectedSongs`에 존재하지 않는 곡 ID가 포함되면 자동 playlist 저장 시 건너뛴다.
13. playlist 목록은 `items:null`, 상세만 items를 포함할 수 있다.
14. playlist item 삭제에는 `songId`가 아니라 `items[].id`가 필요하다.
15. `GET /api/songs/search`는 목록이 아니라 단일 `SongDto`를 반환한다.
16. CORS 기본값과 맞추려면 Vue dev server를 `5173`에서 실행하는 것이 가장 간단하다.
17. `application.properties`에 GMS/TTS 설정이 중복되어 최종 환경값 확인이 필요하다.
18. 백엔드 소스와 일부 SQL/주석의 한글이 현재 콘솔에서 깨져 보인다. 실제 파일 인코딩과 DB connection의 UTF-8 설정을 배포 전에 검증해야 한다.

## 20. 최종 판단

현재 백엔드만으로 RevibeK의 Vue MVP 핵심 흐름을 구현할 수 있다. 프론트는 Vue 3 + Vite + Vue Router + axios + Pinia 조합이 적절하며, 첫 구현 우선순위는 인증, 라디오 생성/결과, 좋아요, 자동 플레이리스트 저장, 플레이리스트 상세 순서가 좋다.

가장 먼저 백엔드와 통합 확인해야 할 항목은 전체 스키마/후반 마이그레이션 적용 여부, 라디오 생성/상세의 서로 다른 곡 응답 구조, refresh token 저장 정책이다. 이 세 항목을 인지하고 adapter/fallback 중심으로 프론트를 구성하면 외부 AI/YouTube/Qdrant 서비스가 비활성인 개발 환경에서도 화면 개발과 핵심 사용자 흐름 검증이 가능하다.
