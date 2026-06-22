-- ============================================================
-- RevibeK (kpop_radio) -- 2차 개편 스키마 DDL (신규 DB 생성용)
-- 작성일: 2026-06-21
-- 스택: Spring Boot + MyBatis + MySQL 8.0
--
-- 실행 순서:
--   1) schema_v2_ddl.sql          (이 파일 -- 테이블 구조)
--   2) schema_v2_seed_data.sql    (마스터/참조 데이터, 기존 demo 데이터)
--   3) schema_v2_songs_load.sql   (songs 전체 교체 + song_moods 시드)
--
-- 자세한 변경 배경/근거는 docs/answer/erd2.md 참고.
--
-- 1차(현행) 스키마 대비 주요 변경점:
--   - songs: score, trend_score, score_updated_at, beats_count, beats_confidence,
--            key_strength 컬럼 제거 (점수 기반 랭킹 폐기, 추천은 embedding/vector 기반으로 전환).
--            spectral_centroid/zero_crossing_rate는 SongVectorUtil.toVector()(Qdrant 9D
--            오디오 임베딩 생성)와 AnalysisServiceImpl이 실제로 읽고 쓰는 컬럼이라 songs에 유지함
--            (당초 erd2.md 초안에서는 함께 제거 대상으로 분류했으나, 코드 의존성 확인 후 정정).
--            mood 컬럼도 같은 이유로 유지함: SongMapper.xml의 레거시 폴백 추천 쿼리
--            (findRecommendedSongsByMood* 등, "song_moods에 결과 없을 때만 폴백"하는 경로)와
--            LikeMapper.xml/insertSong/updateSong이 songs.mood를 실제 컬럼으로 가정하고 있어,
--            제거 시 "Unknown column 's.mood'" SQL 에러가 발생함을 런타임에서 확인하고 정정함
--            (docs/error/error04.md 참고).
--   - score_logs 테이블 제거 (songs.score 제거에 따라 더 이상 의미 없음, 코드 참조 없음 확인됨)
--   - mood_aliases 테이블 제거 (Java MoodNormalizer.java의 정적 Map과 완전히 동일한 내용을
--     DB에 중복 보관하고 있었을 뿐, 어떤 Mapper/Service도 이 테이블을 SELECT하지 않음)
--   - youtube_videos_raw: FastAPI 분석 상태 추적용 컬럼은 유지하되, 분석 결과 자체는
--     신규 analyzed_songs로 분리 저장 (raw 영상 메타와 분석 결과의 책임 분리)
--   - analyzed_songs(신규): FastAPI/Librosa 분석 결과 저장, youtube_videos_raw와 1:1 연결
--   - embedding_songs(신규): 곡별 embedding 메타데이터 저장 (벡터 본체는 Qdrant가 source of
--     truth이며 MySQL에는 모델명/차원/Qdrant point id/파일 경로만 저장)
-- ============================================================

CREATE DATABASE IF NOT EXISTS kpop_radio_2
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE kpop_radio_2;

-- mysql 클라이언트가 latin1로 접속해 UTF-8 데이터를 이중 인코딩하는 문제 방지(docs/error/error08.md)
SET NAMES utf8mb4;

-- ============================================================
-- 1. USERS (변경 없음)
-- ============================================================
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

-- ============================================================
-- 2. SONGS (score/trend_score 및 분석 부가 컬럼 제거, embedding 기반 추천으로 단순화)
--    genre/era/generation: 과도기 자유문자열 유지(기존 코드 호환). 표준 코드는
--    genres.code/generations.code, 다중 감정은 song_moods 사용.
-- ============================================================
CREATE TABLE songs (
  id                CHAR(36)        NOT NULL DEFAULT (UUID()),
  title             VARCHAR(200)    NOT NULL,
  artist            VARCHAR(100)    NOT NULL,
  genre             VARCHAR(50)     NOT NULL COMMENT '[과도기] 자유문자열. 표준 6코드는 genres.code 참고',
  era               VARCHAR(20)     NOT NULL COMMENT '90s | 00s | 10s | 20s (released_at 기준)',
  generation        VARCHAR(20)     NULL     COMMENT '[과도기] 자유문자열(2/3세대). 표준코드는 generations.code. ALL은 저장 금지',
  mood              VARCHAR(50)     NULL     COMMENT '[과도기] 쉼표구분 다중값 자유문자열. SongMapper.xml의 레거시 폴백 추천 쿼리(findRecommendedSongsByMood* 등)가 의존함. 신규 다중 감정은 song_moods 사용',
  type              VARCHAR(20)     NOT NULL DEFAULT 'song' COMMENT 'original | ai_remix | song',
  youtube_url       VARCHAR(300)    NOT NULL,
  youtube_id        VARCHAR(50)     NOT NULL COMMENT 'YouTube 영상 ID',
  thumbnail_url     VARCHAR(500)    NULL,
  view_count        INT             NOT NULL DEFAULT 0,
  like_count        INT             NOT NULL DEFAULT 0,
  released_at       DATE            NULL,
  duration_seconds  INT             NULL,
  bpm               DOUBLE          NULL,
  energy            DOUBLE          NULL,
  danceability      DOUBLE          NULL,
  loudness          DOUBLE          NULL,
  musical_key       VARCHAR(10)     NULL,
  musical_scale     VARCHAR(20)     NULL,
  spectral_centroid DOUBLE          NULL COMMENT 'SongVectorUtil 9D 임베딩 8번째 차원에 사용',
  zero_crossing_rate DOUBLE         NULL COMMENT 'SongVectorUtil 9D 임베딩 9번째 차원에 사용',
  is_analyzed       TINYINT(1)      NOT NULL DEFAULT 0 COMMENT 'analyzed_songs 결과가 songs에 반영되었는지',
  created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_songs_youtube_id (youtube_id),
  INDEX idx_type (type),
  INDEX idx_genre (genre),
  INDEX idx_era (era),
  INDEX idx_generation (generation),
  INDEX idx_analyzed (is_analyzed)
) ENGINE=InnoDB COMMENT='원곡 및 AI 리믹스 노래 정보 (2차 개편: score/trend_score 제거)';

-- ============================================================
-- 3. 감정/세대/장르 마스터
--    mood_aliases는 제거됨 (MoodNormalizer.java 정적 Map과 중복, 코드 미참조 확인됨)
-- ============================================================
CREATE TABLE moods (
    code        VARCHAR(30)  NOT NULL,
    label       VARCHAR(30)  NOT NULL,
    description VARCHAR(255) NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    PRIMARY KEY (code),
    UNIQUE KEY uk_moods_label (label)
) ENGINE=InnoDB COMMENT='감정 마스터, 정확히 7개 코드만 유지';

CREATE TABLE generations (
    code   VARCHAR(20) NOT NULL,
    label  VARCHAR(20) NOT NULL,
    active BOOLEAN     NOT NULL DEFAULT TRUE,
    PRIMARY KEY (code),
    UNIQUE KEY uk_generations_label (label)
) ENGINE=InnoDB COMMENT='세대 마스터. SECOND/THIRD 2개만. ALL은 검색 필터 전용, 저장 금지';

CREATE TABLE genres (
    code   VARCHAR(20) NOT NULL,
    label  VARCHAR(20) NOT NULL,
    active BOOLEAN     NOT NULL DEFAULT TRUE,
    PRIMARY KEY (code),
    UNIQUE KEY uk_genres_label (label)
) ENGINE=InnoDB COMMENT='장르 마스터, 정확히 6개 코드만 유지';

CREATE TABLE song_moods (
    song_id   CHAR(36)    NOT NULL,
    mood_code VARCHAR(30) NOT NULL,
    PRIMARY KEY (song_id, mood_code),
    CONSTRAINT fk_song_moods_song FOREIGN KEY (song_id) REFERENCES songs(id) ON DELETE CASCADE,
    CONSTRAINT fk_song_moods_mood FOREIGN KEY (mood_code) REFERENCES moods(code)
) ENGINE=InnoDB COMMENT='곡<->감정 N:M. 서비스 정책상 곡당 최대 2개(SongServiceImpl.syncMoods 강제, DB 제약 아님)';

CREATE INDEX idx_song_moods_mood_song ON song_moods (mood_code, song_id);

-- ============================================================
-- 4. USER_SONGS / USER_PREFERENCES (변경 없음)
-- ============================================================
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

CREATE TABLE user_preferences (
  id                     CHAR(36)    NOT NULL DEFAULT (UUID()),
  user_id                CHAR(36)    NOT NULL,
  preferred_generations  JSON        NULL,
  preferred_moods        JSON        NULL,
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

-- ============================================================
-- 5. PLAYLISTS / PLAYLIST_SONGS (변경 없음)
-- ============================================================
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

-- ============================================================
-- 6. RADIO_SESSIONS / RADIO_RECOMMENDATIONS / RADIO_LIKES (변경 없음)
-- ============================================================
CREATE TABLE radio_sessions (
  id              CHAR(36)    NOT NULL DEFAULT (UUID()),
  user_id         CHAR(36)    NOT NULL,
  mood            VARCHAR(50) NOT NULL COMMENT '[과도기] 한글 자유문자열 유지',
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
  tts_mode           VARCHAR(30)  NULL COMMENT 'GOOGLE_TTS | BROWSER_TTS',
  tts_audio_url      MEDIUMTEXT   NULL COMMENT 'base64 data URI 또는 정적 파일 URL',
  tts_voice          VARCHAR(100) NULL,
  tts_audio_encoding VARCHAR(30)  NULL,
  playlist_id     CHAR(36)    NULL,
  is_public       BOOLEAN     NOT NULL DEFAULT FALSE,
  published_at    DATETIME    NULL,
  created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_radio_sessions_playlist FOREIGN KEY (playlist_id) REFERENCES playlists(id) ON DELETE SET NULL,
  INDEX idx_user_session (user_id, created_at DESC),
  INDEX idx_mood (mood),
  INDEX idx_radio_era_genre (era, genre),
  INDEX idx_radio_desired_mood (desired_mood),
  INDEX idx_radio_sessions_public (is_public, published_at)
) ENGINE=InnoDB COMMENT='AI 라디오 세션 이력';

CREATE TABLE radio_recommendations (
  id          CHAR(36)    NOT NULL DEFAULT (UUID()),
  session_id  CHAR(36)    NOT NULL,
  song_id     CHAR(36)    NOT NULL,
  order_num   TINYINT     NOT NULL DEFAULT 1,
  reason      VARCHAR(200) NULL,
  PRIMARY KEY (id),
  FOREIGN KEY (session_id) REFERENCES radio_sessions(id) ON DELETE CASCADE,
  FOREIGN KEY (song_id)    REFERENCES songs(id) ON DELETE CASCADE,
  INDEX idx_session (session_id, order_num)
) ENGINE=InnoDB COMMENT='라디오 세션 추천 곡 목록';

CREATE TABLE radio_likes (
    radio_session_id CHAR(36) NOT NULL,
    user_id          CHAR(36) NOT NULL,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (radio_session_id, user_id),
    FOREIGN KEY (radio_session_id) REFERENCES radio_sessions(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id)          REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_radio_likes_user    (user_id),
    INDEX idx_radio_likes_session (radio_session_id)
) ENGINE=InnoDB COMMENT='라디오 사연 좋아요';

-- ============================================================
-- 7. SONG_LIKES (변경 없음). score_logs는 제거됨 (코드 미참조 확인, erd2.md 참고)
-- ============================================================
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

-- ============================================================
-- 8. REVIEWS / FOLLOWS / PLANS / CHALLENGES / CHALLENGE_PARTICIPANTS (변경 없음)
--    모두 Controller/Service/Mapper 코드가 실제로 존재함 (검토 결과는 erd2.md 참고)
-- ============================================================
CREATE TABLE reviews (
  id          CHAR(36)    NOT NULL DEFAULT (UUID()),
  user_id     CHAR(36)    NOT NULL,
  song_id     CHAR(36)    NOT NULL,
  content     TEXT        NOT NULL,
  rating      TINYINT     NULL,
  created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  FOREIGN KEY (user_id) REFERENCES users(id)  ON DELETE CASCADE,
  FOREIGN KEY (song_id) REFERENCES songs(id)  ON DELETE CASCADE,
  INDEX idx_review_song (song_id),
  INDEX idx_review_user (user_id)
) ENGINE=InnoDB COMMENT='곡 리뷰 / 감상평';

CREATE TABLE follows (
  follower_id  CHAR(36)  NOT NULL,
  followee_id  CHAR(36)  NOT NULL,
  created_at   DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (follower_id, followee_id),
  FOREIGN KEY (follower_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (followee_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_followee (followee_id)
) ENGINE=InnoDB COMMENT='팔로우 관계';

CREATE TABLE plans (
  id           CHAR(36)     NOT NULL DEFAULT (UUID()),
  user_id      CHAR(36)     NOT NULL,
  title        VARCHAR(200) NOT NULL,
  description  TEXT         NULL,
  plan_date    DATE         NULL,
  plan_type    VARCHAR(50)  NULL,
  created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_plan_user (user_id),
  INDEX idx_plan_date (plan_date)
) ENGINE=InnoDB COMMENT='사용자 음악 청취 계획 (PlanController/Service/Mapper에서 실사용 확인됨)';

CREATE TABLE challenges (
  id           CHAR(36)     NOT NULL DEFAULT (UUID()),
  creator_id   CHAR(36)     NOT NULL,
  title        VARCHAR(200) NOT NULL,
  description  TEXT         NULL,
  start_date   DATE         NULL,
  end_date     DATE         NULL,
  target_count INT          NOT NULL DEFAULT 1,
  challenge_type VARCHAR(50) NULL,
  created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  FOREIGN KEY (creator_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_challenge_creator (creator_id)
) ENGINE=InnoDB COMMENT='음악 감상 챌린지';

CREATE TABLE challenge_participants (
  id             CHAR(36)    NOT NULL DEFAULT (UUID()),
  challenge_id   CHAR(36)    NOT NULL,
  user_id        CHAR(36)    NOT NULL,
  current_count  INT         NOT NULL DEFAULT 0,
  target_count   INT         NOT NULL DEFAULT 1,
  status         VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
  joined_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_challenge_user (challenge_id, user_id),
  FOREIGN KEY (challenge_id) REFERENCES challenges(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id)      REFERENCES users(id)      ON DELETE CASCADE,
  INDEX idx_cp_user (user_id)
) ENGINE=InnoDB COMMENT='챌린지 참여자 및 진행도';

-- ============================================================
-- 9. YOUTUBE 수집 -> 분석 -> 임베딩 파이프라인
--    youtube_channels -> youtube_videos_raw -> analyzed_songs -> embedding_songs
-- ============================================================
CREATE TABLE youtube_channels (
  id                CHAR(36)      NOT NULL DEFAULT (UUID()),
  url               VARCHAR(255)  NOT NULL,
  channel_id        VARCHAR(50)   UNIQUE COMMENT '외부 YouTube 채널 ID. youtube_videos_raw.channel_id가 참조',
  channel_name      VARCHAR(255),
  uploads_playlist  VARCHAR(60),
  subscriber_count  BIGINT,
  last_checked_at   DATETIME,
  created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='유튜브 채널 목록 (수집 대상)';

CREATE TABLE youtube_videos_raw (
  id            CHAR(36)      NOT NULL DEFAULT (UUID()),
  channel_id    VARCHAR(50)   NOT NULL COMMENT 'FK -> youtube_channels.channel_id (외부 ID, PK 아님)',
  video_id      VARCHAR(20)   NOT NULL UNIQUE,
  video_url     VARCHAR(300),
  title         VARCHAR(500),
  duration_seconds INT        NULL,
  published_at  DATETIME,
  is_imported   TINYINT(1)    NOT NULL DEFAULT 0  COMMENT 'songs 테이블 반영 여부',
  is_analyzed   TINYINT(1)    NOT NULL DEFAULT 0  COMMENT 'analyzed_songs 적재 여부',
  collect_status VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
  fetched_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  FOREIGN KEY (channel_id) REFERENCES youtube_channels(channel_id),
  INDEX idx_imported (is_imported),
  INDEX idx_analyzed (is_analyzed),
  INDEX idx_collect_status (collect_status),
  INDEX idx_published (published_at DESC)
) ENGINE=InnoDB COMMENT='유튜브 영상 수집 대기열 (분석 전 raw queue)';

-- ------------------------------------------------------------
-- analyzed_songs (신규)
-- FastAPI/Librosa(Essentia) 분석 결과를 youtube_videos_raw와 1:1로 저장.
-- RevibeK_AI/backup/{video_id}.json 구조 기준 (status/message/essentia_features).
-- ------------------------------------------------------------
CREATE TABLE analyzed_songs (
  id                  CHAR(36)      NOT NULL DEFAULT (UUID()),
  youtube_video_raw_id CHAR(36)     NOT NULL COMMENT 'FK -> youtube_videos_raw.id',
  youtube_video_id    VARCHAR(20)   NOT NULL COMMENT 'youtube_videos_raw.video_id 비정규화(분석 백업 JSON 파일명과 매칭용)',
  song_id             CHAR(36)      NULL     COMMENT 'songs로 import된 이후 채워짐 (FK -> songs.id)',
  status              VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING | COMPLETED | FAILED',
  message             VARCHAR(255)  NULL     COMMENT 'FastAPI 응답 메시지 (실패 사유 포함)',
  duration_seconds    DOUBLE        NULL,
  bpm                 DOUBLE        NULL,
  energy              DOUBLE        NULL,
  danceability        DOUBLE        NULL,
  loudness            DOUBLE        NULL,
  musical_key         VARCHAR(10)   NULL,
  musical_scale       VARCHAR(20)   NULL,
  beats_count         INT           NULL,
  beats_confidence    DOUBLE        NULL,
  key_strength        DOUBLE        NULL,
  spectral_centroid   DOUBLE        NULL,
  zero_crossing_rate  DOUBLE        NULL,
  analyzed_at         DATETIME      NULL     COMMENT 'FastAPI 분석 완료 시각',
  created_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_analyzed_songs_raw (youtube_video_raw_id),
  FOREIGN KEY (youtube_video_raw_id) REFERENCES youtube_videos_raw(id) ON DELETE CASCADE,
  FOREIGN KEY (song_id) REFERENCES songs(id) ON DELETE SET NULL,
  INDEX idx_analyzed_songs_video_id (youtube_video_id),
  INDEX idx_analyzed_songs_status (status),
  INDEX idx_analyzed_songs_song (song_id)
) ENGINE=InnoDB COMMENT='FastAPI/Librosa 음향 분석 결과 (RevibeK_AI/backup 동기화 대상)';

-- ------------------------------------------------------------
-- embedding_songs (신규)
-- 곡별 embedding 메타데이터 + 벡터 본체. MySQL이 단일 소스(source of truth)이고
-- Qdrant는 색인 캐시다(answer16.md — 벡터가 로컬 파일/볼륨에만 있어 배포 시 깨지는 문제 해결).
-- RevibeK_AI/song_embeddings/{songId}.json (model=text-embedding-3-small, 1536dim)
-- 과 Spring Boot 9D 오디오 임베딩(SongVectorUtil, revibek_songs 컬렉션) 둘 다 기록 가능하도록
-- embedding_type으로 구분한다.
-- ------------------------------------------------------------
CREATE TABLE embedding_songs (
  id                CHAR(36)      NOT NULL DEFAULT (UUID()),
  song_id           CHAR(36)      NOT NULL COMMENT 'FK -> songs.id',
  embedding_type     VARCHAR(20)   NOT NULL COMMENT 'AUDIO_9D | TEXT_OPENAI 등',
  model_name        VARCHAR(100)  NOT NULL COMMENT '예: text-embedding-3-small, SongVectorUtil(internal)',
  dimension         INT           NOT NULL COMMENT '벡터 차원 (9, 1536 등)',
  source_text       TEXT          NULL     COMMENT 'TEXT 계열 임베딩 입력 원문 (제목+가수+장르 등)',
  vector            JSON          NOT NULL COMMENT '임베딩 벡터 배열 본체. 예: [0.0028, -0.0217, ...]',
  qdrant_collection VARCHAR(100)  NOT NULL COMMENT '예: revibek_songs, revibek_song_text_embeddings',
  qdrant_point_id   CHAR(36)      NOT NULL COMMENT 'Qdrant point id. 현재 정책상 songs.id(UUID)와 동일값',
  is_indexed        TINYINT(1)    NOT NULL DEFAULT 0 COMMENT 'Qdrant upsert 성공 여부',
  indexed_at        DATETIME      NULL,
  created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_embedding_songs_song_type (song_id, embedding_type),
  FOREIGN KEY (song_id) REFERENCES songs(id) ON DELETE CASCADE,
  INDEX idx_embedding_songs_collection (qdrant_collection),
  INDEX idx_embedding_songs_indexed (is_indexed)
) ENGINE=InnoDB COMMENT='곡 embedding 메타데이터 + 벡터 본체 (MySQL이 단일 소스, Qdrant는 색인 캐시)';
