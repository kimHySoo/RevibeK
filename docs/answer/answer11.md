# RevibeK Vue 프론트엔드 설계를 위한 백엔드 구조/API/JSON 분석 결과

## 1. 전체 결론

RevibeK 백엔드는 Spring Boot(Maven) + MyBatis + MySQL 기반으로, "감정/상황 기반 K-POP AI 커버 라디오" 서비스의 핵심 도메인(인증/회원, 라디오 생성, 추천곡, 좋아요, 플레이리스트, 곡 검색, 사용자 곡 보관, 유튜브, 분석/임베딩, Qdrant 벡터 검색, AI(Claude)/TTS)이 모두 구현되어 있다.

- 인증은 JWT(Access/Refresh) 기반이며, `Authorization: Bearer {accessToken}` 헤더로 인증한다.
- 라디오 생성(`POST /api/radio`)은 감정/상황/시대/장르 등을 입력받아 추천곡 + AI DJ 멘트 + TTS 음성까지 한 번에 반환하는 핵심 API로, 프론트엔드의 메인 플로우(라디오 생성 → 추천 결과 화면)를 그대로 구현할 수 있다.
- 좋아요(`/api/likes`), 플레이리스트(`/api/playlists`), 사용자 보관곡(`/api/usersongs`) API가 모두 구현되어 있어 "추천곡 → 좋아요 → 플레이리스트 저장" 흐름을 구성할 수 있다.
- 다만 "라디오 추천 결과를 플레이리스트에 한 번에 담는" 전용 API는 없으며, 프론트엔드에서는 `POST /api/playlists`(생성) 후 `POST /api/playlists/{playlistId}/items`(곡 추가)를 곡별로 반복 호출해야 한다.
- FE(Vue) 프론트엔드 폴더는 현재 프로젝트에 존재하지 않는다(즉, 백엔드만 존재하는 상태). 이 문서가 Vue 프론트엔드 설계의 출발점이 된다.
- CORS는 `application.properties`의 `app.cors.allowed-origins`로 `http://localhost:3000`, `http://localhost:5173` 등 로컬 개발 서버를 허용하도록 이미 구성되어 있다.

## 2. 백엔드 파일 구조 분석

### 2.1 프로젝트 루트

실제 Maven 프로젝트 루트는 `c:\pjt\RevibeK\RevibeK` (즉 `RevibeK/RevibeK`)이며, 소스는 `RevibeK/RevibeK/src/main/java/com/ssafy/revibek` 아래에 도메인별 패키지로 구성되어 있다. 빌드 산출물(`bin/`, `target/`)은 분석 대상에서 제외했다.

별도로 `RevibeK/RevibeK_AI`라는 FastAPI(Python) 기반 AI 분석 서버가 존재하며, Spring Boot 백엔드가 `analysis`, `embedding`, `qdrant` 도메인을 통해 이 FastAPI 서버와 통신한다(설정값 `fastapi.host=http://localhost:8000`).

### 2.2 패키지 구조 (com.ssafy.revibek 하위)

```
com.ssafy.revibek
├── RevibeKApplication.java
├── ai/                     # Claude(GMS) 챗봇 + Google TTS
│   ├── controller/ (AiController, GoogleTtsController)
│   ├── dao/ (GoogleTtsDao, GoogleTtsRestDao)
│   ├── dto/ (AiChatRequestDto, AiChatResponseDto, ChatTtsRequestDto, ChatTtsResponseDto,
│   │         TtsPreset, TtsSynthesizeRequestDto/ResponseDto, TtsVoiceResponseDto)
│   │   └── external/ (ClaudeMessageRequestDto/ResponseDto, GoogleTtsSynthesize*, GoogleTtsVoicesResponseDto)
│   └── service/ (ClaudeGmsService, GmsCreditBudgetTracker, GoogleTtsService)
├── analysis/               # FastAPI 음악 분석 연동
│   ├── client/ (FastApiClient)
│   ├── controller/ (AnalysisController)
│   ├── dto/ (AnalyzeRequestDto, AnalyzeResponseDto, RawVideoDto)
│   ├── mapper/ (RawVideoMapper)
│   └── service/ (AnalysisJsonSyncService, AnalysisService, AnalysisServiceImpl)
├── auth/                   # JWT, OAuth2
│   ├── JwtAuthenticationFilter.java
│   ├── JwtTokenProvider.java
│   ├── OAuth2SuccessHandler.java
│   ├── RefreshTokenStore.java
│   └── dto/ (AuthTokenResponseDto, LogoutRequestDto, RefreshTokenRequestDto)
├── common/
│   ├── dto/ (ApiResponseDto, ErrorResponse)
│   └── exception/ (GlobalExceptionHandler)
├── config/                 # SecurityConfig, QdrantConfig, RestTemplateConfig, ObjectMapperConfig, PasswordEncoderConfig, FastApiLauncher
├── embedding/              # 곡 텍스트 임베딩 생성/동기화
│   ├── controller/ (EmbeddingController)
│   └── service/ (EmbeddingQdrantSyncService, SongEmbeddingService, SongEmbeddingServiceImpl)
├── explore/                # YouTube URL 기반 유사곡 탐색
│   ├── controller/ (ExploreController)
│   ├── dto/ (ExploreResponseDto)
│   └── service/ (ExploreService)
├── like/                   # 좋아요
│   ├── controller/ (LikeController)
│   ├── dto/ (LikeDto, LikeStatusDto)
│   ├── mapper/ (LikeMapper + LikeMapper.xml)
│   └── service/ (LikeService)
├── playlist/               # 플레이리스트
│   ├── controller/ (PlaylistController)
│   ├── dto/ (PlaylistDto, PlaylistItemDto)
│   ├── mapper/ (PlaylistMapper + PlaylistMapper.xml)
│   └── service/ (PlaylistService)
├── preference/             # 사용자 음악 취향(온보딩)
│   ├── controller/ (PreferenceController)
│   ├── dto/ (UserPreferenceDto, UserPreferenceRequestDto)
│   ├── mapper/ (PreferenceMapper + PreferenceMapper.xml)
│   └── service/ (PreferenceService)
├── qdrant/                 # 벡터 DB(Qdrant) 연동
│   ├── QdrantController.java, QdrantService.java, SongVectorUtil.java
│   └── dto/ (VectorSearchResponseDto)
├── radio/                  # 라디오 세션(핵심 도메인)
│   ├── ai/ (AiDjMentService, AiDjPromptBuilder, GmsClient)
│   ├── controller/ (RadioController)
│   ├── dto/ (RadioCreateRequestDto/ResponseDto, RadioRequestDto, RadioResponseDto,
│   │         RecommendedSongResponseDto, TtsFallbackResponseDto)
│   ├── mapper/ (RadioMapper + RadioMapper.xml)
│   └── service/ (RadioService)
├── song/                   # 곡
│   ├── controller/ (SongController)
│   ├── dto/ (SongDto)
│   ├── mapper/ (SongDao + SongMapper.xml)
│   └── service/ (SongService, SongServiceImpl, TitleArtistParsingService)
├── spotify/                # Spotify 연동(era/generation 채우기)
│   └── service/ (SpotifyService, SpotifyServiceImpl)
├── tts/                    # 라디오용 TTS (DJ멘트 음성화)
│   └── TtsClient.java, TtsResponseDto.java, TtsService.java
├── user/                   # 회원/인증
│   ├── controller/ (AuthController, UserController)
│   ├── dto/ (EmailVerificationCheckRequestDto, EmailVerificationSendRequestDto, UserAuthDto,
│   │         UserLoginRequestDto, UserRegisterRequestDto, UserResponseDto, UserUpdateRequestDto)
│   ├── mapper/ (UserMapper + UserMapper.xml)
│   └── service/ (AuthService, EmailVerificationService, UserService)
├── usersong/               # 사용자별 저장곡/평점/재생 이력
│   ├── controller/ (UserSongController)
│   ├── dto/ (UserSongRequestDto, UserSongResponseDto)
│   ├── mapper/ (UserSongMapper + UserSongMapper.xml)
│   └── service/ (UserSongService)
└── youtube/                # 유튜브 채널/영상
    ├── controller/ (YoutubeController)
    ├── dto/ (YoutubeChannelDto, YoutubeFallbackResponseDto, YoutubeVideoDto,
    │         YoutubeVideoResponseDto, YoutubeVideoStatsDto)
    ├── mapper/ (YoutubeMapper + YoutubeMapper.xml)
    └── service/ (YoutubeService, YoutubeServiceImpl)
```

### 2.3 MyBatis 매퍼 XML 위치

`src/main/resources/mapper/` 아래에 도메인별 디렉터리로 위치:
- `mapper/analysis/RawVideoMapper.xml`
- `mapper/like/LikeMapper.xml`
- `mapper/playlist/PlaylistMapper.xml`
- `mapper/preference/PreferenceMapper.xml`
- `mapper/radio/RadioMapper.xml`
- `mapper/song/SongMapper.xml`
- `mapper/user/UserMapper.xml`
- `mapper/usersong/UserSongMapper.xml`
- `mapper/youtube/YoutubeMapper.xml`

`application.properties`에서 `mybatis.mapper-locations=classpath*:mapper/**/*.xml`, `mybatis.configuration.map-underscore-to-camel-case=true`로 설정되어 있어 DB 컬럼(snake_case)이 DTO 필드(camelCase)로 자동 매핑된다.

### 2.4 application.properties 주요 설정 (값은 일반화하여 설명, 시크릿 비노출)

- **앱 이름**: `RevibeK`, 프로필 `secret`을 include하여 별도 `application-secret.properties`에서 민감 정보를 분리 관리.
- **DB**: MySQL, DB명 `kpop_radio`, 기본 접속 정보는 환경변수(`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`)로 오버라이드 가능.
- **MyBatis**: 매퍼 XML 자동 스캔, snake_case ↔ camelCase 자동 매핑.
- **Security/CORS**: `app.cors.allowed-origins` 기본값에 `http://localhost:3000`, `http://localhost:5173`, `http://127.0.0.1:3000`, `http://127.0.0.1:5173` 포함 (Vue 개발 서버 포트 5173과 호환).
- **JWT**: `jwt.secret`, `jwt.access-token-expiration-ms`(기본 1시간), `jwt.refresh-token-expiration-ms`(기본 14일).
- **OAuth2 Google**: `app.oauth.google.enabled` 플래그로 활성화 여부 제어(기본 false). 활성화 시 `/auth/google/callback` 콜백 경로 사용.
- **이메일 인증**: `app.email.verification.mode`(기본 `mock`)와 `app.email.verification.mock-code`(기본 `123456`)로, 개발 단계에서는 실제 메일 발송 없이 고정 코드로 인증 가능.
- **GMS(Claude/OpenAI)**: Claude 기반 DJ 멘트/챗봇 생성, OpenAI 기반 곡 제목/아티스트 분리 및 임베딩.
- **Spotify**: 곡 발매연도 기반 era/generation 보강(기본 비활성화).
- **Google Cloud TTS**: DJ 멘트 음성 합성(기본 비활성화, `tts.enabled=false`).
- **YouTube**: YouTube Data API 연동, fallback 모드는 `db`(API 키 없을 때 DB 데이터로 대체).
- **Qdrant/Vector**: 벡터 검색 활성화 여부, 컬렉션명 등(기본 `qdrant.enabled=false`).
- **FastAPI**: RevibeK_AI(Python) 서버 연동 주소(`fastapi.host=http://localhost:8000`), 기본 활성화.

### 2.5 기존 docs/ 및 FE/ 폴더 상태

- `docs/answer/` 디렉터리는 분석 시작 시점에 비어 있었다(이번 문서가 최초 생성).
- 프로젝트 루트(`c:\pjt\RevibeK`)에 `FE/` 폴더는 존재하지 않는다. 즉, Vue 프론트엔드는 아직 시작되지 않은 상태이며 이 문서를 기준으로 신규 생성해야 한다.

## 3. 주요 도메인 구조

**도메인명**: auth (인증)
**역할**: 회원가입, 이메일 인증, 로그인, 토큰 재발급, 로그아웃
**Controller**: `AuthController` (`/api/auth/**`)
**Service**: `AuthService`, `EmailVerificationService`
**DTO**: `UserRegisterRequestDto`, `UserLoginRequestDto`, `EmailVerificationSendRequestDto`, `EmailVerificationCheckRequestDto`, `AuthTokenResponseDto`, `RefreshTokenRequestDto`, `LogoutRequestDto`, `UserAuthDto`
**Mapper**: `UserMapper`
**XML Mapper**: `mapper/user/UserMapper.xml`
**관련 DB 테이블**: `users`
**프론트엔드에서 필요한 기능**: 회원가입 폼, 이메일 인증코드 발송/검증 폼, 로그인 폼, accessToken/refreshToken 저장 및 자동 재발급, 로그아웃 버튼

---

**도메인명**: user (사용자)
**역할**: 내 정보 조회/수정/삭제
**Controller**: `UserController` (`/api/users/**`)
**Service**: `UserService`
**DTO**: `UserResponseDto`, `UserUpdateRequestDto`
**Mapper**: `UserMapper`
**XML Mapper**: `mapper/user/UserMapper.xml`
**관련 DB 테이블**: `users`
**프론트엔드에서 필요한 기능**: 마이페이지(닉네임/이메일/로그인방식 표시), 회원정보 수정 폼, 회원탈퇴 버튼

---

**도메인명**: radio (AI 라디오)
**역할**: 감정/상황/시대/장르 기반 곡 추천 + AI DJ 멘트 생성 + TTS, 라디오 세션 이력 조회
**Controller**: `RadioController` (`/api/radio/**`)
**Service**: `RadioService`, `AiDjMentService`, `AiDjPromptBuilder`, `GmsClient`
**DTO**: `RadioCreateRequestDto`, `RadioCreateResponseDto`, `RadioRequestDto`, `RadioResponseDto`(+내부 `RadioSongDto`), `RecommendedSongResponseDto`, `TtsFallbackResponseDto`
**Mapper**: `RadioMapper`
**XML Mapper**: `mapper/radio/RadioMapper.xml`
**관련 DB 테이블**: `radio_sessions`, `radio_recommendations`, `songs`, `user_preferences`(추천 보조)
**프론트엔드에서 필요한 기능**: 라디오 생성 입력 폼(mood/story/era/genre/situation/desiredMood/videoType/preferredArtist/excludedKeywords), 생성 결과 화면(추천곡 리스트 + DJ멘트 + TTS 오디오 재생), 내 라디오 세션 히스토리 목록/상세

---

**도메인명**: playlist (플레이리스트)
**역할**: 사용자 플레이리스트 생성/조회/곡 추가/삭제
**Controller**: `PlaylistController` (`/api/playlists/**`)
**Service**: `PlaylistService`
**DTO**: `PlaylistDto`, `PlaylistItemDto`
**Mapper**: `PlaylistMapper`
**XML Mapper**: `mapper/playlist/PlaylistMapper.xml`
**관련 DB 테이블**: `playlists`, `playlist_songs`
**프론트엔드에서 필요한 기능**: 플레이리스트 목록/생성/상세 화면, 추천곡을 플레이리스트에 추가하는 버튼/모달, 플레이리스트 곡 삭제, 플레이리스트 삭제

---

**도메인명**: like (좋아요)
**역할**: 곡에 대한 좋아요 등록/취소/상태/카운트, 내가 좋아요한 곡 목록
**Controller**: `LikeController` (`/api/likes/**`)
**Service**: `LikeService`
**DTO**: `LikeDto`, `LikeStatusDto`
**Mapper**: `LikeMapper`
**XML Mapper**: `mapper/like/LikeMapper.xml`
**관련 DB 테이블**: `song_likes`, `songs`
**프론트엔드에서 필요한 기능**: 곡 카드/플레이어의 좋아요 토글 버튼(하트 아이콘), 좋아요 수 표시, "내가 좋아요한 곡" 목록 화면

---

**도메인명**: song (곡)
**역할**: 곡 등록/조회/검색/장르별 조회/추천/수정/삭제, 제목·아티스트 파싱, era/generation 보강
**Controller**: `SongController` (`/api/songs/**`)
**Service**: `SongService`/`SongServiceImpl`, `TitleArtistParsingService`
**DTO**: `SongDto`
**Mapper**: `SongDao`
**XML Mapper**: `mapper/song/SongMapper.xml`
**관련 DB 테이블**: `songs`, `score_logs`
**프론트엔드에서 필요한 기능**: 곡 검색창, 곡 상세 카드(제목/아티스트/장르/연대/유튜브 링크/점수), 추천곡 리스트(`/recommend`)를 활용한 홈/Explore 화면

---

**도메인명**: usersong (사용자 저장곡)
**역할**: 곡 저장/저장목록 조회/별점 등록/재생수 증가/저장 취소
**Controller**: `UserSongController` (`/api/usersongs/**`)
**Service**: `UserSongService`
**DTO**: `UserSongRequestDto`, `UserSongResponseDto`
**Mapper**: `UserSongMapper`
**XML Mapper**: `mapper/usersong/UserSongMapper.xml`
**관련 DB 테이블**: `user_songs`
**프론트엔드에서 필요한 기능**: "내 보관함" 화면(저장곡 목록), 별점 입력 UI(1~5), 재생 시 재생수 증가 호출, 저장 취소 버튼

---

**도메인명**: youtube (유튜브)
**역할**: 유튜브 채널 URL 등록 및 채널 정보/업로드 영상 수집(관리자/배치성 기능에 가까움)
**Controller**: `YoutubeController` (`/api/youtube/**`)
**Service**: `YoutubeService`/`YoutubeServiceImpl`
**DTO**: `YoutubeChannelDto`, `YoutubeFallbackResponseDto`, `YoutubeVideoDto`, `YoutubeVideoResponseDto`, `YoutubeVideoStatsDto`
**Mapper**: `YoutubeMapper`
**XML Mapper**: `mapper/youtube/YoutubeMapper.xml`
**관련 DB 테이블**: `youtube_channels`, `youtube_videos_raw`
**프론트엔드에서 필요한 기능**: 일반 사용자 화면에서는 직접 노출 가능성 낮음(운영/관리자 도구용). 단, 추천곡의 `youtubeUrl`/`youtubeId`를 통한 영상 임베드 플레이어는 라디오/플레이리스트 화면에서 필수

---

**도메인명**: analysis (음악 분석)
**역할**: FastAPI(RevibeK_AI) 연동 음악 분석(단건/배치), 분석 결과 JSON DB 동기화, 유튜브 통계 보강
**Controller**: `AnalysisController` (`/api/analysis/**`)
**Service**: `AnalysisService`/`AnalysisServiceImpl`, `AnalysisJsonSyncService`
**DTO**: `AnalyzeRequestDto`, `AnalyzeResponseDto`, `RawVideoDto`
**Mapper**: `RawVideoMapper`
**XML Mapper**: `mapper/analysis/RawVideoMapper.xml`
**관련 DB 테이블**: `songs`, `youtube_videos_raw`
**프론트엔드에서 필요한 기능**: 일반 사용자 플로우에는 직접 노출되지 않음(운영자용 배치 트리거). 분석 결과(bpm, energy, danceability 등)는 `SongDto`를 통해 곡 상세 정보로 표시 가능

---

**도메인명**: explore (탐색)
**역할**: YouTube URL 입력 → 해당 곡 분석/조회 → 유사곡 추천
**Controller**: `ExploreController` (`/api/explore`)
**Service**: `ExploreService`
**DTO**: `ExploreResponseDto`(song, similar, isNew)
**Mapper**: 없음(내부적으로 SongService/Qdrant 등 활용)
**XML Mapper**: 없음
**관련 DB 테이블**: `songs`
**프론트엔드에서 필요한 기능**: "유튜브 링크로 비슷한 곡 찾기" 입력창 + 결과(원곡 정보 + 유사곡 리스트) 화면

---

**도메인명**: qdrant (벡터 검색)
**역할**: 곡 벡터 임베딩 Qdrant 적재, 특정 곡 기준 유사곡 벡터 검색(+DB fallback)
**Controller**: `QdrantController` (`/api/qdrant/**`)
**Service**: `QdrantService`, `SongVectorUtil`
**DTO**: `VectorSearchResponseDto`(source, message, results)
**Mapper**: 없음(SongService 재사용)
**XML Mapper**: 없음
**관련 DB 테이블**: `songs` (Qdrant 자체는 별도 벡터DB)
**프론트엔드에서 필요한 기능**: 곡 상세/플레이어 화면에서 "이 곡과 비슷한 곡" 추천 섹션에 `/api/qdrant/similar/{songId}` 결과 사용 가능(필수는 아님, 향상 기능)

---

**도메인명**: ai (Claude 챗봇 / TTS)
**역할**: Claude(GMS) 기반 텍스트 생성(`/chat`), 텍스트+TTS 동시 생성(`/chat-tts`), Google TTS 음성 합성/목소리 목록
**Controller**: `AiController`(`/api/ai/**`), `GoogleTtsController`(`/api/ai/tts/**`)
**Service**: `ClaudeGmsService`, `GmsCreditBudgetTracker`, `GoogleTtsService`
**DTO**: `AiChatRequestDto`, `AiChatResponseDto`, `ChatTtsRequestDto`, `ChatTtsResponseDto`, `TtsSynthesizeRequestDto/ResponseDto`, `TtsVoiceResponseDto`, `TtsPreset`, 그리고 `external.*`(Claude/Google TTS 실제 API 연동 DTO)
**Mapper**: 없음(외부 API 연동)
**XML Mapper**: 없음
**관련 DB 테이블**: 없음(직접적인 테이블 없음, 라디오 생성 시 `radio_sessions.dj_ment`에 결과 저장)
**프론트엔드에서 필요한 기능**: 라디오 생성 화면에서 DJ 멘트(텍스트) 표시 + TTS 오디오 플레이어. 별도 AI 챗봇 UI를 만들 경우 `/api/ai/chat` 활용 가능(선택)

---

**도메인명**: tts (라디오 TTS)
**역할**: `radio` 도메인에서 DJ 멘트를 음성으로 변환(GoogleTtsService 래핑), 결과를 `TtsFallbackResponseDto`로 라디오 응답에 포함
**Controller**: 없음(별도 컨트롤러 없이 `RadioService`에서 내부 호출)
**Service**: `TtsService`, `TtsClient`
**DTO**: `TtsResponseDto`
**Mapper**: 없음
**XML Mapper**: 없음
**관련 DB 테이블**: 없음
**프론트엔드에서 필요한 기능**: 라디오 생성 응답의 `tts.audioUrl` 또는 `tts.audioContentBase64`(모드에 따라 다름)를 오디오 플레이어로 재생. `tts.mode`가 fallback인 경우 텍스트만 표시

---

**도메인명**: preference (사용자 음악 취향, 보조 도메인)
**역할**: 온보딩에서 선호 세대/분위기/아티스트/장르/영상 타입, 제외 장르/키워드 저장 — 라디오 추천 시 기본값으로 활용
**Controller**: `PreferenceController` (`/api/preferences/**`)
**Service**: `PreferenceService`
**DTO**: `UserPreferenceDto`, `UserPreferenceRequestDto`
**Mapper**: `PreferenceMapper`
**XML Mapper**: `mapper/preference/PreferenceMapper.xml`
**관련 DB 테이블**: `user_preferences`
**프론트엔드에서 필요한 기능**: 회원가입 후 온보딩(취향 선택) 화면, 마이페이지에서 취향 수정/삭제

## 4. Controller/API 목록

아래는 실제 컨트롤러 코드에서 확인된 API만 정리한 목록이다.

### AuthController (`/api/auth`, 인증 불필요 — SecurityConfig에서 permitAll)
- `POST /api/auth/email/send` — 이메일 인증코드 발송
- `POST /api/auth/email/verify` — 이메일 인증코드 검증
- `POST /api/auth/signup` — 회원가입
- `POST /api/auth/login` — 로그인
- `POST /api/auth/refresh` — Access Token 재발급
- `POST /api/auth/logout` — 로그아웃(리프레시 토큰 폐기)

### UserController (`/api/users`, 인증 필요)
- `GET /api/users/me` — 내 정보 조회
- `PUT /api/users/me` — 내 정보 수정
- `DELETE /api/users/me` — 회원 탈퇴

### RadioController (`/api/radio`, 인증 필요 — `/api/radio/**`)
- `POST /api/radio` — 라디오 세션 생성(추천곡+DJ멘트+TTS)
- `GET /api/radio/{id}` — 라디오 세션 단건 조회
- `GET /api/radio/me` — 내 라디오 세션 목록 조회

### PlaylistController (`/api/playlists`, 인증 필요)
- `POST /api/playlists` — 플레이리스트 생성
- `GET /api/playlists` — 내 플레이리스트 목록 조회
- `GET /api/playlists/{playlistId}` — 플레이리스트 상세(포함 곡 목록)
- `POST /api/playlists/{playlistId}/items` — 플레이리스트에 곡 추가
- `DELETE /api/playlists/{playlistId}/items/{itemId}` — 플레이리스트 곡 삭제
- `DELETE /api/playlists/{playlistId}` — 플레이리스트 삭제

### LikeController (`/api/likes`, 인증 필요)
- `POST /api/likes` — 좋아요 등록
- `DELETE /api/likes/{songId}` — 좋아요 취소
- `GET /api/likes/{songId}/status` — 좋아요 상태 조회(liked, likeCount)
- `GET /api/likes` — 내가 좋아요한 항목 목록(LikeDto 리스트)
- `GET /api/likes/songs` — 내가 좋아요한 곡 상세 목록(SongDto 리스트)
- `GET /api/likes/{songId}/count` — 곡별 좋아요 수(인증 불필요 — SecurityConfig에서 anyRequest permitAll 적용, but `/api/likes/**`는 authenticated로 지정되어 있으므로 실제로는 인증 필요할 수 있음. 주의사항 참고)

### SongController (`/api/songs`)
- `POST /api/songs` — 곡 등록
- `GET /api/songs` — 전체 곡 조회
- `GET /api/songs/{id}` — 곡 단건 조회
- `GET /api/songs/search?title=` — 제목으로 곡 검색
- `GET /api/songs/genre?genre=` — 장르별 곡 조회
- `GET /api/songs/recommend` — 추천곡(점수 기반 상위) 조회
- `PUT /api/songs/{id}` — 곡 수정
- `POST /api/songs/parse-titles` — 제목/아티스트 GPT 분리(운영용)
- `GET /api/songs/parse-titles/debug` — 디버그용
- `POST /api/songs/fill-era-generation` — Spotify 기반 era/generation 보강(운영용)
- `DELETE /api/songs/{id}` — 곡 삭제

### UserSongController (`/api/usersongs`, 인증 필요)
- `POST /api/usersongs` — 곡 저장
- `GET /api/usersongs/me` — 저장 목록 조회
- `PUT /api/usersongs/rating` — 별점 등록
- `PUT /api/usersongs/play/{songId}` — 재생 카운트 증가
- `DELETE /api/usersongs/{songId}` — 저장 취소

### YoutubeController (`/api/youtube`)
- `POST /api/youtube/channel` — 단일 채널 등록/처리(`{ "url": "..." }`)
- `POST /api/youtube/channels` — 다중 채널 등록/처리(`{ "urls": ["...", "..."] }`)

### ExploreController (`/api/explore`)
- `GET /api/explore?url=&limit=` — YouTube URL 기반 유사곡 탐색(분석→추천)

### QdrantController (`/api/qdrant`)
- `POST /api/qdrant/embed` — 전체 곡 벡터 Qdrant 업서트(운영용)
- `GET /api/qdrant/similar/{songId}?limit=` — 유사곡 벡터 검색(Qdrant 결과 없으면 DB score fallback)

### AnalysisController (`/api/analysis`)
- `POST /api/analysis/{songId}` — 곡 단건 분석(FastAPI 호출)
- `POST /api/analysis/sync-from-json` — 분석 결과 JSON DB 동기화(운영용)
- `POST /api/analysis/fill-youtube-stats` — 유튜브 통계 보강(운영용)
- `POST /api/analysis/batch` — 미분석 영상 일괄 분석(운영용)

### AiController (`/api/ai`)
- `POST /api/ai/chat` — Claude 텍스트 생성
- `POST /api/ai/chat-tts` — Claude 텍스트 생성 + TTS 합성

### GoogleTtsController (`/api/ai/tts`)
- `GET /api/ai/tts/voices?languageCode=` — 사용 가능 음성 목록
- `POST /api/ai/tts/synthesize` — TTS 합성(Base64 응답)
- `POST /api/ai/tts/synthesize/audio?preset=` — TTS 합성(오디오 바이너리 응답)

### PreferenceController (`/api/preferences`)
- `POST /api/preferences` — 취향 저장
- `GET /api/preferences/me` — 내 취향 조회
- `PUT /api/preferences/me` — 내 취향 수정
- `DELETE /api/preferences/me` — 내 취향 삭제

### EmbeddingController (`/api/embeddings`, 운영용)
- `POST /api/embeddings/generate` — 곡 임베딩 생성
- `POST /api/embeddings/sync-to-qdrant` — 임베딩 Qdrant 동기화

---

### 인증 필요 여부 정리 (SecurityConfig 기준)

`SecurityConfig.filterChain()`에서 명시된 규칙:
- `/api/auth/**`, `/oauth2/**`, `/login/oauth2/**`, `/auth/google/callback`, `/swagger-ui/**`, `/v3/api-docs/**` → **permitAll**
- `GET/PUT/DELETE /api/users/me` → **authenticated**
- `/api/usersongs/**` → **authenticated**
- `/api/radio/**` → **authenticated**
- `/api/likes/**` → **authenticated**
- `/api/playlists/**` → **authenticated**
- 그 외 모든 요청(`anyRequest`) → **permitAll** (즉 `/api/songs/**`, `/api/explore`, `/api/qdrant/**`, `/api/analysis/**`, `/api/ai/**`, `/api/youtube/**`, `/api/embeddings/**`, `/api/preferences/**`은 컨트롤러 단에서 `Authentication`을 사용하지만 SecurityConfig 상으로는 인증을 강제하지 않음 — 자세한 내용은 19장 참고)

## 5. 인증/JWT 흐름

### 5.1 회원가입 요청 JSON
```json
POST /api/auth/signup
{
  "nickname": "감성덕후",
  "email": "user1@example.com",
  "password": "password123"
}
```
응답: `200 OK`, body는 단순 문자열 `"회원가입 완료"` (JSON이 아닌 문자열 응답이므로 axios에서 `response.data`가 문자열임에 주의)

### 5.2 이메일 인증 요청 JSON
```json
// 인증코드 발송
POST /api/auth/email/send
{ "email": "user1@example.com" }
// 응답: "인증코드 발송 완료" (문자열)

// 인증코드 검증 (개발모드 mock-code 기본값 "123456")
POST /api/auth/email/verify
{ "email": "user1@example.com", "code": "123456" }
// 응답: "이메일 인증 완료" (문자열)
```

### 5.3 로그인 요청/응답 JSON
```json
POST /api/auth/login
{
  "email": "user1@example.com",
  "password": "password123"
}
```
응답(`AuthTokenResponseDto`):
```json
{
  "accessToken": "eyJhbGciOiJI...",
  "refreshToken": "eyJhbGciOiJI...",
  "tokenType": "Bearer",
  "expiresIn": 3600000,
  "user": {
    "id": "u001-0000-0000-0000-000000000001",
    "nickname": "감성덕후",
    "email": "user1@example.com",
    "provider": "local"
  }
}
```

### 5.4 토큰 저장 제안
- `accessToken`: 메모리(Pinia store) + `localStorage` (또는 `sessionStorage`) 동시 보관 — 새로고침 시 복원 목적
- `refreshToken`: `localStorage`에 보관(쿠키 기반 HttpOnly 저장이 더 안전하지만 현재 백엔드는 body로 refreshToken을 직접 주고받는 구조이므로 localStorage 사용을 전제로 함)
- 로그인 성공 시 `authState.accessToken`, `authState.refreshToken`, `authState.user`를 모두 저장

### 5.5 Authorization 헤더 형식
```
Authorization: Bearer {accessToken}
```
`JwtAuthenticationFilter`가 `"Bearer "` 접두사를 확인 후 토큰을 파싱하므로 정확히 `Bearer ` + 토큰 형식이어야 한다.

### 5.6 /api/users/me 호출
```
GET /api/users/me
Authorization: Bearer {accessToken}
```
응답(`UserResponseDto`):
```json
{
  "id": "u001-0000-0000-0000-000000000001",
  "nickname": "감성덕후",
  "email": "user1@example.com",
  "provider": "local"
}
```

### 5.7 토큰 재발급(refresh)
```json
POST /api/auth/refresh
{ "refreshToken": "eyJhbGciOiJI..." }
```
응답: `AuthTokenResponseDto` (위 5.3과 동일 형식, 새 accessToken/refreshToken 발급)

### 5.8 로그아웃
```json
POST /api/auth/logout
{ "refreshToken": "eyJhbGciOiJI..." }
```
응답: `"로그아웃 완료"` (문자열). 프론트에서는 호출 성공 여부와 무관하게 로컬 저장소의 토큰을 제거해야 한다.

### 5.9 인증 실패 처리 / axios interceptor 제안
- 요청 인터셉터: `authState.accessToken`이 있으면 모든 요청에 `Authorization: Bearer {accessToken}` 자동 첨부
- 응답 인터셉터: `401 Unauthorized` 수신 시
  1. `POST /api/auth/refresh`로 accessToken 재발급 시도
  2. 성공하면 원래 요청을 새 토큰으로 재시도
  3. refresh도 실패하면 로그아웃 처리(토큰 삭제 + 로그인 페이지로 리다이렉트)
- 동시에 여러 요청이 401을 받는 경우를 대비해 refresh 요청은 1회만 수행하고 나머지 요청은 대기시키는 큐(promise lock) 패턴 권장

## 6. Radio API 분석

### 6.1 POST /api/radio (라디오 세션 생성)

**인증 필요**: 예 (`Authorization: Bearer ...` 또는 `X-USER-ID` 헤더, 또는 `userId` 쿼리파라미터 — `resolveUserId()`가 우선순위대로 확인)

**Request Body** (`RadioCreateRequestDto`):
```json
{
  "mood": "그리운",
  "story": "오늘 오래된 사진을 보다가 학창시절이 너무 그리워졌어요.",
  "era": "2세대",
  "genre": "댄스",
  "situation": "퇴근길 지하철",
  "desiredMood": "위로받고 싶음",
  "videoType": "무대영상",
  "preferredArtist": "",
  "excludedKeywords": "title:cover"
}
```
모든 필드는 비어 있어도 되며, 비어 있을 경우 `RadioService.normalizeRequest()`가 사용자의 `user_preferences`(있다면) 또는 기본값(`mood=감성`, `era=2세대` 등)으로 채운다.

**Response Body** (`RadioCreateResponseDto`):
```json
{
  "radioSessionId": "f3a1...",
  "userId": "u001-...",
  "mood": "그리운",
  "story": "오늘 오래된 사진을 보다가 학창시절이 너무 그리워졌어요.",
  "era": "2세대",
  "genre": "댄스",
  "situation": "퇴근길 지하철",
  "desiredMood": "위로받고 싶음",
  "videoType": "무대영상",
  "preferredArtist": "",
  "excludedKeywords": "title:cover",
  "djMent": "안녕하세요, DJ 리아예요. ...",
  "recommendationSource": "DB_MOOD_ERA_GENRE",
  "tts": {
    "mode": "google-tts" ,
    "text": "안녕하세요, DJ 리아예요. ...",
    "audioUrl": null
  },
  "recommendedSongs": [
    {
      "songId": "s011-...",
      "title": "캔디 (AI 리마스터)",
      "artist": "H.O.T",
      "era": "2세대",
      "genre": "댄스",
      "youtubeUrl": "https://www.youtube.com/watch?v=dummy011",
      "youtubeId": "dummy011",
      "score": 85.4,
      "reason": "2004년~2011년 전후 2세대 K-POP의 강한 후렴과 무대 감성이 있어 그리운 마음을 퇴근길 지하철 상황에 맞춰 환기해줄 곡입니다. 요청한 무대영상 감상 흐름에도 어울립니다."
    }
  ]
}
```

**관련 DTO**: `RadioCreateRequestDto`, `RadioCreateResponseDto`, `RecommendedSongResponseDto`, `TtsFallbackResponseDto`
**관련 Service**: `RadioService.createRadio()`, `AiDjMentService`, `TtsService`, `PreferenceService`
**프론트엔드 화면**: 라디오 생성 입력 화면 → 결과(추천 라디오) 화면
**주의사항**:
- `tts.audioUrl`/`tts.audioContentBase64` 여부는 `gcp.tts.*` 및 `tts.enabled` 설정에 따라 달라짐(기본 비활성화 시 텍스트만 제공되는 fallback 모드 가능성). `tts.mode` 값으로 분기 처리 필요.
- `recommendationSource`는 DB 추천 단계(필터 조건)에 따라 달라지는 내부 디버그성 값으로, UI에 직접 노출할 필요는 없으나 디버깅에 유용.
- `recommendedSongs`가 빈 배열일 수 있음(`DB_EMPTY`) — 이 경우 "추천곡이 없습니다" UI 처리 필요.

### 6.2 GET /api/radio/{id} (세션 단건 조회)

**인증 필요**: 예
**Request Body**: 없음 (path: `id` = radioSessionId)
**Response Body** (`RadioResponseDto`):
```json
{
  "id": "f3a1...",
  "mood": "그리운",
  "story": "오늘 오래된 사진을 보다가...",
  "era": "2세대",
  "genre": "댄스",
  "situation": "퇴근길 지하철",
  "desiredMood": "위로받고 싶음",
  "videoType": "무대영상",
  "preferredArtist": "",
  "excludedKeywords": "title:cover",
  "recommendationSource": "DB_MOOD_ERA_GENRE",
  "djMent": "안녕하세요, DJ 리아예요...",
  "comfortText": null,
  "novelExcerpt": null,
  "createdAt": "2026-06-13T10:20:30",
  "songs": [
    { "songId": "s011-...", "title": "캔디 (AI 리마스터)", "artist": "H.O.T", "orderNum": 1, "reason": "..." }
  ]
}
```
**관련 DTO**: `RadioResponseDto`, `RadioResponseDto.RadioSongDto`
**관련 Service**: `RadioService.getSession()`
**프론트엔드 화면**: 라디오 히스토리 상세(다시 듣기) 화면
**주의사항**: 존재하지 않거나 본인 소유가 아닌 세션 조회 시 `RuntimeException("존재하지 않는 세션이거나 접근 권한이 없습니다.")` 발생 → `GlobalExceptionHandler`에 의해 500 에러로 반환됨(상태코드가 404가 아님에 주의).

### 6.3 GET /api/radio/me (내 세션 목록)

**인증 필요**: 예
**Request Body**: 없음
**Response Body**: `RadioResponseDto`의 배열 (각 항목에 `songs` 포함)
**관련 Service**: `RadioService.getSessionByUser()`
**프론트엔드 화면**: "내 라디오 히스토리" 목록 화면(마이페이지 또는 별도 탭)

## 7. Playlist API 분석

### 7.1 POST /api/playlists (생성)
**인증 필요**: 예
**Request Body** (`PlaylistDto`, `name`만 필수):
```json
{
  "name": "새벽 감성 모음",
  "moodTag": "그리운",
  "isPublic": true
}
```
**Response Body**: 생성된 `PlaylistDto` (id, userId, createdAt 포함, items는 보통 빈 배열 또는 null)
```json
{
  "id": "pl01-...",
  "userId": "u001-...",
  "name": "새벽 감성 모음",
  "moodTag": "그리운",
  "isPublic": true,
  "createdAt": "2026-06-13T10:30:00",
  "items": []
}
```
**관련 DTO**: `PlaylistDto`
**관련 Service**: `PlaylistService.createPlaylist()`
**프론트엔드 화면**: 플레이리스트 생성 모달/페이지
**주의사항**: `id`, `userId`, `createdAt`은 서버에서 생성되므로 요청 시 보내지 않아도 됨(보내도 무시될 가능성이 높음 — Service 구현에 따라 다름).

### 7.2 GET /api/playlists (내 플레이리스트 목록)
**인증 필요**: 예
**Response Body**: `PlaylistDto`의 배열
```json
[
  { "id": "pl01-...", "userId": "u001-...", "name": "새벽 감성 모음", "moodTag": "그리운", "isPublic": true, "createdAt": "2026-06-13T10:30:00", "items": [] },
  { "id": "pl02-...", "userId": "u001-...", "name": "출퇴근길 위로 플리", "moodTag": "지친", "isPublic": false, "createdAt": "2026-06-12T08:00:00", "items": [] }
]
```
**프론트엔드 화면**: 플레이리스트 목록(마이페이지/플레이리스트 탭)

### 7.3 GET /api/playlists/{playlistId} (상세)
**인증 필요**: 예
**Response Body**: `PlaylistDto` + `items`(`PlaylistItemDto` 배열)
```json
{
  "id": "pl01-...",
  "userId": "u001-...",
  "name": "새벽 감성 모음",
  "moodTag": "그리운",
  "isPublic": true,
  "createdAt": "2026-06-13T10:30:00",
  "items": [
    {
      "id": "ps01-...",
      "playlistId": "pl01-...",
      "songId": "s012-...",
      "title": "To Heaven (AI 리마스터)",
      "artist": "god",
      "genre": "발라드",
      "era": "00s",
      "youtubeUrl": "https://www.youtube.com/watch?v=dummy012",
      "youtubeId": "dummy012",
      "orderNum": 1,
      "addedAt": "2026-06-13T10:31:00"
    }
  ]
}
```
**프론트엔드 화면**: 플레이리스트 상세(곡 목록 + 재생/삭제 액션)
**주의사항**: 본인 소유가 아닌 플레이리스트 접근 시 서버 로직에 따라 예외 발생 가능(공개 플레이리스트 열람 정책은 코드상 명확히 구분되어 있지 않음 — `isPublic` 필드는 존재하나 컨트롤러는 `authentication.getName()` 기준으로만 조회).

### 7.4 POST /api/playlists/{playlistId}/items (곡 추가)
**인증 필요**: 예
**Request Body** (`PlaylistItemDto`, `songId` 필수):
```json
{
  "songId": "s011-0000-0000-0000-000000000011",
  "title": "캔디 (AI 리마스터)",
  "artist": "H.O.T",
  "genre": "댄스",
  "era": "2세대",
  "youtubeUrl": "https://www.youtube.com/watch?v=dummy011",
  "youtubeId": "dummy011"
}
```
**Response Body**: 생성된 `PlaylistItemDto` (id, playlistId, orderNum, addedAt 포함)
**관련 Service**: `PlaylistService.addItem()`
**프론트엔드 화면**: 라디오 추천 결과 카드의 "플레이리스트에 추가" 버튼 → 플레이리스트 선택 모달 → 이 API 호출
**주의사항**: `title`/`artist`/`genre`/`era`/`youtubeUrl`/`youtubeId`는 선택값이지만, 라디오 추천 응답(`RecommendedSongResponseDto`)의 필드를 그대로 채워 보내면 플레이리스트 상세에서 별도 조회 없이 표시 가능 — 프론트에서 이 매핑을 적극 활용 권장.

### 7.5 DELETE /api/playlists/{playlistId}/items/{itemId} (곡 삭제)
**인증 필요**: 예
**Response Body**: `{ "message": "플레이리스트 항목 삭제 완료" }`
**프론트엔드 화면**: 플레이리스트 상세에서 곡 항목 삭제 버튼

### 7.6 DELETE /api/playlists/{playlistId} (플레이리스트 삭제)
**인증 필요**: 예
**Response Body**: `{ "message": "플레이리스트 삭제 완료" }`
**프론트엔드 화면**: 플레이리스트 목록/상세에서 삭제 버튼(확인 모달 권장)

## 8. Like API 분석

### 8.1 POST /api/likes (좋아요 등록)
**인증 필요**: 예
**Request Body** (`LikeDto`, `songId` 필수):
```json
{ "songId": "s011-0000-0000-0000-000000000011" }
```
**Response Body** (`LikeStatusDto`):
```json
{ "songId": "s011-0000-0000-0000-000000000011", "liked": true, "likeCount": 12 }
```
**관련 Service**: `LikeService.addLike()`
**프론트엔드 화면**: 곡 카드/플레이어의 좋아요(하트) 버튼

### 8.2 DELETE /api/likes/{songId} (좋아요 취소)
**인증 필요**: 예
**Response Body** (`LikeStatusDto`):
```json
{ "songId": "s011-0000-0000-0000-000000000011", "liked": false, "likeCount": 11 }
```

### 8.3 GET /api/likes/{songId}/status (좋아요 상태 조회)
**인증 필요**: 예 (SecurityConfig상 `/api/likes/**`는 authenticated)
**Response Body** (`LikeStatusDto`):
```json
{ "songId": "s011-0000-0000-0000-000000000011", "liked": true, "likeCount": 12 }
```
**프론트엔드 화면**: 곡 카드 렌더링 시 초기 좋아요 상태 표시(좋아요 버튼 채워짐/비워짐)

### 8.4 GET /api/likes (내 좋아요 목록 - 원시 데이터)
**인증 필요**: 예
**Response Body**: `LikeDto` 배열
```json
[
  { "id": "lk01-...", "userId": "u001-...", "songId": "s011-...", "createdAt": "2026-06-10T09:00:00" }
]
```

### 8.5 GET /api/likes/songs (내 좋아요 곡 상세 목록)
**인증 필요**: 예
**Response Body**: `SongDto` 배열 (곡 제목, 아티스트, 유튜브 정보 등 전체 필드 포함)
**프론트엔드 화면**: "내가 좋아요한 곡" 목록 화면(마이페이지) — 이 API가 곡 상세 정보를 직접 제공하므로 `GET /api/likes`보다 이 API가 화면 구현에 더 적합

### 8.6 GET /api/likes/{songId}/count (곡 좋아요 수)
**인증 필요**: 코드상 인증 객체를 사용하지 않으나, SecurityConfig는 `/api/likes/**` 전체를 authenticated로 지정 — 실질적으로 인증 필요할 가능성이 높음(19장 참고)
**Response Body**:
```json
{ "songId": "s011-0000-0000-0000-000000000011", "likeCount": 12 }
```
**프론트엔드 화면**: 곡 카드에 좋아요 수 배지 표시

## 9. User/Auth API 분석

(5장과 중복되는 내용을 포함하되, 요청/응답 형태를 표 형태로 다시 정리)

| API | 인증 | Request | Response |
|---|---|---|---|
| `POST /api/auth/signup` | 불필요 | `UserRegisterRequestDto` (nickname, email, password) | 문자열 `"회원가입 완료"` |
| `POST /api/auth/email/send` | 불필요 | `EmailVerificationSendRequestDto` (email) | 문자열 `"인증코드 발송 완료"` |
| `POST /api/auth/email/verify` | 불필요 | `EmailVerificationCheckRequestDto` (email, code: 6자리 숫자) | 문자열 `"이메일 인증 완료"` |
| `POST /api/auth/login` | 불필요 | `UserLoginRequestDto` (email, password) | `AuthTokenResponseDto` |
| `POST /api/auth/refresh` | 불필요(리프레시 토큰 자체가 인증) | `RefreshTokenRequestDto` (refreshToken) | `AuthTokenResponseDto` |
| `POST /api/auth/logout` | 불필요(리프레시 토큰 자체가 인증) | `LogoutRequestDto` (refreshToken) | 문자열 `"로그아웃 완료"` |
| `GET /api/users/me` | 필요 | 없음 | `UserResponseDto` (id, nickname, email, provider) |
| `PUT /api/users/me` | 필요 | `UserUpdateRequestDto` (nickname, email) | 문자열 `"수정 완료"` |
| `DELETE /api/users/me` | 필요 | 없음 | 문자열 `"삭제 완료"` |

**주의사항**:
- 회원가입/인증/수정/탈퇴 API들은 JSON이 아닌 **순수 문자열**을 응답 body로 반환한다. axios에서는 `response.data`가 문자열("회원가입 완료" 등)이 되므로, 성공 판정은 HTTP status(200)와 문자열 내용으로 처리해야 한다.
- `UserUpdateRequestDto`에는 `password` 필드가 없으므로, 비밀번호 변경 기능은 현재 백엔드에 구현되어 있지 않다(19장 참고).
- `provider` 값은 `local | google | kakao` 중 하나이며, 소셜 로그인 사용자는 `password` 입력란을 표시할 필요가 없다.

## 10. Vue 프론트엔드 핵심 사용자 흐름

1. **회원가입/로그인**
   - 신규 사용자: `POST /api/auth/email/send` → `POST /api/auth/email/verify` → `POST /api/auth/signup` → `POST /api/auth/login`
   - 기존 사용자: `POST /api/auth/login`
   - 로그인 성공 시 `accessToken`/`refreshToken`/`user` 저장 후 메인(라디오 생성) 화면으로 이동

2. **(선택) 취향 온보딩**
   - `POST /api/preferences` 로 선호 세대/분위기/아티스트/장르/영상타입/제외 항목 저장
   - 이후 라디오 생성 시 입력값이 비어 있으면 이 취향이 기본값으로 사용됨(`RadioService.normalizeRequest()`)

3. **감정/상황/시대/장르 입력 → AI 커버 라디오 생성**
   - 라디오 생성 화면에서 `mood`, `story`, `era`, `genre`, `situation`, `desiredMood`, `videoType`, `preferredArtist`, `excludedKeywords` 입력
   - `POST /api/radio` 호출

4. **AI DJ 코멘트 확인**
   - 응답의 `djMent`(텍스트)와 `tts`(음성, 모드에 따라 `audioUrl`/`audioContentBase64`) 표시
   - 오디오 플레이어로 DJ 멘트 음성 재생

5. **추천 곡/영상 확인**
   - 응답의 `recommendedSongs` 배열을 카드 리스트로 렌더링(제목, 아티스트, 장르, 연대, 추천 이유, 유튜브 썸네일/임베드)

6. **좋아요**
   - 각 추천곡 카드에서 `POST /api/likes` (등록) / `DELETE /api/likes/{songId}` (취소)
   - 카드 노출 시 `GET /api/likes/{songId}/status`로 초기 상태 확인

7. **플레이리스트에 저장**
   - "플레이리스트에 추가" 클릭 → 플레이리스트 선택 모달(`GET /api/playlists`로 목록 표시, 없으면 `POST /api/playlists`로 새로 생성)
   - 선택한 플레이리스트에 `POST /api/playlists/{playlistId}/items`로 곡 추가(추천곡 정보를 그대로 전달)

8. **내 플레이리스트 보기**
   - `GET /api/playlists` → 목록, `GET /api/playlists/{playlistId}` → 상세(곡 목록), 곡별 유튜브 재생/삭제 가능

9. **(부가) 라디오 히스토리**
   - `GET /api/radio/me` → 과거 생성한 라디오 세션 목록, `GET /api/radio/{id}` → 특정 세션 다시보기

10. **(부가) 내 보관함 / 좋아요 곡**
    - `GET /api/usersongs/me` (저장곡), `GET /api/likes/songs` (좋아요 곡)

## 11. Vue 화면 구성 제안

### 11.1 Landing (랜딩)
- **route**: `/`
- **사용 목적**: 서비스 소개, 로그인/회원가입 유도
- **필요 API**: 없음(또는 인기곡 미리보기용 `GET /api/songs/recommend`)
- **필요 상태값**: `authState.isAuthenticated`
- **주요 컴포넌트**: `HeroSection`, `FeatureCards`, `CtaButtons`
- **사용자 액션**: "라디오 만들기" 클릭 → 로그인 여부에 따라 분기
- **성공 시 이동**: 로그인 상태면 `/radio/create`, 아니면 `/login`
- **실패 시 처리**: 해당 없음

### 11.2 Login (로그인)
- **route**: `/login`
- **사용 목적**: 이메일/비밀번호 로그인
- **필요 API**: `POST /api/auth/login`
- **필요 상태값**: `authState.accessToken`, `authState.refreshToken`, `authState.user`
- **주요 컴포넌트**: `LoginForm`, `ErrorAlert`
- **사용자 액션**: 이메일/비밀번호 입력 후 로그인 버튼 클릭
- **성공 시 이동**: `/radio/create` (또는 이전 페이지)
- **실패 시 처리**: `ErrorResponse.message`를 alert/toast로 표시(이메일/비밀번호 불일치 등)

### 11.3 Signup (회원가입)
- **route**: `/signup`
- **사용 목적**: 닉네임/이메일/비밀번호 입력, 이메일 인증, 가입 완료
- **필요 API**: `POST /api/auth/email/send`, `POST /api/auth/email/verify`, `POST /api/auth/signup`
- **필요 상태값**: 로컬 폼 상태(`nickname`, `email`, `password`, `code`, `isEmailVerified`)
- **주요 컴포넌트**: `SignupForm`, `EmailVerifyStep`, `PasswordInput`
- **사용자 액션**: 이메일 입력 → 인증코드 발송 → 코드 입력 후 검증 → 닉네임/비밀번호 입력 → 가입
- **성공 시 이동**: `/login` (또는 자동 로그인 후 `/preferences/onboarding`)
- **실패 시 처리**: 필드별 유효성 오류(`fieldErrors`)를 입력란 아래에 표시, 이메일 미인증 시 가입 버튼 비활성화

### 11.4 Preferences Onboarding (취향 온보딩, 선택)
- **route**: `/onboarding`
- **사용 목적**: 선호 세대/분위기/아티스트/장르/영상타입, 제외 항목 입력
- **필요 API**: `POST /api/preferences`
- **필요 상태값**: `preferenceState`(폼 데이터)
- **주요 컴포넌트**: `TagSelector`, `GenreChips`, `MoodChips`
- **사용자 액션**: 각 카테고리에서 다중 선택 후 저장
- **성공 시 이동**: `/radio/create`
- **실패 시 처리**: 저장 실패해도 스킵 가능(필수 아님)

### 11.5 Radio Create (라디오 생성)
- **route**: `/radio/create`
- **사용 목적**: 감정/상황/시대/장르 등을 입력해 라디오 생성 요청
- **필요 API**: `POST /api/radio`, (선택) `GET /api/preferences/me`로 기존 취향 프리필
- **필요 상태값**: `radioState.form`(mood, story, era, genre, situation, desiredMood, videoType, preferredArtist, excludedKeywords), `uiState.isLoading`
- **주요 컴포넌트**: `MoodSelector`, `StoryTextarea`, `EraGenreSelector`, `AdvancedOptionsPanel`, `SubmitButton`
- **사용자 액션**: 무드/상황/시대/장르 선택, 사연 입력, "라디오 만들기" 클릭
- **성공 시 이동**: `/radio/result/{radioSessionId}` (응답의 `radioSessionId`를 라우트 파라미터로 사용하거나, 생성 응답을 store에 저장 후 `/radio/result`로 이동)
- **실패 시 처리**: 로딩 스피너 종료 후 에러 토스트, 폼 유지(재시도 가능)

### 11.6 Radio Result (라디오 결과)
- **route**: `/radio/result/:id` (또는 `/radio/result`, store 기반)
- **사용 목적**: AI DJ 멘트 + 추천곡 리스트 표시, 좋아요/플레이리스트 저장
- **필요 API**: `GET /api/radio/{id}` (재방문 시) 또는 생성 직후 store 데이터 사용, `POST /api/likes`, `DELETE /api/likes/{songId}`, `GET /api/likes/{songId}/status`, `GET /api/playlists`, `POST /api/playlists`, `POST /api/playlists/{playlistId}/items`
- **필요 상태값**: `radioState.currentSession`(djMent, tts, recommendedSongs), `playlistState.myPlaylists`, `uiState.selectedSongForPlaylist`
- **주요 컴포넌트**: `DjMentCard`(텍스트+오디오 플레이어), `RecommendedSongCard`(좋아요 버튼, 유튜브 임베드, "플레이리스트에 추가" 버튼), `AddToPlaylistModal`
- **사용자 액션**: DJ 멘트 듣기, 곡 좋아요, 영상 재생, 플레이리스트에 추가
- **성공 시 이동**: 해당 화면에 머무름(토스트로 피드백)
- **실패 시 처리**: 좋아요/플레이리스트 추가 실패 시 토스트 알림, 추천곡이 비어있으면 안내 메시지("추천곡을 찾지 못했습니다")

### 11.7 Playlist List (플레이리스트 목록)
- **route**: `/playlists`
- **사용 목적**: 내 플레이리스트 전체 보기, 새 플레이리스트 생성
- **필요 API**: `GET /api/playlists`, `POST /api/playlists`, `DELETE /api/playlists/{playlistId}`
- **필요 상태값**: `playlistState.myPlaylists`
- **주요 컴포넌트**: `PlaylistCard`, `CreatePlaylistModal`
- **사용자 액션**: 카드 클릭(상세 이동), 새 플레이리스트 생성, 삭제
- **성공 시 이동**: 카드 클릭 시 `/playlists/:id`
- **실패 시 처리**: 목록 로드 실패 시 빈 상태 + 재시도 버튼

### 11.8 Playlist Detail (플레이리스트 상세)
- **route**: `/playlists/:id`
- **사용 목적**: 플레이리스트 내 곡 목록 확인, 재생, 곡 삭제
- **필요 API**: `GET /api/playlists/{playlistId}`, `DELETE /api/playlists/{playlistId}/items/{itemId}`, `DELETE /api/playlists/{playlistId}`
- **필요 상태값**: `playlistState.currentPlaylist`
- **주요 컴포넌트**: `PlaylistHeader`, `PlaylistSongList`, `YoutubePlayerModal`
- **사용자 액션**: 곡 재생(유튜브 임베드/모달), 곡 삭제, 플레이리스트 삭제
- **성공 시 이동**: 플레이리스트 삭제 시 `/playlists`로 복귀
- **실패 시 처리**: 곡 삭제 실패 시 토스트, 목록 갱신 실패 시 새로고침 버튼

### 11.9 My Page (마이페이지)
- **route**: `/me`
- **사용 목적**: 내 정보 조회/수정/탈퇴, 좋아요 곡/보관함/라디오 히스토리 진입점
- **필요 API**: `GET /api/users/me`, `PUT /api/users/me`, `DELETE /api/users/me`, `GET /api/likes/songs`, `GET /api/usersongs/me`, `GET /api/radio/me`
- **필요 상태값**: `authState.user`, `userState.likedSongs`, `userState.savedSongs`, `userState.radioHistory`
- **주요 컴포넌트**: `ProfileCard`, `ProfileEditForm`, `LikedSongsTab`, `SavedSongsTab`, `RadioHistoryTab`, `DeleteAccountButton`
- **사용자 액션**: 닉네임/이메일 수정, 탭 전환(좋아요/보관함/히스토리), 회원 탈퇴
- **성공 시 이동**: 탈퇴 성공 시 로그아웃 처리 후 `/`
- **실패 시 처리**: 수정 실패 시 필드 오류 표시, 탈퇴 확인 모달 필수

## 12. Vue 컴포넌트 구조

```
FE/
├── index.html
├── package.json
├── vite.config.js
├── .env (VITE_API_BASE_URL=http://localhost:8080)
└── src/
    ├── main.js
    ├── App.vue
    ├── router/
    │   └── index.js                  # vue-router 라우트 정의 (11장 화면 매핑)
    ├── api/
    │   ├── axios.js                  # axios 인스턴스 + 인터셉터(JWT 첨부, 401 재발급)
    │   ├── authApi.js                # /api/auth/**, /api/users/**
    │   ├── radioApi.js                # /api/radio/**
    │   ├── playlistApi.js             # /api/playlists/**
    │   ├── likeApi.js                  # /api/likes/**
    │   ├── songApi.js                  # /api/songs/**, /api/explore, /api/qdrant
    │   ├── userSongApi.js              # /api/usersongs/**
    │   └── preferenceApi.js            # /api/preferences/**
    ├── store/                          # Pinia
    │   ├── authStore.js
    │   ├── radioStore.js
    │   ├── playlistStore.js
    │   ├── preferenceStore.js
    │   └── uiStore.js
    ├── pages/
    │   ├── LandingPage.vue
    │   ├── LoginPage.vue
    │   ├── SignupPage.vue
    │   ├── OnboardingPage.vue
    │   ├── RadioCreatePage.vue
    │   ├── RadioResultPage.vue
    │   ├── PlaylistListPage.vue
    │   ├── PlaylistDetailPage.vue
    │   └── MyPage.vue
    ├── components/
    │   ├── common/
    │   │   ├── AppHeader.vue
    │   │   ├── AppFooter.vue
    │   │   ├── ToastNotification.vue
    │   │   ├── LoadingSpinner.vue
    │   │   └── ConfirmModal.vue
    │   ├── auth/
    │   │   ├── LoginForm.vue
    │   │   ├── SignupForm.vue
    │   │   └── EmailVerifyStep.vue
    │   ├── radio/
    │   │   ├── MoodSelector.vue
    │   │   ├── StoryTextarea.vue
    │   │   ├── EraGenreSelector.vue
    │   │   ├── AdvancedOptionsPanel.vue
    │   │   ├── DjMentCard.vue
    │   │   ├── RecommendedSongCard.vue
    │   │   └── AddToPlaylistModal.vue
    │   ├── playlist/
    │   │   ├── PlaylistCard.vue
    │   │   ├── PlaylistSongList.vue
    │   │   └── CreatePlaylistModal.vue
    │   ├── song/
    │   │   ├── SongCard.vue
    │   │   ├── LikeButton.vue
    │   │   └── YoutubePlayerModal.vue
    │   └── user/
    │       ├── ProfileCard.vue
    │       ├── ProfileEditForm.vue
    │       └── RadioHistoryList.vue
    └── assets/
        ├── styles/
        │   ├── main.css
        │   ├── variables.css       # 컬러/그라디언트/다크모드 변수
        │   └── components.css
        └── images/
```

## 13. axios API 모듈 구조

### 13.1 api/axios.js
```javascript
import axios from 'axios'
import { useAuthStore } from '@/store/authStore'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  headers: { 'Content-Type': 'application/json' }
})

api.interceptors.request.use((config) => {
  const authStore = useAuthStore()
  if (authStore.accessToken) {
    config.headers.Authorization = `Bearer ${authStore.accessToken}`
  }
  return config
})

let isRefreshing = false
let pendingQueue = []

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const authStore = useAuthStore()
    const { config, response } = error

    if (response?.status === 401 && !config._retry) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          pendingQueue.push({ resolve, reject, config })
        })
      }

      config._retry = true
      isRefreshing = true
      try {
        await authStore.refreshAccessToken()
        pendingQueue.forEach(({ resolve, config: c }) => resolve(api(c)))
        pendingQueue = []
        return api(config)
      } catch (refreshError) {
        pendingQueue.forEach(({ reject }) => reject(refreshError))
        pendingQueue = []
        authStore.logout()
        window.location.href = '/login'
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }
    return Promise.reject(error)
  }
)

export default api
```

### 13.2 api/authApi.js
```javascript
import api from './axios'

export const authApi = {
  sendEmailCode: (email) => api.post('/api/auth/email/send', { email }),
  verifyEmailCode: (email, code) => api.post('/api/auth/email/verify', { email, code }),
  signup: (nickname, email, password) => api.post('/api/auth/signup', { nickname, email, password }),
  login: (email, password) => api.post('/api/auth/login', { email, password }),
  refresh: (refreshToken) => api.post('/api/auth/refresh', { refreshToken }),
  logout: (refreshToken) => api.post('/api/auth/logout', { refreshToken }),
  getMyInfo: () => api.get('/api/users/me'),
  updateMyInfo: (nickname, email) => api.put('/api/users/me', { nickname, email }),
  deleteMyAccount: () => api.delete('/api/users/me')
}
```

### 13.3 api/radioApi.js
```javascript
import api from './axios'

export const radioApi = {
  createRadio: (payload) => api.post('/api/radio', payload),
  // payload: { mood, story, era, genre, situation, desiredMood, videoType, preferredArtist, excludedKeywords }
  getSession: (id) => api.get(`/api/radio/${id}`),
  getMySessions: () => api.get('/api/radio/me')
}
```

### 13.4 api/playlistApi.js
```javascript
import api from './axios'

export const playlistApi = {
  createPlaylist: (name, moodTag, isPublic) =>
    api.post('/api/playlists', { name, moodTag, isPublic }),
  getMyPlaylists: () => api.get('/api/playlists'),
  getPlaylist: (playlistId) => api.get(`/api/playlists/${playlistId}`),
  addItem: (playlistId, item) => api.post(`/api/playlists/${playlistId}/items`, item),
  // item: { songId, title, artist, genre, era, youtubeUrl, youtubeId }
  deleteItem: (playlistId, itemId) => api.delete(`/api/playlists/${playlistId}/items/${itemId}`),
  deletePlaylist: (playlistId) => api.delete(`/api/playlists/${playlistId}`)
}
```

### 13.5 api/likeApi.js
```javascript
import api from './axios'

export const likeApi = {
  addLike: (songId) => api.post('/api/likes', { songId }),
  removeLike: (songId) => api.delete(`/api/likes/${songId}`),
  getLikeStatus: (songId) => api.get(`/api/likes/${songId}/status`),
  getMyLikes: () => api.get('/api/likes'),
  getMyLikedSongs: () => api.get('/api/likes/songs'),
  getLikeCount: (songId) => api.get(`/api/likes/${songId}/count`)
}
```

### 13.6 api/songApi.js
```javascript
import api from './axios'

export const songApi = {
  getAllSongs: () => api.get('/api/songs'),
  getSongById: (id) => api.get(`/api/songs/${id}`),
  searchByTitle: (title) => api.get('/api/songs/search', { params: { title } }),
  getByGenre: (genre) => api.get('/api/songs/genre', { params: { genre } }),
  getRecommended: () => api.get('/api/songs/recommend'),
  explore: (url, limit = 10) => api.get('/api/explore', { params: { url, limit } }),
  getSimilar: (songId, limit = 10) => api.get(`/api/qdrant/similar/${songId}`, { params: { limit } })
}
```

### 13.7 api/userSongApi.js (보관함)
```javascript
import api from './axios'

export const userSongApi = {
  saveSong: (songId, rating = null) => api.post('/api/usersongs', { songId, rating }),
  getSavedSongs: () => api.get('/api/usersongs/me'),
  updateRating: (songId, rating) => api.put('/api/usersongs/rating', { songId, rating }),
  increasePlayCount: (songId) => api.put(`/api/usersongs/play/${songId}`),
  deleteSavedSong: (songId) => api.delete(`/api/usersongs/${songId}`)
}
```

### 13.8 api/preferenceApi.js
```javascript
import api from './axios'

export const preferenceApi = {
  save: (payload) => api.post('/api/preferences', payload),
  getMine: () => api.get('/api/preferences/me'),
  update: (payload) => api.put('/api/preferences/me', payload),
  remove: () => api.delete('/api/preferences/me')
  // payload: { preferredGenerations, preferredMoods, preferredArtists,
  //            preferredGenres, preferredVideoTypes, excludedGenres, excludedKeywords }
}
```

## 14. API별 요청/응답 JSON 예시

### 14.1 회원가입
```json
// Request: POST /api/auth/signup
{ "nickname": "감성덕후", "email": "user1@example.com", "password": "password123" }
// Response: 200 OK, body: "회원가입 완료"
```

### 14.2 이메일 인증
```json
// Request: POST /api/auth/email/send
{ "email": "user1@example.com" }
// Response: "인증코드 발송 완료"

// Request: POST /api/auth/email/verify
{ "email": "user1@example.com", "code": "123456" }
// Response: "이메일 인증 완료"
```

### 14.3 로그인
```json
// Request: POST /api/auth/login
{ "email": "user1@example.com", "password": "password123" }

// Response: 200 OK
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600000,
  "user": { "id": "u001-...", "nickname": "감성덕후", "email": "user1@example.com", "provider": "local" }
}
```

### 14.4 내 정보 조회
```json
// Request: GET /api/users/me  (Authorization: Bearer ...)
// Response:
{ "id": "u001-...", "nickname": "감성덕후", "email": "user1@example.com", "provider": "local" }
```

### 14.5 라디오 생성
```json
// Request: POST /api/radio
{
  "mood": "그리운",
  "story": "오늘 오래된 사진을 보다가 학창시절이 너무 그리워졌어요.",
  "era": "2세대",
  "genre": "댄스",
  "situation": "퇴근길 지하철",
  "desiredMood": "위로받고 싶음",
  "videoType": "무대영상",
  "preferredArtist": "",
  "excludedKeywords": ""
}

// Response:
{
  "radioSessionId": "f3a1b2c3-...",
  "userId": "u001-...",
  "mood": "그리운",
  "story": "오늘 오래된 사진을 보다가 학창시절이 너무 그리워졌어요.",
  "era": "2세대",
  "genre": "댄스",
  "situation": "퇴근길 지하철",
  "desiredMood": "위로받고 싶음",
  "videoType": "무대영상",
  "preferredArtist": "",
  "excludedKeywords": "",
  "djMent": "안녕하세요, DJ 리아예요. 오늘 사연 잘 들었어요...",
  "recommendationSource": "DB_MOOD_ERA_GENRE",
  "tts": { "mode": "google-tts", "text": "안녕하세요, DJ 리아예요...", "audioUrl": null },
  "recommendedSongs": [
    {
      "songId": "s011-...",
      "title": "캔디 (AI 리마스터)",
      "artist": "H.O.T",
      "era": "2세대",
      "genre": "댄스",
      "youtubeUrl": "https://www.youtube.com/watch?v=dummy011",
      "youtubeId": "dummy011",
      "score": 85.4,
      "reason": "2004년~2011년 전후 2세대 K-POP의 강한 후렴과 무대 감성이 있어 그리운 마음을 퇴근길 지하철 상황에 맞춰 환기해줄 곡입니다. 요청한 무대영상 감상 흐름에도 어울립니다."
    }
  ]
}
```

### 14.6 라디오 세션 상세 조회
```json
// Request: GET /api/radio/f3a1b2c3-...
// Response:
{
  "id": "f3a1b2c3-...",
  "mood": "그리운",
  "story": "오늘 오래된 사진을 보다가 학창시절이 너무 그리워졌어요.",
  "era": "2세대",
  "genre": "댄스",
  "situation": "퇴근길 지하철",
  "desiredMood": "위로받고 싶음",
  "videoType": "무대영상",
  "preferredArtist": "",
  "excludedKeywords": "",
  "recommendationSource": "DB_MOOD_ERA_GENRE",
  "djMent": "안녕하세요, DJ 리아예요...",
  "comfortText": null,
  "novelExcerpt": null,
  "createdAt": "2026-06-13T10:20:30",
  "songs": [
    { "songId": "s011-...", "title": "캔디 (AI 리마스터)", "artist": "H.O.T", "orderNum": 1, "reason": "..." }
  ]
}
```

### 14.7 좋아요 등록/취소/상태
```json
// Request: POST /api/likes  { "songId": "s011-..." }
// Response: { "songId": "s011-...", "liked": true, "likeCount": 12 }

// Request: DELETE /api/likes/s011-...
// Response: { "songId": "s011-...", "liked": false, "likeCount": 11 }

// Request: GET /api/likes/s011-.../status
// Response: { "songId": "s011-...", "liked": false, "likeCount": 11 }
```

### 14.8 플레이리스트 생성/목록/상세
```json
// Request: POST /api/playlists  { "name": "새벽 감성 모음", "moodTag": "그리운", "isPublic": true }
// Response:
{ "id": "pl01-...", "userId": "u001-...", "name": "새벽 감성 모음", "moodTag": "그리운", "isPublic": true, "createdAt": "2026-06-13T10:30:00", "items": [] }

// Request: GET /api/playlists
// Response:
[
  { "id": "pl01-...", "userId": "u001-...", "name": "새벽 감성 모음", "moodTag": "그리운", "isPublic": true, "createdAt": "2026-06-13T10:30:00", "items": [] }
]

// Request: GET /api/playlists/pl01-...
// Response:
{
  "id": "pl01-...", "userId": "u001-...", "name": "새벽 감성 모음", "moodTag": "그리운", "isPublic": true,
  "createdAt": "2026-06-13T10:30:00",
  "items": [
    { "id": "ps01-...", "playlistId": "pl01-...", "songId": "s012-...", "title": "To Heaven (AI 리마스터)", "artist": "god", "genre": "발라드", "era": "00s", "youtubeUrl": "https://www.youtube.com/watch?v=dummy012", "youtubeId": "dummy012", "orderNum": 1, "addedAt": "2026-06-13T10:31:00" }
  ]
}
```

### 14.9 플레이리스트에 곡 추가
```json
// Request: POST /api/playlists/pl01-.../items
{
  "songId": "s011-...",
  "title": "캔디 (AI 리마스터)",
  "artist": "H.O.T",
  "genre": "댄스",
  "era": "2세대",
  "youtubeUrl": "https://www.youtube.com/watch?v=dummy011",
  "youtubeId": "dummy011"
}
// Response:
{ "id": "ps13-...", "playlistId": "pl01-...", "songId": "s011-...", "title": "캔디 (AI 리마스터)", "artist": "H.O.T", "genre": "댄스", "era": "2세대", "youtubeUrl": "https://www.youtube.com/watch?v=dummy011", "youtubeId": "dummy011", "orderNum": 2, "addedAt": "2026-06-13T11:00:00" }
```

### 14.10 라디오 결과 → 플레이리스트 저장 플로우 (수동 조합)
백엔드에 "라디오 결과를 플레이리스트에 한 번에 저장"하는 전용 API는 없으므로, 프론트엔드에서 다음 순서로 조합한다.

```javascript
// 1) 플레이리스트가 없으면 새로 생성
const { data: playlist } = await playlistApi.createPlaylist('새벽 감성 모음', '그리운', true)

// 2) 추천곡 각각을 곡 추가 API로 전송 (radio 응답의 recommendedSongs 배열을 그대로 매핑)
for (const song of radioResult.recommendedSongs) {
  await playlistApi.addItem(playlist.id, {
    songId: song.songId,
    title: song.title,
    artist: song.artist,
    genre: song.genre,
    era: song.era,
    youtubeUrl: song.youtubeUrl,
    youtubeId: song.youtubeId
  })
}
```

## 15. JavaScript 상태 관리 구조

### 15.1 Plain JS (reactive 객체) 방식
```javascript
// store/state.js (Vue 3 reactive 기반, Pinia 없이 사용할 경우)
import { reactive } from 'vue'

export const authState = reactive({
  accessToken: localStorage.getItem('accessToken') || null,
  refreshToken: localStorage.getItem('refreshToken') || null,
  user: null, // { id, nickname, email, provider }
  isAuthenticated: false
})

export const radioState = reactive({
  form: {
    mood: '', story: '', era: '', genre: '', situation: '',
    desiredMood: '', videoType: '', preferredArtist: '', excludedKeywords: ''
  },
  currentSession: null, // RadioCreateResponseDto
  history: [] // RadioResponseDto[]
})

export const playlistState = reactive({
  myPlaylists: [], // PlaylistDto[]
  currentPlaylist: null // PlaylistDto with items
})

export const uiState = reactive({
  isLoading: false,
  toast: null, // { type: 'success' | 'error', message: string }
  modal: null  // 'addToPlaylist' | 'createPlaylist' | 'confirmDelete' | null
})
```

### 15.2 Pinia 방식
```javascript
// store/authStore.js
import { defineStore } from 'pinia'
import { authApi } from '@/api/authApi'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    accessToken: localStorage.getItem('accessToken') || null,
    refreshToken: localStorage.getItem('refreshToken') || null,
    user: null
  }),
  getters: {
    isAuthenticated: (state) => !!state.accessToken
  },
  actions: {
    async login(email, password) {
      const { data } = await authApi.login(email, password)
      this.setTokens(data.accessToken, data.refreshToken)
      this.user = data.user
    },
    setTokens(accessToken, refreshToken) {
      this.accessToken = accessToken
      this.refreshToken = refreshToken
      localStorage.setItem('accessToken', accessToken)
      localStorage.setItem('refreshToken', refreshToken)
    },
    async refreshAccessToken() {
      const { data } = await authApi.refresh(this.refreshToken)
      this.setTokens(data.accessToken, data.refreshToken)
      return data
    },
    async fetchMyInfo() {
      const { data } = await authApi.getMyInfo()
      this.user = data
    },
    logout() {
      this.accessToken = null
      this.refreshToken = null
      this.user = null
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
    }
  }
})

// store/radioStore.js
import { defineStore } from 'pinia'
import { radioApi } from '@/api/radioApi'

export const useRadioStore = defineStore('radio', {
  state: () => ({
    form: {
      mood: '', story: '', era: '', genre: '', situation: '',
      desiredMood: '', videoType: '', preferredArtist: '', excludedKeywords: ''
    },
    currentSession: null,
    history: []
  }),
  actions: {
    async createRadio() {
      const { data } = await radioApi.createRadio(this.form)
      this.currentSession = data
      return data
    },
    async fetchSession(id) {
      const { data } = await radioApi.getSession(id)
      this.currentSession = data
      return data
    },
    async fetchHistory() {
      const { data } = await radioApi.getMySessions()
      this.history = data
    }
  }
})

// store/playlistStore.js
import { defineStore } from 'pinia'
import { playlistApi } from '@/api/playlistApi'

export const usePlaylistStore = defineStore('playlist', {
  state: () => ({
    myPlaylists: [],
    currentPlaylist: null
  }),
  actions: {
    async fetchMyPlaylists() {
      const { data } = await playlistApi.getMyPlaylists()
      this.myPlaylists = data
    },
    async createPlaylist(name, moodTag, isPublic) {
      const { data } = await playlistApi.createPlaylist(name, moodTag, isPublic)
      this.myPlaylists.push(data)
      return data
    },
    async fetchPlaylist(id) {
      const { data } = await playlistApi.getPlaylist(id)
      this.currentPlaylist = data
    },
    async addItem(playlistId, item) {
      const { data } = await playlistApi.addItem(playlistId, item)
      if (this.currentPlaylist?.id === playlistId) {
        this.currentPlaylist.items.push(data)
      }
      return data
    }
  }
})

// store/uiStore.js
import { defineStore } from 'pinia'

export const useUiStore = defineStore('ui', {
  state: () => ({
    isLoading: false,
    toast: null,
    modal: null
  }),
  actions: {
    showToast(type, message) {
      this.toast = { type, message }
      setTimeout(() => { this.toast = null }, 3000)
    },
    openModal(name) { this.modal = name },
    closeModal() { this.modal = null }
  }
})
```

## 16. CSS 디자인 방향

K-POP AI 커버 라디오 서비스의 정서(추억/노스탤지어 + AI/미래지향 + 라디오/플레이어 감성)를 표현하기 위한 방향:

### 16.1 컬러 팔레트
- **베이스**: 다크 모드 우선 (`#0B0B14` ~ `#14141F` 배경) — 라디오/심야 감성
- **포인트(네온 그라디언트)**: 퍼플 → 핑크 → 시안 그라디언트 (`linear-gradient(135deg, #8A5CFF 0%, #FF5CA8 50%, #5CE1FF 100%)`)
- **텍스트**: 기본 `#F5F5FA`(거의 흰색), 보조 텍스트 `#A0A0B8`
- **강조 컬러**: 좋아요/하트 `#FF4D6D`, 성공 `#3DDC97`, 경고/에러 `#FF6B6B`
- **카드 배경**: `#1B1B2A` 반투명(`rgba(27,27,42,0.85)`) + 블러(backdrop-filter)로 글래스모피즘 느낌

### 16.2 배경 스타일
- 전체 배경에 미묘한 그라디언트 + 노이즈/그레인 텍스처
- 라디오 생성/결과 화면 상단에 음악 파형(waveform) 또는 LP판 회전 애니메이션 같은 장식 요소
- 별빛/그라디언트 블롭(blob) 애니메이션으로 "추억" + "감성" 분위기 강조

### 16.3 버튼 스타일
- Primary 버튼: 네온 그라디언트 배경 + 둥근 모서리(`border-radius: 999px`) + hover 시 글로우(box-shadow)
- Secondary 버튼: 투명 배경 + 그라디언트 보더(border)
- 아이콘 버튼(좋아요/재생): 원형, hover 시 스케일 확대 애니메이션

### 16.4 카드 UI
- 추천곡 카드: 좌측 유튜브 썸네일(16:9), 우측 제목/아티스트/태그(장르, 세대), 하단 추천 이유(작은 글씨), 우측 상단 좋아요 하트 + "플레이리스트 추가" 버튼
- 카드 hover 시 살짝 떠오르는 효과(translateY + shadow 강조)
- 플레이리스트 카드: 정사각형 썸네일 콜라주(곡 4개 썸네일을 2x2로 배치) + 카드 하단 이름/공개여부 배지

### 16.5 라디오 생성 화면 UI
- 단계형(스텝) 또는 한 화면 카드형 폼: 무드 선택(이모지+텍스트 칩), 시대/장르 토글 버튼 그룹, 사연 입력 textarea(글자수 표시)
- "라디오 만들기" 버튼은 화면 하단 고정(sticky), 로딩 중에는 LP판 회전 애니메이션으로 대기 화면 표현

### 16.6 추천 결과 카드 UI
- 상단에 DJ 멘트 카드: 아바타(DJ 캐릭터 일러스트) + 말풍선 텍스트 + 오디오 플레이어(파형 시각화)
- 추천곡 리스트는 세로 스크롤 카드 리스트, 각 카드에 유튜브 임베드(클릭 시 인라인 재생 또는 모달)

### 16.7 플레이리스트 UI
- 플레이리스트 상세는 음악 플레이어 스타일(좌측 큰 앨범아트/썸네일 + 우측 트랙리스트), 현재 재생곡 하이라이트
- 트랙리스트 각 행: 순서번호, 제목/아티스트, 장르/세대 태그, 삭제 아이콘(hover 시 노출)

### 16.8 모바일 반응형 방향
- 기본 모바일 퍼스트(360px~) 설계, 카드는 1열, 데스크탑(1024px+)에서 2~3열 그리드로 확장
- 하단 탭바(모바일): 홈/라디오 생성/플레이리스트/마이페이지
- 데스크탑: 상단 헤더 네비게이션 + 사이드 보조 패널(취향/필터)
- 오디오 플레이어(DJ 멘트, 곡 재생)는 모바일에서 화면 하단 고정 미니 플레이어 형태 권장

## 17. mock data 구조

```javascript
// mock/user.js
export const mockUser = {
  id: 'u001-0000-0000-0000-000000000001',
  nickname: '감성덕후',
  email: 'user1@example.com',
  provider: 'local'
}

export const mockAuthResponse = {
  accessToken: 'mock-access-token',
  refreshToken: 'mock-refresh-token',
  tokenType: 'Bearer',
  expiresIn: 3600000,
  user: mockUser
}

// mock/radio.js
export const mockRadioResult = {
  radioSessionId: 'f3a1b2c3-mock',
  userId: 'u001-0000-0000-0000-000000000001',
  mood: '그리운',
  story: '오늘 오래된 사진을 보다가 학창시절이 너무 그리워졌어요.',
  era: '2세대',
  genre: '댄스',
  situation: '퇴근길 지하철',
  desiredMood: '위로받고 싶음',
  videoType: '무대영상',
  preferredArtist: '',
  excludedKeywords: '',
  djMent: '안녕하세요, DJ 리아예요. 오늘 사연 잘 들었어요. 학창시절 추억을 떠올리며 이 곡들 들어보세요.',
  recommendationSource: 'DB_MOOD_ERA_GENRE',
  tts: { mode: 'google-tts', text: '안녕하세요, DJ 리아예요...', audioUrl: null },
  recommendedSongs: [
    {
      songId: 's011-0000-0000-0000-000000000011',
      title: '캔디 (AI 리마스터)',
      artist: 'H.O.T',
      era: '2세대',
      genre: '댄스',
      youtubeUrl: 'https://www.youtube.com/watch?v=dummy011',
      youtubeId: 'dummy011',
      score: 85.4,
      reason: '2000년대 초반 2세대 K-POP의 강한 후렴과 무대 감성이 있어 그리운 마음을 환기해줄 곡입니다.'
    },
    {
      songId: 's013-0000-0000-0000-000000000013',
      title: '여보세요 (AI 리마스터)',
      artist: '핑클',
      era: '2세대',
      genre: '댄스',
      youtubeUrl: 'https://www.youtube.com/watch?v=dummy013',
      youtubeId: 'dummy013',
      score: 78.6,
      reason: '학창시절 누구나 알던 명곡의 AI 리마스터로 추억 소환에 최적입니다.'
    }
  ]
}

// mock/playlist.js
export const mockPlaylists = [
  { id: 'pl01-0000-0000-0000-000000000001', userId: 'u001-...', name: '새벽 감성 모음', moodTag: '그리운', isPublic: true, createdAt: '2026-06-12T10:30:00', items: [] },
  { id: 'pl02-0000-0000-0000-000000000002', userId: 'u001-...', name: '출퇴근길 위로 플리', moodTag: '지친', isPublic: false, createdAt: '2026-06-10T08:00:00', items: [] }
]

export const mockPlaylistDetail = {
  id: 'pl01-0000-0000-0000-000000000001',
  userId: 'u001-...',
  name: '새벽 감성 모음',
  moodTag: '그리운',
  isPublic: true,
  createdAt: '2026-06-12T10:30:00',
  items: [
    { id: 'ps01-...', playlistId: 'pl01-...', songId: 's012-...', title: 'To Heaven (AI 리마스터)', artist: 'god', genre: '발라드', era: '00s', youtubeUrl: 'https://www.youtube.com/watch?v=dummy012', youtubeId: 'dummy012', orderNum: 1, addedAt: '2026-06-12T10:31:00' },
    { id: 'ps02-...', playlistId: 'pl01-...', songId: 's016-...', title: '고해 (AI 리마스터)', artist: '이소라', genre: '발라드', era: '00s', youtubeUrl: 'https://www.youtube.com/watch?v=dummy016', youtubeId: 'dummy016', orderNum: 2, addedAt: '2026-06-12T10:32:00' }
  ]
}

// mock/like.js
export const mockLikeStatus = { songId: 's011-0000-0000-0000-000000000011', liked: false, likeCount: 12 }
```

## 18. v0 또는 Vue UI 생성용 최종 프롬프트

```
서비스명: RevibeK (리바이브케이)
컨셉: 감정/상황 기반 K-POP AI 커버 라디오 추천 서비스. 사용자가 현재 기분/상황/시대/장르를
입력하면 AI DJ가 사연을 듣고 그에 맞는 K-POP(원곡 + AI 리믹스/커버) 추천 플레이리스트와
DJ 멘트(음성 포함)를 만들어주는 서비스입니다.

기술 스택: Vue 3 (Composition API, <script setup>), Vite, Vue Router, Pinia, Axios,
순수 HTML/CSS (Tailwind 또는 일반 CSS 둘 다 가능), 백엔드는 Spring Boot REST API
(base URL: http://localhost:8080, JWT Bearer 인증)

디자인 톤: 다크 모드 기반, 퍼플-핑크-시안 네온 그라디언트, 글래스모피즘 카드,
라디오/뮤직 플레이어 감성, 노스탤지어(추억) + 미래지향(AI) 대비, 둥근 모서리와 글로우 효과,
모바일 퍼스트 반응형

페이지 구성:
1. Landing(/) - 서비스 소개 + 시작하기 버튼
2. Login(/login) - 이메일/비밀번호 로그인 폼
3. Signup(/signup) - 닉네임/이메일/비밀번호 + 이메일 인증(코드 발송/검증) 단계형 폼
4. Onboarding(/onboarding) - 선호 세대/분위기/아티스트/장르/영상타입 칩 선택(선택사항)
5. RadioCreate(/radio/create) - 기분(이모지 칩), 사연(textarea), 시대/장르/상황/원하는분위기/
   영상타입/선호아티스트/제외키워드 입력 폼, "라디오 만들기" 버튼
6. RadioResult(/radio/result/:id) - 상단 DJ 멘트 카드(아바타+말풍선+오디오 플레이어),
   추천곡 카드 리스트(유튜브 썸네일, 제목/아티스트/장르/세대 태그, 추천이유, 좋아요 버튼,
   "플레이리스트에 추가" 버튼)
7. PlaylistList(/playlists) - 내 플레이리스트 카드 그리드 + 새 플레이리스트 생성 모달
8. PlaylistDetail(/playlists/:id) - 플레이어 스타일 레이아웃(좌측 앨범아트, 우측 트랙리스트)
9. MyPage(/me) - 프로필 카드, 정보 수정, 좋아요한 곡/보관함/라디오 히스토리 탭, 회원탈퇴

컴포넌트 구조: components/common(헤더,푸터,토스트,모달,로딩스피너), components/auth(로그인폼,
회원가입폼,이메일인증), components/radio(무드선택기,스토리입력,DJ멘트카드,추천곡카드,
플레이리스트추가모달), components/playlist(플레이리스트카드,트랙리스트,생성모달),
components/song(곡카드,좋아요버튼,유튜브플레이어모달), components/user(프로필카드,
프로필수정폼,라디오히스토리)

Mock 데이터: mock/user.js, mock/radio.js, mock/playlist.js, mock/like.js 형태로
실제 API 응답과 동일한 구조의 더미 데이터를 제공하세요. 컴포넌트는 이 mock 데이터로
독립적으로 렌더링/스토리북 테스트 가능해야 합니다.

API 연동 가정: api/axios.js(JWT 인터셉터+401 재발급), api/authApi.js, api/radioApi.js,
api/playlistApi.js, api/likeApi.js, api/songApi.js, api/userSongApi.js,
api/preferenceApi.js 모듈을 통해 실제 백엔드(Spring Boot, /api/auth, /api/users,
/api/radio, /api/playlists, /api/likes, /api/songs, /api/usersongs, /api/preferences)와
연동합니다.

로그인 후 사용자 흐름: 로그인 → (선택)취향 온보딩 → 라디오 생성 화면에서 기분/사연/시대/장르
입력 → "라디오 만들기" 클릭 → 결과 화면에서 AI DJ 멘트(텍스트+음성) 확인 → 추천곡 리스트에서
좋아요 누르기 / 유튜브 영상 재생 → "플레이리스트에 추가" 클릭 → 기존 플레이리스트 선택 또는
새 플레이리스트 생성 → 곡 저장 → 마이페이지에서 플레이리스트/좋아요곡/라디오 히스토리 확인

반응형: 모바일(360px~) 1열 카드, 태블릿/데스크탑(768px~, 1024px~) 2~3열 그리드,
모바일 하단 탭바 + 데스크탑 상단 헤더 네비게이션, 오디오 플레이어는 화면 하단 고정
미니 플레이어로 구현하세요.

위 내용을 기반으로 Vue 3 + Vite 프로젝트 구조(파일 트리), 각 페이지/컴포넌트의 HTML/CSS/JS
(Composition API, <script setup>) 코드를 생성해주세요. 실제 API가 연동되지 않은 상태에서도
mock 데이터로 모든 화면이 정상적으로 보이도록 작성하고, axios 모듈은 주석으로 실제 엔드포인트를
명시해주세요.
```

## 19. 구현 시 주의사항

### 19.1 인증/보안 관련
- **`/api/likes/**`, `/api/playlists/**`, `/api/radio/**`, `/api/usersongs/**`, `/api/users/me`(GET/PUT/DELETE)는 SecurityConfig상 `authenticated()`** 로 명시되어 있어 JWT 없이는 401이 발생한다. 프론트엔드는 이 경로 호출 전 반드시 로그인 상태를 확인해야 한다.
- 반면 **`/api/songs/**`, `/api/explore`, `/api/qdrant/**`, `/api/analysis/**`, `/api/ai/**`, `/api/youtube/**`, `/api/embeddings/**`, `/api/preferences/**`는 SecurityConfig의 `anyRequest().permitAll()` 규칙에 해당되어 인증 없이도 호출 가능**하다. 그러나 `PreferenceController`, `RadioController` 등은 `Authentication` 객체가 없으면 `resolveUserId()`에서 예외를 던지므로(`X-USER-ID` 헤더나 `userId` 쿼리파라미터로 우회 가능하지만 운영에서는 사용하지 말 것), 실질적으로는 로그인 후 사용을 전제로 설계해야 한다.
- `LikeController.getLikeCount()`는 메서드 내부에서 `Authentication`을 사용하지 않지만, 클래스 레벨 매핑(`/api/likes/**`)이 SecurityConfig상 authenticated이므로 비로그인 상태에서 호출하면 401이 발생할 가능성이 높다. 곡 카드에 좋아요 수를 비로그인 상태로 노출하려면 별도 public API가 필요하다(현재는 없음).
- 비밀번호 변경(예: "비밀번호 재설정/변경") API는 `UserController`/`UserUpdateRequestDto`에 존재하지 않는다. 마이페이지에서 비밀번호 변경 UI를 만들 경우 백엔드 추가 작업이 필요하다.
- OAuth2(구글) 로그인은 `app.oauth.google.enabled=false`가 기본값이며, 클라이언트ID/시크릿이 설정되어야만 `/auth/google/callback` 플로우가 활성화된다. 현재 상태에서는 구글 로그인 버튼을 노출하지 않거나, 비활성화 안내가 필요하다.

### 19.2 응답 형식 일관성
- `AuthController`, `UserController`의 다수 API가 **JSON 객체가 아닌 순수 문자열**을 반환한다("회원가입 완료", "수정 완료" 등). axios로 받으면 `response.data`가 문자열이 되므로, 이를 토대로 성공/실패를 판정하는 로직(상태코드 200 = 성공)을 적용해야 한다.
- 에러 응답은 `GlobalExceptionHandler`에 의해 `ErrorResponse`(timestamp, status, error, message, path, fieldErrors) 형태로 통일되어 있다. axios 인터셉터에서 `error.response.data.message` 및 `error.response.data.fieldErrors`를 활용해 폼 에러를 표시할 수 있다.
- `PreferenceController`, `EmbeddingController` 등 일부는 `ApiResponseDto<T>`(success, message, data) 래퍼를 사용하지만, `RadioController`, `PlaylistController`, `LikeController` 등 핵심 도메인은 래퍼 없이 DTO를 직접 반환한다. 프론트엔드 axios 래퍼 함수 작성 시 도메인별로 응답 구조가 다름을 인지해야 한다.

### 19.3 라디오/추천 관련
- `recommendedSongs`가 빈 배열로 반환될 수 있다(`recommendationSource: "DB_EMPTY"`). 이 경우 UI에서 "추천곡을 찾지 못했습니다" 등의 안내가 필요하다.
- `tts` 필드의 `audioUrl`/`audioContentBase64`는 `tts.enabled=false`(기본값) 또는 GCP TTS 설정 미완료 시 비어있을 수 있다(`TtsResponseDto`/`TtsFallbackResponseDto`의 `mode` 값으로 fallback 여부를 판별해야 함 — 실제 mode 값은 `TtsService`/`GoogleTtsService` 구현에 의존하므로, 프론트에서는 `audioUrl`이 null이면 텍스트만 표시하는 방어적 처리가 필요).
- `GET /api/radio/{id}` 호출 시 세션이 없거나 타인 소유면 `RuntimeException`이 발생해 **500 에러**로 응답된다(404가 아님). 프론트에서는 500 응답에서도 "세션을 찾을 수 없습니다" 식의 처리를 고려해야 한다.

### 19.4 플레이리스트/좋아요 관련
- "라디오 결과 → 플레이리스트 한 번에 저장"하는 전용 백엔드 API는 없다. 곡 개수만큼 `POST /api/playlists/{playlistId}/items`를 반복 호출해야 하므로, UI에서는 로딩 상태와 부분 실패(일부 곡만 추가됨) 처리를 고려해야 한다.
- `PlaylistDto.isPublic`은 응답에 포함되지만, "공개 플레이리스트 둘러보기"(다른 사용자의 공개 플레이리스트 탐색) API는 현재 컨트롤러에 존재하지 않는다. 공개 피드 화면을 만들 계획이 있다면 백엔드 추가가 필요하다.

### 19.5 CORS
- `SecurityConfig.corsConfigurationSource()`에서 `app.cors.allowed-origins` 기본값에 `http://localhost:3000`, `http://localhost:5173`, `http://127.0.0.1:3000`, `http://127.0.0.1:5173`이 포함되어 있어 Vite(5173) 또는 CRA(3000) 개발 서버 모두 별도 설정 없이 사용 가능하다. `allowCredentials(true)`이므로 axios에서 쿠키 기반 인증을 사용할 경우 `withCredentials: true`가 필요하지만, 현재는 JWT를 헤더로 전달하므로 필수는 아니다.
- 배포 환경에서는 `CORS_ALLOWED_ORIGINS` 환경변수로 실제 프론트엔드 도메인을 추가해야 한다.

### 19.6 외부 연동 의존성(기본 비활성화 항목)
- `YOUTUBE_ENABLED=true`이지만 `youtube.api.key`가 비어있으면 `youtube.fallback.mode=db`로 동작 — 즉 DB에 저장된 `youtube_url`/`youtube_id`를 그대로 사용한다(추천곡의 유튜브 링크는 schema의 mock 데이터처럼 `dummy0xx` 형태일 수 있음에 유의, 실제 데이터 적재 여부 확인 필요).
- `qdrant.enabled=false`, `tts.enabled=false`, `spotify.enabled=false`가 기본값이므로, 유사곡 벡터검색/TTS음성/Spotify era 보강 기능은 환경 설정이 안 되어 있으면 fallback 경로로만 동작한다. 프론트엔드는 이 기능들이 "있으면 좋은(nice-to-have)" 향상 기능으로 취급하고, 없어도 핵심 플로우(라디오 생성→추천→좋아요→플레이리스트)는 동작하도록 설계해야 한다.

### 19.7 FE 프로젝트 부재
- 현재 리포지토리에는 `FE/` 또는 Vue 프로젝트가 전혀 존재하지 않는다. 이 문서를 기반으로 신규 Vue 3 + Vite 프로젝트를 생성해야 하며, `package.json`, `vite.config.js`, `.env`(API base URL) 등을 처음부터 구성해야 한다.

## 20. 최종 판단

RevibeK 백엔드는 프론트엔드(Vue) 개발을 시작하기에 **충분히 준비된 상태**로 판단된다.

- **준비된 부분**: 회원가입/이메일인증/로그인/토큰재발급/로그아웃(JWT), 내 정보 조회/수정/탈퇴, 핵심 차별 기능인 AI 라디오 생성(추천곡+DJ멘트+TTS), 라디오 히스토리 조회, 좋아요(등록/취소/상태/목록), 플레이리스트(생성/목록/상세/곡추가/곡삭제/삭제), 곡 검색/조회/추천, 보관함(저장/별점/재생수) 등 "감정 입력 → AI 라디오 생성 → 추천곡 확인 → 좋아요 → 플레이리스트 저장 → 마이페이지" 핵심 플로우 전체가 실제 동작 가능한 API로 구현되어 있다.
- **부분적으로 준비된 부분**: TTS(음성), Qdrant 유사곡 검색, Spotify era 보강, YouTube 실데이터 연동은 기본 설정상 비활성화되어 있어, 데모/개발 환경에서는 fallback(텍스트만, DB 점수 기반 추천 등) 동작을 전제로 UI를 방어적으로 설계해야 한다.
- **미구현/추가 검토 필요**: 비밀번호 변경, 공개 플레이리스트 탐색(소셜 피드), "라디오 추천 일괄 저장" 전용 API, 비로그인 사용자를 위한 곡별 좋아요 수 공개 API 등은 현재 백엔드에 없으므로, 해당 기능을 프론트엔드 요구사항에 포함할 경우 백엔드 추가 개발이 선행되어야 한다.
- **결론**: 위 핵심 플로우를 우선 구현 대상으로 삼아 Vue 3 + Pinia + Axios 기반 SPA를 구축하면, 백엔드 추가 작업 없이도 RevibeK의 주요 사용자 경험(AI 커버 라디오 생성 → 추천/좋아요/플레이리스트)을 데모 가능한 수준으로 완성할 수 있다.
