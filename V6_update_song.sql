USE kpop_radio;

-- FK 제약 임시 해제
SET FOREIGN_KEY_CHECKS = 0;

-- 기존 테이블 드랍 (songs 참조하는 테이블도 함께)
DROP TABLE IF EXISTS playlist_songs;
DROP TABLE IF EXISTS radio_recommendations;
DROP TABLE IF EXISTS score_logs;
DROP TABLE IF EXISTS user_songs;
DROP TABLE IF EXISTS songs;

-- FK 제약 복구
SET FOREIGN_KEY_CHECKS = 1;

-- songs 테이블 재생성
CREATE TABLE songs (
    id                CHAR(36)        NOT NULL DEFAULT (UUID()),
    title             VARCHAR(200)    NOT NULL,
    artist            VARCHAR(100)    NOT NULL,
    genre             VARCHAR(50)     NOT NULL     COMMENT '발라드 | 댄스 | 힙합 | R&B | 록',
    era               VARCHAR(20)     NOT NULL     COMMENT '90s | 00s | 10s | 20s',
    type              VARCHAR(20)     NOT NULL     COMMENT 'original | ai_remix',
    youtube_url       VARCHAR(300)    NOT NULL,
    youtube_id        VARCHAR(50)     NOT NULL     COMMENT 'YouTube 영상 ID',
    view_count        INT             NOT NULL     DEFAULT 0,
    like_count        INT             NOT NULL     DEFAULT 0,
    trend_score       FLOAT           NOT NULL     DEFAULT 0.0  COMMENT '최근 7일 증가율 기반',
    score             FLOAT           NOT NULL     DEFAULT 0.0  COMMENT '가중 합산 점수 (0~100)',
    score_updated_at  DATETIME        NULL,
    released_at       DATE            NULL,
    created_at        DATETIME        NOT NULL     DEFAULT CURRENT_TIMESTAMP,

    -- 분석 필드
    duration_seconds  INT             NULL         COMMENT '영상 길이(초)',
    bpm               FLOAT           NULL         COMMENT '템포',
    energy            FLOAT           NULL         COMMENT '에너지 (0~1)',
    danceability      FLOAT           NULL         COMMENT '댄서빌리티 (0~1)',
    loudness          FLOAT           NULL         COMMENT '음량 (dB)',
    musical_key       VARCHAR(10)     NULL         COMMENT '음악 키 (C, D, E ...)',
    musical_scale     VARCHAR(10)     NULL         COMMENT '장조/단조 (major | minor)',

    PRIMARY KEY (id),
    INDEX idx_type    (type),
    INDEX idx_genre   (genre),
    INDEX idx_era     (era),
    INDEX idx_score   (score DESC),
    INDEX idx_youtube_id (youtube_id)
) ENGINE=InnoDB COMMENT='원곡 및 AI 리믹스 노래 정보';

-- user_songs 재생성
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
    INDEX idx_user_saved   (user_id, is_saved),
    INDEX idx_user_rating  (user_id, rating)
) ENGINE=InnoDB COMMENT='유저별 노래 저장/평가/재생 이력';

-- score_logs 재생성
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

-- radio_recommendations 재생성
CREATE TABLE radio_recommendations (
    id          CHAR(36)     NOT NULL DEFAULT (UUID()),
    session_id  CHAR(36)     NOT NULL,
    song_id     CHAR(36)     NOT NULL,
    order_num   TINYINT      NOT NULL DEFAULT 1 COMMENT '추천 순서',
    reason      VARCHAR(200) NULL     COMMENT '추천 이유',
    PRIMARY KEY (id),
    FOREIGN KEY (session_id) REFERENCES radio_sessions(id) ON DELETE CASCADE,
    FOREIGN KEY (song_id)    REFERENCES songs(id)          ON DELETE CASCADE,
    INDEX idx_session (session_id, order_num)
) ENGINE=InnoDB COMMENT='라디오 세션 추천 곡 목록';

-- playlist_songs 재생성
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