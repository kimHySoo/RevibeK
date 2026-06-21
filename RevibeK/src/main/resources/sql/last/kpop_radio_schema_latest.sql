-- ============================================================
-- RevibeK (kpop_radio) — 통합 최신 스키마 (신규 DB 생성용)
-- 작성일: 2026-06-21
-- 스택: Spring Boot + MyBatis + MySQL 8.0
--
-- 이 파일은 다음을 한 파일로 통합한 것이다(실행 순서대로):
--   kpop_radio_schema.sql (베이스, DDL만 — 데모 mock data/GRANT는 제외)
--   + migration_add_radio_session_playlist_id.sql (playlist_id, 이미 베이스에 흡수됨)
--   + migration_add_radio_tts.sql (tts_* 컬럼, 이미 베이스에 흡수됨)
--   + migration_add_public_radio_feed.sql (is_public/published_at/radio_likes/challenge_type)
--   + migration_add_review_follow_plan_challenge.sql (reviews/follows/plans/challenges/challenge_participants)
--   + migration_add_mood_generation_genre_normalization.sql (moods/mood_aliases/song_moods/generations/genres)
--
-- 기존 운영 DB(이미 데이터가 있는 DB)에는 이 파일을 그대로 실행하지 말 것 —
-- "신규 DB 생성용"이며, 기존 DB 갱신은 migration_full_erd_normalization.sql을 사용한다.
-- ============================================================

CREATE DATABASE IF NOT EXISTS kpop_radio
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE kpop_radio;

-- ============================================================
-- 1. USERS
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
-- 2. SONGS
--    genre/era/generation/mood: 과도기 레거시 자유문자열 컬럼. 즉시 삭제하지 않음.
--    신규 추천 경로는 song_moods(N:M)를 우선 사용하고, 결과가 없을 때만 이
--    컬럼들로 폴백한다(RadioService.recommendSongs 참고).
-- ============================================================
CREATE TABLE songs (
  id                CHAR(36)        NOT NULL DEFAULT (UUID()),
  title             VARCHAR(200)    NOT NULL,
  artist            VARCHAR(100)    NOT NULL,
  genre             VARCHAR(50)     NOT NULL COMMENT '[과도기] 자유문자열. 표준 6코드는 genres.code/GenreNormalizer 참고',
  era               VARCHAR(20)     NOT NULL COMMENT '90s | 00s | 10s | 20s (released_at 기준)',
  generation        VARCHAR(20)     NULL     COMMENT '[과도기] 자유문자열(1~5세대 등). 표준 2코드(SECOND/THIRD)는 generations.code 참고. ALL은 절대 저장하지 않음',
  mood              VARCHAR(50)     NULL     COMMENT '[과도기] 쉼표구분 다중값 자유문자열. 신규 다중 감정은 song_moods 사용',
  type              VARCHAR(20)     NOT NULL COMMENT 'original | ai_remix | song',
  youtube_url       VARCHAR(300)    NOT NULL,
  youtube_id        VARCHAR(50)     NOT NULL COMMENT 'YouTube 영상 ID. UNIQUE는 기존 중복 데이터 점검 후 조건부 부여(db_audit_queries.sql 참고)',
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

-- ============================================================
-- 3. 감정/세대/장르 마스터 (정규화 신규 테이블)
-- ============================================================
CREATE TABLE moods (
    code        VARCHAR(30)  NOT NULL,
    label       VARCHAR(30)  NOT NULL,
    description VARCHAR(255) NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    PRIMARY KEY (code),
    UNIQUE KEY uk_moods_label (label)
) ENGINE=InnoDB COMMENT='감정 마스터, 정확히 7개 코드만 유지';

CREATE TABLE mood_aliases (
    alias     VARCHAR(30) NOT NULL,
    mood_code VARCHAR(30) NOT NULL,
    PRIMARY KEY (alias),
    CONSTRAINT fk_mood_aliases_mood FOREIGN KEY (mood_code) REFERENCES moods(code)
) ENGINE=InnoDB COMMENT='감정 동의어 -> 표준코드. Java MoodNormalizer와 1:1 동일 매핑 유지 필수';

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
) ENGINE=InnoDB COMMENT='곡<->감정 N:M. 서비스 정책상 곡당 최대 2개(SongServiceImpl.syncMoods에서 강제, DB 제약 아님)';

CREATE INDEX idx_song_moods_mood_song ON song_moods (mood_code, song_id);

-- ============================================================
-- 4. USER_SONGS / USER_PREFERENCES
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
  preferred_generations  JSON        NULL COMMENT '[현행유지] JSON 자유배열. 표준코드 검증은 애플리케이션 레벨(전면 정규화 보류, 이유는 answer 문서 참고)',
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
-- 5. PLAYLISTS / PLAYLIST_SONGS
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
  UNIQUE KEY uq_playlist_song (playlist_id, song_id) COMMENT '한 플레이리스트에 동일 곡 중복 추가 금지',
  FOREIGN KEY (playlist_id) REFERENCES playlists(id) ON DELETE CASCADE,
  FOREIGN KEY (song_id)     REFERENCES songs(id)     ON DELETE CASCADE,
  INDEX idx_playlist_order (playlist_id, order_num)
) ENGINE=InnoDB COMMENT='플레이리스트 구성 곡';

-- ============================================================
-- 6. RADIO_SESSIONS (playlist_id/TTS/공개피드 컬럼 모두 베이스에 포함)
--    mood/era/genre/desired_mood: 과도기 한글 자유문자열, 컬럼명/형식 변경 없음
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
  tts_mode           VARCHAR(30)  NULL COMMENT 'GOOGLE_TTS | BROWSER_TTS, 생성 당시 합성 결과',
  tts_audio_url      MEDIUMTEXT   NULL COMMENT 'Google TTS data URI(base64) 또는 정적 파일 URL. base64 오디오가 TEXT(64KB) 한도를 넘을 수 있어 MEDIUMTEXT 사용',
  tts_voice          VARCHAR(100) NULL COMMENT '합성에 사용된 voice 이름',
  tts_audio_encoding VARCHAR(30)  NULL COMMENT '합성 오디오 인코딩 (예: MP3, LINEAR16)',
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
  order_num   TINYINT     NOT NULL DEFAULT 1 COMMENT '추천 순서',
  reason      VARCHAR(200) NULL    COMMENT '추천 이유 (DJ 멘트 생성에 재활용)',
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
-- 7. SCORE_LOGS
-- ============================================================
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

-- ============================================================
-- 8. SONG_LIKES
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
-- 9. REVIEWS / FOLLOWS / PLANS / CHALLENGES / CHALLENGE_PARTICIPANTS
--    reviews: 동일 user_id+song_id 복수 리뷰 허용 여부가 코드상 불명확하여
--    UNIQUE(user_id, song_id)를 임의로 추가하지 않았다(answer 문서 "남은 제한사항" 참고).
-- ============================================================
CREATE TABLE reviews (
  id          CHAR(36)    NOT NULL DEFAULT (UUID()),
  user_id     CHAR(36)    NOT NULL,
  song_id     CHAR(36)    NOT NULL,
  content     TEXT        NOT NULL,
  rating      TINYINT     NULL     COMMENT '1~5점, NULL 허용',
  created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  FOREIGN KEY (user_id) REFERENCES users(id)  ON DELETE CASCADE,
  FOREIGN KEY (song_id) REFERENCES songs(id)  ON DELETE CASCADE,
  INDEX idx_review_song (song_id),
  INDEX idx_review_user (user_id)
) ENGINE=InnoDB COMMENT='곡 리뷰 / 감상평';

CREATE TABLE follows (
  follower_id  CHAR(36)  NOT NULL COMMENT '팔로우 하는 사람',
  followee_id  CHAR(36)  NOT NULL COMMENT '팔로우 받는 사람',
  created_at   DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (follower_id, followee_id),
  FOREIGN KEY (follower_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (followee_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_followee (followee_id)
  -- 자기 자신 팔로우 방지 CHECK(follower_id <> followee_id)는 MySQL 8.0.16+ 에서
  -- 지원되지만, 운영 MySQL 버전 확인 전이라 이번 작업에서는 추가하지 않음
  -- (db_audit_queries.sql에서 SELECT VERSION() 확인 후 별도 ALTER로 추가 권장).
) ENGINE=InnoDB COMMENT='팔로우 관계';

CREATE TABLE plans (
  id           CHAR(36)     NOT NULL DEFAULT (UUID()),
  user_id      CHAR(36)     NOT NULL,
  title        VARCHAR(200) NOT NULL,
  description  TEXT         NULL,
  plan_date    DATE         NULL     COMMENT '예정 감상 날짜',
  plan_type    VARCHAR(50)  NULL,
  created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_plan_user (user_id),
  INDEX idx_plan_date (plan_date)
) ENGINE=InnoDB COMMENT='사용자 음악 청취 계획 (현재 사용 현황은 answer 문서 참고)';

CREATE TABLE challenges (
  id           CHAR(36)     NOT NULL DEFAULT (UUID()),
  creator_id   CHAR(36)     NOT NULL COMMENT '챌린지 생성자',
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
) ENGINE=InnoDB COMMENT='음악 감상 챌린지 (현재 사용 현황은 answer 문서 참고)';

CREATE TABLE challenge_participants (
  id             CHAR(36)    NOT NULL DEFAULT (UUID()),
  challenge_id   CHAR(36)    NOT NULL,
  user_id        CHAR(36)    NOT NULL,
  current_count  INT         NOT NULL DEFAULT 0,
  target_count   INT         NOT NULL DEFAULT 1,
  status         VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS' COMMENT 'IN_PROGRESS | COMPLETED',
  joined_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_challenge_user (challenge_id, user_id),
  FOREIGN KEY (challenge_id) REFERENCES challenges(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id)      REFERENCES users(id)      ON DELETE CASCADE,
  INDEX idx_cp_user (user_id)
) ENGINE=InnoDB COMMENT='챌린지 참여자 및 진행도';

-- ============================================================
-- 10. YOUTUBE 수집 테이블
--     youtube_videos_raw.channel_id는 youtube_channels.id(PK, 시스템 UUID)가 아니라
--     youtube_channels.channel_id(외부 YouTube 채널 ID 문자열, UNIQUE)를 참조한다.
--     이름이 같아 FK처럼 안 보일 수 있으나 실제 운영 코드/DDL 기준으로 의도된 설계다.
-- ============================================================
CREATE TABLE youtube_channels (
  id                CHAR(36)      NOT NULL DEFAULT (UUID()),
  url               VARCHAR(255)  NOT NULL,
  channel_id        VARCHAR(50)   UNIQUE COMMENT '외부 YouTube 채널 ID. youtube_videos_raw.channel_id가 참조하는 대상',
  channel_name      VARCHAR(255),
  uploads_playlist  VARCHAR(60),
  subscriber_count  BIGINT,
  last_checked_at   DATETIME,
  created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='유튜브 채널 목록';

CREATE TABLE youtube_videos_raw (
  id            CHAR(36)      NOT NULL DEFAULT (UUID()),
  channel_id    VARCHAR(50)   NOT NULL COMMENT 'FK -> youtube_channels.channel_id (외부 ID, PK 아님)',
  video_id      VARCHAR(20)   NOT NULL UNIQUE,
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

-- ============================================================
-- 11. 초기 마스터 데이터 (감정 7 / 세대 2 / 장르 6, 정확한 개수 고정)
-- ============================================================
INSERT INTO moods (code, label) VALUES
('TIRED',      '지침'),
('EXCITED',    '설렘'),
('NOSTALGIC',  '회상'),
('CONFIDENT',  '자신감'),
('COMFORT',    '위로'),
('LONELY',     '외로움'),
('ENERGETIC',  '신남');

INSERT INTO mood_aliases (alias, mood_code) VALUES
('지침', 'TIRED'), ('지친', 'TIRED'), ('피곤함', 'TIRED'), ('피곤한', 'TIRED'), ('무기력', 'TIRED'), ('힘듦', 'TIRED'),
('설렘', 'EXCITED'), ('설레는', 'EXCITED'), ('설레임', 'EXCITED'),
('회상', 'NOSTALGIC'), ('그리움', 'NOSTALGIC'), ('그리운', 'NOSTALGIC'), ('추억', 'NOSTALGIC'), ('추억회상', 'NOSTALGIC'), ('과거회상', 'NOSTALGIC'), ('향수', 'NOSTALGIC'),
('자신감', 'CONFIDENT'), ('자신있는', 'CONFIDENT'), ('당당함', 'CONFIDENT'), ('당당한', 'CONFIDENT'), ('파이팅', 'CONFIDENT'),
('위로', 'COMFORT'), ('힐링', 'COMFORT'), ('편안함', 'COMFORT'), ('안정', 'COMFORT'), ('따뜻함', 'COMFORT'),
('외로움', 'LONELY'), ('외로운', 'LONELY'), ('쓸쓸함', 'LONELY'), ('쓸쓸한', 'LONELY'), ('고독', 'LONELY'),
('신남', 'ENERGETIC'), ('신나는', 'ENERGETIC'), ('흥겨움', 'ENERGETIC'), ('즐거움', 'ENERGETIC'), ('행복한', 'ENERGETIC'), ('활기', 'ENERGETIC');

INSERT INTO generations (code, label) VALUES
('SECOND', '2세대'),
('THIRD',  '3세대');

INSERT INTO genres (code, label) VALUES
('DANCE',  '댄스'),
('BALLAD', '발라드'),
('RNB',    'R&B'),
('HIPHOP', '힙합'),
('IDOL',   '아이돌'),
('OST',    'OST');
