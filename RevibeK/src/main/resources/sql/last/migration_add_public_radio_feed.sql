-- Migration: 공개 라디오 피드 / 라디오 좋아요 / 챌린지 유형 추가
-- 대상 DB: kpop_radio (MySQL 8)
-- 작성일: 2026-06-16
--
-- 주의사항:
--   MySQL 8은 ALTER TABLE ADD COLUMN IF NOT EXISTS 를 지원하지 않는다.
--   이미 적용된 DB에서 실행하면 "Duplicate column name" 오류가 발생한다.
--   한 번만 실행하거나, 각 ALTER 앞에 컬럼 존재 여부를 확인하고 실행할 것.

USE kpop_radio;

-- 1. radio_sessions: is_public 컬럼 추가
ALTER TABLE radio_sessions
    ADD COLUMN is_public BOOLEAN NOT NULL DEFAULT FALSE;

-- 2. radio_sessions: published_at 컬럼 추가
ALTER TABLE radio_sessions
    ADD COLUMN published_at DATETIME NULL;

-- 3. radio_sessions: is_public 인덱스 추가
ALTER TABLE radio_sessions
    ADD INDEX idx_radio_sessions_public (is_public, published_at);

-- 4. radio_likes 테이블 생성
CREATE TABLE IF NOT EXISTS radio_likes (
    radio_session_id CHAR(36) NOT NULL,
    user_id          CHAR(36) NOT NULL,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (radio_session_id, user_id),
    FOREIGN KEY (radio_session_id) REFERENCES radio_sessions(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id)          REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_radio_likes_user    (user_id),
    INDEX idx_radio_likes_session (radio_session_id)
) ENGINE=InnoDB COMMENT='라디오 사연 좋아요';

-- 5. challenges: challenge_type 컬럼 추가
ALTER TABLE challenges
    ADD COLUMN challenge_type VARCHAR(50) NULL;
