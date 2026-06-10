-- ============================================
-- K-POP AI 라디오 서비스 — MySQL Schema + Mock Data
-- 작성일: 2025-05-22
-- 스택: Spring Boot + MyBatis + MySQL 8.0
-- ============================================

-- DB 생성
CREATE DATABASE IF NOT EXISTS kpop_radio
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE kpop_radio;

-- ============================================
-- DDL — 테이블 생성
-- ============================================

-- 1. USERS
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


-- 2. SONGS
CREATE TABLE songs (
  id                CHAR(36)        NOT NULL DEFAULT (UUID()),
  title             VARCHAR(200)    NOT NULL,
  artist            VARCHAR(100)    NOT NULL,
  genre             VARCHAR(50)     NOT NULL COMMENT '발라드 | 댄스 | 힙합 | R&B | 록',
  era               VARCHAR(20)     NOT NULL COMMENT '90s | 00s | 10s | 20s',
  type              VARCHAR(20)     NOT NULL COMMENT 'original | ai_remix',
  youtube_url       VARCHAR(300)    NOT NULL,
  youtube_id        VARCHAR(50)     NOT NULL COMMENT 'YouTube 영상 ID',
  view_count        INT             NOT NULL DEFAULT 0,
  like_count        INT             NOT NULL DEFAULT 0,
  trend_score       FLOAT           NOT NULL DEFAULT 0.0 COMMENT '최근 7일 증가율 기반',
  score             FLOAT           NOT NULL DEFAULT 0.0 COMMENT '가중 합산 점수 (0~100)',
  score_updated_at  DATETIME        NULL,
  released_at       DATE            NULL,
  created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_type (type),
  INDEX idx_genre (genre),
  INDEX idx_era (era),
  INDEX idx_score (score DESC),
  INDEX idx_youtube_id (youtube_id)
) ENGINE=InnoDB COMMENT='원곡 및 AI 리믹스 노래 정보';


-- 3. USER_SONGS (저장 / 평가 / 재생 이력)
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


-- 4. RADIO_SESSIONS (라디오 생성 이력)
CREATE TABLE radio_sessions (
  id              CHAR(36)    NOT NULL DEFAULT (UUID()),
  user_id         CHAR(36)    NOT NULL,
  mood            VARCHAR(50) NOT NULL COMMENT '외로운 | 설레는 | 그리운 | 지친 | 행복한 | 슬픈',
  story           TEXT        NULL,
  dj_ment        TEXT        NULL     COMMENT 'Claude API 생성 DJ 멘트',
  comfort_text    TEXT        NULL     COMMENT 'AI 위로 메시지',
  novel_excerpt   TEXT        NULL     COMMENT '활용된 소설 구절',
  created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_user_session (user_id, created_at DESC),
  INDEX idx_mood (mood)
) ENGINE=InnoDB COMMENT='AI 라디오 세션 이력';


-- 5. RADIO_RECOMMENDATIONS (라디오 추천 곡)
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


-- 6. SCORE_LOGS (점수 변경 이력)
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


-- 7. PLAYLISTS
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


-- 8. PLAYLIST_SONGS
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

-- 9. 채널 테이블
CREATE TABLE youtube_channels (
  id                CHAR(36)      NOT NULL DEFAULT (UUID()),
  url               VARCHAR(255)  NOT NULL,
  channel_id        VARCHAR(50)   UNIQUE,
  channel_name      VARCHAR(255),
  uploads_playlist  VARCHAR(60),
  subscriber_count  BIGINT,
  last_checked_at   DATETIME,
  created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='유튜브 채널 목록';

-- 10. 크롤링된 영상 후보 테이블 (songs 반영 전 대기열)
CREATE TABLE youtube_videos_raw (
  id            CHAR(36)      NOT NULL DEFAULT (UUID()),
  channel_id    VARCHAR(50)   NOT NULL,
  video_id      VARCHAR(20)   NOT NULL UNIQUE,  -- 중복 방지
  title         VARCHAR(500),
  published_at  DATETIME,
  is_imported   TINYINT(1)    NOT NULL DEFAULT 0  COMMENT 'songs 테이블 반영 여부',
  fetched_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  FOREIGN KEY (channel_id) REFERENCES youtube_channels(channel_id),
  INDEX idx_imported (is_imported),
  INDEX idx_published (published_at DESC)
) ENGINE=InnoDB COMMENT='유튜브 영상 수집 대기열';

-- ============================================
-- MOCK DATA — 목업 데이터 INSERT
-- ============================================

-- ① USERS (5명)
INSERT INTO users (id, nickname, email, provider, provider_id, password_hash) VALUES
  ('u001-0000-0000-0000-000000000001', '감성덕후', 'user1@example.com', 'google',  'g_001', NULL),
  ('u002-0000-0000-0000-000000000002', '새벽세시', 'user2@example.com', 'kakao',   'k_002', NULL),
  ('u003-0000-0000-0000-000000000003', '레트로킹', 'user3@example.com', 'local',   NULL,    '$2a$10$mockHashValue1'),
  ('u004-0000-0000-0000-000000000004', '별빛수집가', 'user4@example.com','google', 'g_004', NULL),
  ('u005-0000-0000-0000-000000000005', '추억여행자', 'user5@example.com','local',  NULL,    '$2a$10$mockHashValue2');
show tables;

-- ② SONGS (원곡 10곡 + AI 리믹스 10곡)
INSERT INTO songs (id, title, artist, genre, era, type, youtube_url, youtube_id, view_count, like_count, trend_score, score, score_updated_at, released_at) VALUES

-- 원곡
('s001-0000-0000-0000-000000000001', '캔디', 'H.O.T', '댄스', '90s', 'original',
 'https://www.youtube.com/watch?v=dummy001', 'dummy001', 4500000, 82000, 55.0, 72.5, NOW(), '1996-09-15'),

('s002-0000-0000-0000-000000000002', 'To Heaven', 'god', '발라드', '00s', 'original',
 'https://www.youtube.com/watch?v=dummy002', 'dummy002', 3800000, 95000, 48.0, 75.2, NOW(), '2001-05-10'),

('s003-0000-0000-0000-000000000003', '여보세요', '핑클', '댄스', '90s', 'original',
 'https://www.youtube.com/watch?v=dummy003', 'dummy003', 2900000, 71000, 42.0, 65.8, NOW(), '1998-10-01'),

('s004-0000-0000-0000-000000000004', '내 사람', 'SG워너비', '발라드', '00s', 'original',
 'https://www.youtube.com/watch?v=dummy004', 'dummy004', 5200000, 98000, 52.0, 78.1, NOW(), '2004-03-22'),

('s005-0000-0000-0000-000000000005', '가시', '버스커버스커', '발라드', '10s', 'original',
 'https://www.youtube.com/watch?v=dummy005', 'dummy005', 8900000, 155000, 70.0, 88.3, NOW(), '2012-03-29'),

('s006-0000-0000-0000-000000000006', '고해', '이소라', '발라드', '00s', 'original',
 'https://www.youtube.com/watch?v=dummy006', 'dummy006', 2100000, 88000, 38.0, 62.4, NOW(), '2003-11-05'),

('s007-0000-0000-0000-000000000007', '쿵따리샤바라', '클론', '댄스', '90s', 'original',
 'https://www.youtube.com/watch?v=dummy007', 'dummy007', 3300000, 67000, 61.0, 69.7, NOW(), '1996-06-01'),

('s008-0000-0000-0000-000000000008', '기사도', '젝스키스', '댄스', '90s', 'original',
 'https://www.youtube.com/watch?v=dummy008', 'dummy008', 2600000, 59000, 45.0, 63.2, NOW(), '1997-04-10'),

('s009-0000-0000-0000-000000000009', '인연', '이선희', '발라드', '00s', 'original',
 'https://www.youtube.com/watch?v=dummy009', 'dummy009', 6100000, 112000, 58.0, 82.6, NOW(), '2006-08-15'),

('s010-0000-0000-0000-000000000010', '여수 밤바다', '버스커버스커', '발라드', '10s', 'original',
 'https://www.youtube.com/watch?v=dummy010', 'dummy010', 7700000, 143000, 66.0, 85.9, NOW(), '2012-03-29'),

-- AI 리믹스
('s011-0000-0000-0000-000000000011', '캔디 (AI 리마스터)', 'H.O.T', '댄스', '90s', 'ai_remix',
 'https://www.youtube.com/watch?v=dummy011', 'dummy011', 1200000, 48000, 88.0, 85.4, NOW(), '2024-01-10'),

('s012-0000-0000-0000-000000000012', 'To Heaven (AI 리마스터)', 'god', '발라드', '00s', 'ai_remix',
 'https://www.youtube.com/watch?v=dummy012', 'dummy012', 980000, 52000, 91.0, 87.2, NOW(), '2024-02-14'),

('s013-0000-0000-0000-000000000013', '여보세요 (AI 리마스터)', '핑클', '댄스', '90s', 'ai_remix',
 'https://www.youtube.com/watch?v=dummy013', 'dummy013', 750000, 38000, 79.0, 78.6, NOW(), '2024-01-25'),

('s014-0000-0000-0000-000000000014', '내 사람 (AI 리마스터)', 'SG워너비', '발라드', '00s', 'ai_remix',
 'https://www.youtube.com/watch?v=dummy014', 'dummy014', 890000, 45000, 82.0, 81.3, NOW(), '2024-03-05'),

('s015-0000-0000-0000-000000000015', '가시 (AI 리마스터)', '버스커버스커', '발라드', '10s', 'ai_remix',
 'https://www.youtube.com/watch?v=dummy015', 'dummy015', 1500000, 71000, 95.0, 92.7, NOW(), '2024-02-28'),

('s016-0000-0000-0000-000000000016', '고해 (AI 리마스터)', '이소라', '발라드', '00s', 'ai_remix',
 'https://www.youtube.com/watch?v=dummy016', 'dummy016', 620000, 41000, 74.0, 73.8, NOW(), '2024-04-01'),

('s017-0000-0000-0000-000000000017', '쿵따리샤바라 (AI 리믹스)', '클론', '댄스', '90s', 'ai_remix',
 'https://www.youtube.com/watch?v=dummy017', 'dummy017', 830000, 36000, 85.0, 79.2, NOW(), '2024-03-20'),

('s018-0000-0000-0000-000000000018', 'Dreams Come True (AI 리마스터)', 'S.E.S', '댄스', '90s', 'ai_remix',
 'https://www.youtube.com/watch?v=dummy018', 'dummy018', 710000, 43000, 80.0, 77.5, NOW(), '2024-01-30'),

('s019-0000-0000-0000-000000000019', '인연 (AI 리마스터)', '이선희', '발라드', '00s', 'ai_remix',
 'https://www.youtube.com/watch?v=dummy019', 'dummy019', 940000, 56000, 87.0, 84.1, NOW(), '2024-04-10'),

('s020-0000-0000-0000-000000000020', '여수 밤바다 (AI 리마스터)', '버스커버스커', '발라드', '10s', 'ai_remix',
 'https://www.youtube.com/watch?v=dummy020', 'dummy020', 1350000, 68000, 93.0, 91.4, NOW(), '2024-03-15');


-- ③ USER_SONGS (저장·평가 데이터)
INSERT INTO user_songs (id, user_id, song_id, is_saved, rating, play_count, last_played_at) VALUES
  ('us01-0000-0000-0000-000000000001', 'u001-0000-0000-0000-000000000001', 's015-0000-0000-0000-000000000015', 1, 5, 12, NOW()),
  ('us02-0000-0000-0000-000000000002', 'u001-0000-0000-0000-000000000001', 's012-0000-0000-0000-000000000012', 1, 4, 8,  NOW()),
  ('us03-0000-0000-0000-000000000003', 'u001-0000-0000-0000-000000000001', 's005-0000-0000-0000-000000000005', 0, 3, 3,  NOW()),
  ('us04-0000-0000-0000-000000000004', 'u002-0000-0000-0000-000000000002', 's020-0000-0000-0000-000000000020', 1, 5, 20, NOW()),
  ('us05-0000-0000-0000-000000000005', 'u002-0000-0000-0000-000000000002', 's011-0000-0000-0000-000000000011', 1, 4, 15, NOW()),
  ('us06-0000-0000-0000-000000000006', 'u002-0000-0000-0000-000000000002', 's009-0000-0000-0000-000000000009', 1, 5, 9,  NOW()),
  ('us07-0000-0000-0000-000000000007', 'u003-0000-0000-0000-000000000003', 's001-0000-0000-0000-000000000001', 1, 5, 25, NOW()),
  ('us08-0000-0000-0000-000000000008', 'u003-0000-0000-0000-000000000003', 's007-0000-0000-0000-000000000007', 1, 4, 18, NOW()),
  ('us09-0000-0000-0000-000000000009', 'u003-0000-0000-0000-000000000003', 's017-0000-0000-0000-000000000017', 1, 4, 11, NOW()),
  ('us10-0000-0000-0000-000000000010', 'u004-0000-0000-0000-000000000004', 's019-0000-0000-0000-000000000019', 1, 5, 7,  NOW()),
  ('us11-0000-0000-0000-000000000011', 'u004-0000-0000-0000-000000000004', 's016-0000-0000-0000-000000000016', 1, 3, 4,  NOW()),
  ('us12-0000-0000-0000-000000000012', 'u005-0000-0000-0000-000000000005', 's004-0000-0000-0000-000000000004', 1, 4, 6,  NOW()),
  ('us13-0000-0000-0000-000000000013', 'u005-0000-0000-0000-000000000005', 's014-0000-0000-0000-000000000014', 1, 5, 10, NOW());


-- ④ RADIO_SESSIONS (라디오 세션 3개)
INSERT INTO radio_sessions (id, user_id, mood, story, dj_ment, comfort_text, novel_excerpt) VALUES
  ('rs01-0000-0000-0000-000000000001',
   'u001-0000-0000-0000-000000000001',
   '그리운',
   '오늘 오래된 사진을 보다가 학창시절이 너무 그리워졌어요.',
   '안녕하세요, DJ 리아예요. 감성덕후님의 사연을 들었어요. 그 시절의 기억이 얼마나 소중한지 느껴져요. 오늘은 그 추억을 음악으로 꺼내볼까요?',
   '그리움은 사랑했던 시간이 남긴 가장 따뜻한 흔적이에요. 오늘 밤, 그 시절의 음악이 당신 곁에 있을게요.',
   '"사람은 누구나 자기만의 시절을 가슴 속에 간직하며 산다." — 박완서, 그 많던 싱아는 누가 다 먹었을까'),

  ('rs02-0000-0000-0000-000000000002',
   'u002-0000-0000-0000-000000000002',
   '지친',
   '요즘 일이 너무 힘들어서 아무것도 하기 싫어요. 그냥 음악이나 듣고 싶어요.',
   '새벽세시님, 오늘 많이 지치셨군요. 아무것도 안 해도 괜찮아요. 지금 이 순간은 그냥 쉬어가도 충분해요.',
   '지쳐도 괜찮아요. 쉬어가는 것도 앞으로 나아가는 용기니까요. 오늘 밤은 음악에 모든 걸 맡겨보세요.',
   '"가끔은 아무것도 하지 않는 것이 가장 용감한 일이다." — 헤르만 헤세, 데미안'),

  ('rs03-0000-0000-0000-000000000003',
   'u004-0000-0000-0000-000000000004',
   '외로운',
   '혼자 있는 밤이 너무 길게 느껴져요. 누군가 옆에 있으면 좋겠어요.',
   '별빛수집가님, 혼자인 밤이 유독 길게 느껴질 때가 있죠. 오늘 밤은 DJ 리아가 함께할게요. 이 음악들이 조용히 옆에 앉아 있을 거예요.',
   '혼자라는 느낌은 때로 마음이 더 넓어지는 시간이기도 해요. 이 노래들이 그 공간을 채워줄 거예요.',
   '"외로움은 혼자 있는 것이 아니라, 이해받지 못한다는 느낌이다." — 프리드리히 니체');


-- ⑤ RADIO_RECOMMENDATIONS (세션별 추천 곡)
INSERT INTO radio_recommendations (id, session_id, song_id, order_num, reason) VALUES
  ('rr01-0000-0000-0000-000000000001', 'rs01-0000-0000-0000-000000000001', 's011-0000-0000-0000-000000000011', 1, '90년대 감성을 AI로 되살린 곡으로 그리움을 자극해요'),
  ('rr02-0000-0000-0000-000000000002', 'rs01-0000-0000-0000-000000000001', 's013-0000-0000-0000-000000000013', 2, '학창시절 누구나 알던 명곡의 AI 리마스터'),
  ('rr03-0000-0000-0000-000000000003', 'rs01-0000-0000-0000-000000000001', 's018-0000-0000-0000-000000000018', 3, '90년대 감성 집약체, 추억 소환에 최적'),
  ('rr04-0000-0000-0000-000000000004', 'rs02-0000-0000-0000-000000000002', 's012-0000-0000-0000-000000000012', 1, '지친 마음을 위로하는 발라드 1순위'),
  ('rr05-0000-0000-0000-000000000005', 'rs02-0000-0000-0000-000000000002', 's016-0000-0000-0000-000000000016', 2, '이소라 특유의 감성이 지친 마음을 어루만져줘요'),
  ('rr06-0000-0000-0000-000000000006', 'rs02-0000-0000-0000-000000000002', 's015-0000-0000-0000-000000000015', 3, '잔잔한 멜로디로 쉬어가기 좋은 곡'),
  ('rr07-0000-0000-0000-000000000007', 'rs03-0000-0000-0000-000000000003', 's020-0000-0000-0000-000000000020', 1, '밤바다 감성으로 외로운 밤을 채워줘요'),
  ('rr08-0000-0000-0000-000000000008', 'rs03-0000-0000-0000-000000000003', 's019-0000-0000-0000-000000000019', 2, '이선희의 따뜻한 목소리가 곁에 있는 느낌'),
  ('rr09-0000-0000-0000-000000000009', 'rs03-0000-0000-0000-000000000003', 's014-0000-0000-0000-000000000014', 3, '포근한 발라드로 외로움을 달래줘요');


-- ⑥ SCORE_LOGS (점수 갱신 이력 샘플)
INSERT INTO score_logs (id, song_id, score_before, score_after, view_count, like_count, trend_score, logged_at) VALUES
  ('sl01-0000-0000-0000-000000000001', 's015-0000-0000-0000-000000000015', 88.1, 92.7, 1500000, 71000, 95.0, NOW()),
  ('sl02-0000-0000-0000-000000000002', 's020-0000-0000-0000-000000000020', 89.2, 91.4, 1350000, 68000, 93.0, NOW()),
  ('sl03-0000-0000-0000-000000000003', 's012-0000-0000-0000-000000000012', 85.0, 87.2, 980000,  52000, 91.0, NOW()),
  ('sl04-0000-0000-0000-000000000004', 's011-0000-0000-0000-000000000011', 82.3, 85.4, 1200000, 48000, 88.0, NOW()),
  ('sl05-0000-0000-0000-000000000005', 's005-0000-0000-0000-000000000005', 87.1, 88.3, 8900000, 155000, 70.0, NOW());


-- ⑦ PLAYLISTS
INSERT INTO playlists (id, user_id, name, mood_tag, is_public) VALUES
  ('pl01-0000-0000-0000-000000000001', 'u001-0000-0000-0000-000000000001', '새벽 감성 모음', '그리운', 1),
  ('pl02-0000-0000-0000-000000000002', 'u002-0000-0000-0000-000000000002', '출퇴근길 위로 플리', '지친', 1),
  ('pl03-0000-0000-0000-000000000003', 'u003-0000-0000-0000-000000000003', '90s 레전드 모음', NULL, 0);


-- ⑧ PLAYLIST_SONGS
INSERT INTO playlist_songs (id, playlist_id, song_id, order_num) VALUES
  ('ps01-0000-0000-0000-000000000001', 'pl01-0000-0000-0000-000000000001', 's012-0000-0000-0000-000000000012', 1),
  ('ps02-0000-0000-0000-000000000002', 'pl01-0000-0000-0000-000000000001', 's016-0000-0000-0000-000000000016', 2),
  ('ps03-0000-0000-0000-000000000003', 'pl01-0000-0000-0000-000000000001', 's019-0000-0000-0000-000000000019', 3),
  ('ps04-0000-0000-0000-000000000004', 'pl01-0000-0000-0000-000000000001', 's020-0000-0000-0000-000000000020', 4),
  ('ps05-0000-0000-0000-000000000005', 'pl02-0000-0000-0000-000000000002', 's015-0000-0000-0000-000000000015', 1),
  ('ps06-0000-0000-0000-000000000006', 'pl02-0000-0000-0000-000000000002', 's014-0000-0000-0000-000000000014', 2),
  ('ps07-0000-0000-0000-000000000007', 'pl02-0000-0000-0000-000000000002', 's012-0000-0000-0000-000000000012', 3),
  ('ps08-0000-0000-0000-000000000008', 'pl03-0000-0000-0000-000000000003', 's001-0000-0000-0000-000000000001', 1),
  ('ps09-0000-0000-0000-000000000009', 'pl03-0000-0000-0000-000000000003', 's007-0000-0000-0000-000000000007', 2),
  ('ps10-0000-0000-0000-000000000010', 'pl03-0000-0000-0000-000000000003', 's008-0000-0000-0000-000000000008', 3),
  ('ps11-0000-0000-0000-000000000011', 'pl03-0000-0000-0000-000000000003', 's003-0000-0000-0000-000000000003', 4),
  ('ps12-0000-0000-0000-000000000012', 'pl03-0000-0000-0000-000000000003', 's011-0000-0000-0000-000000000011', 5);


-- ============================================
-- 확인용 조회 쿼리
-- ============================================

-- 점수 기반 TOP 10 추천 (AI 리믹스 우선)
SELECT title, artist, type, genre, era, score
FROM songs
ORDER BY score DESC
LIMIT 10;

-- 특정 유저의 저장 목록
SELECT u.nickname, s.title, s.artist, us.rating, us.play_count
FROM user_songs us
JOIN users u ON u.id = us.user_id
JOIN songs s ON s.id = us.song_id
WHERE us.user_id = 'u001-0000-0000-0000-000000000001'
  AND us.is_saved = 1
ORDER BY us.play_count DESC;

-- 기분별 추천 (mood 기반 라디오 세션 통계)
SELECT rs.mood, COUNT(*) AS session_count, AVG(s.score) AS avg_song_score
FROM radio_sessions rs
JOIN radio_recommendations rr ON rr.session_id = rs.id
JOIN songs s ON s.id = rr.song_id
GROUP BY rs.mood
ORDER BY session_count DESC;


CREATE USER IF NOT EXISTS 'SSAFY'@'localhost' IDENTIFIED BY 'SSAFY';
CREATE USER IF NOT EXISTS 'SSAFY'@'127.0.0.1' IDENTIFIED BY 'SSAFY';

GRANT ALL PRIVILEGES ON kpop_radio.* TO 'SSAFY'@'localhost';
GRANT ALL PRIVILEGES ON kpop_radio.* TO 'SSAFY'@'127.0.0.1';
FLUSH PRIVILEGES;

USE kpop_radio;
SHOW TABLES;