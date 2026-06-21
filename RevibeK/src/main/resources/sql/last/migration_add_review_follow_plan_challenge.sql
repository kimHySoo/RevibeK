-- ============================================================
-- RevibeK Migration: Review / Follow / Plan / Challenge 테이블
-- 작성일: 2026-06-16
-- MySQL 8 기준, CREATE TABLE IF NOT EXISTS 방식 (멱등성 보장)
-- 실행 순서: kpop_radio_schema.sql 실행 이후에 실행
-- ============================================================

USE kpop_radio;

-- ============================================================
-- 1. reviews  (F06~F09 리뷰)
-- ============================================================
CREATE TABLE IF NOT EXISTS reviews (
  id          CHAR(36)    NOT NULL DEFAULT (UUID()),
  user_id     CHAR(36)    NOT NULL,
  song_id     CHAR(36)    NOT NULL,
  content     TEXT        NOT NULL,
  rating      TINYINT     NULL     COMMENT '1~5점, NULL 허용 (별점 없는 감상평 가능)',
  created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  FOREIGN KEY (user_id) REFERENCES users(id)  ON DELETE CASCADE,
  FOREIGN KEY (song_id) REFERENCES songs(id)  ON DELETE CASCADE,
  INDEX idx_review_song (song_id),
  INDEX idx_review_user (user_id)
) ENGINE=InnoDB COMMENT='곡 리뷰 / 감상평';


-- ============================================================
-- 2. follows  (F16 팔로우/팔로잉)
-- ============================================================
CREATE TABLE IF NOT EXISTS follows (
  follower_id  CHAR(36)  NOT NULL COMMENT '팔로우 하는 사람',
  followee_id  CHAR(36)  NOT NULL COMMENT '팔로우 받는 사람',
  created_at   DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (follower_id, followee_id),
  FOREIGN KEY (follower_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (followee_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_followee (followee_id)
) ENGINE=InnoDB COMMENT='팔로우 관계';


-- ============================================================
-- 3. plans  (F17 계획/일정 관리)
-- ============================================================
CREATE TABLE IF NOT EXISTS plans (
  id           CHAR(36)     NOT NULL DEFAULT (UUID()),
  user_id      CHAR(36)     NOT NULL,
  title        VARCHAR(200) NOT NULL,
  description  TEXT         NULL,
  plan_date    DATE         NULL     COMMENT '예정 감상 날짜',
  plan_type    VARCHAR(50)  NULL     COMMENT '라디오청취 | 플레이리스트 | 학습 | 운동 | 휴식 | 새벽감성',
  created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_plan_user (user_id),
  INDEX idx_plan_date (plan_date)
) ENGINE=InnoDB COMMENT='사용자 음악 청취 계획';


-- ============================================================
-- 4. challenges  (F18 챌린지)
-- ============================================================
CREATE TABLE IF NOT EXISTS challenges (
  id           CHAR(36)     NOT NULL DEFAULT (UUID()),
  creator_id   CHAR(36)     NOT NULL COMMENT '챌린지 생성자',
  title        VARCHAR(200) NOT NULL,
  description  TEXT         NULL,
  start_date   DATE         NULL,
  end_date     DATE         NULL,
  target_count INT          NOT NULL DEFAULT 1 COMMENT '달성 목표 횟수',
  created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  FOREIGN KEY (creator_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_challenge_creator (creator_id)
) ENGINE=InnoDB COMMENT='음악 감상 챌린지 (예: 7일 동안 2세대 K-POP 듣기)';


-- ============================================================
-- 5. challenge_participants  (챌린지 참여자 / 진행도)
-- ============================================================
CREATE TABLE IF NOT EXISTS challenge_participants (
  id             CHAR(36)    NOT NULL DEFAULT (UUID()),
  challenge_id   CHAR(36)    NOT NULL,
  user_id        CHAR(36)    NOT NULL,
  current_count  INT         NOT NULL DEFAULT 0  COMMENT '현재 달성 횟수',
  target_count   INT         NOT NULL DEFAULT 1  COMMENT '목표 횟수 (참여 시 복사)',
  status         VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS' COMMENT 'IN_PROGRESS | COMPLETED',
  joined_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_challenge_user (challenge_id, user_id),
  FOREIGN KEY (challenge_id) REFERENCES challenges(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id)      REFERENCES users(id)      ON DELETE CASCADE,
  INDEX idx_cp_user (user_id)
) ENGINE=InnoDB COMMENT='챌린지 참여자 및 진행도';
