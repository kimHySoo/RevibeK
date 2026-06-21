-- =====================================================================
-- 감정(mood)/세대(generation)/장르(genre) 표준 코드 정규화 마이그레이션
-- 작성 배경: songs.mood / songs.genre / songs.generation 은 자유 문자열로
-- 저장되어 있어 추천 쿼리가 LIKE/정확매치 문자열 비교에 의존하고, 어휘가
-- 통일되어 있지 않다. 이 파일은 그 위에 표준 코드 마스터 테이블과
-- song_moods 매핑 테이블을 추가하되, 기존 songs 컬럼(mood/genre/era/
-- generation)은 절대 변경/삭제하지 않는 과도기(append-only) 마이그레이션이다.
--
-- 적용 방법(Flyway/Liquibase 미사용 프로젝트 컨벤션):
--   mysql -h <host> -P <port> -u <user> -p kpop_radio < migration_add_mood_generation_genre_normalization.sql
-- 모든 구문이 IF NOT EXISTS / INSERT IGNORE 라 재실행해도 안전하다.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. moods: 감정 마스터 (정확히 7개만 유지)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS moods (
    code        VARCHAR(30)  NOT NULL,
    label       VARCHAR(30)  NOT NULL,
    description VARCHAR(255) NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    PRIMARY KEY (code),
    UNIQUE KEY uk_moods_label (label)
) ENGINE=InnoDB;

INSERT IGNORE INTO moods (code, label) VALUES
('TIRED',      '지침'),
('EXCITED',    '설렘'),
('NOSTALGIC',  '회상'),
('CONFIDENT',  '자신감'),
('COMFORT',    '위로'),
('LONELY',     '외로움'),
('ENERGETIC',  '신남');

-- ---------------------------------------------------------------------
-- 2. mood_aliases: 감정 동의어 → 표준 코드 (Java MoodNormalizer와 동일 매핑)
--    SQL 기반 일괄 이관 쿼리에서 재사용하기 위한 보조 테이블.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS mood_aliases (
    alias     VARCHAR(30) NOT NULL,
    mood_code VARCHAR(30) NOT NULL,
    PRIMARY KEY (alias),
    CONSTRAINT fk_mood_aliases_mood
        FOREIGN KEY (mood_code) REFERENCES moods(code)
) ENGINE=InnoDB;

INSERT IGNORE INTO mood_aliases (alias, mood_code) VALUES
('지침', 'TIRED'), ('지친', 'TIRED'), ('피곤함', 'TIRED'), ('피곤한', 'TIRED'), ('무기력', 'TIRED'), ('힘듦', 'TIRED'),
('설렘', 'EXCITED'), ('설레는', 'EXCITED'), ('설레임', 'EXCITED'),
('회상', 'NOSTALGIC'), ('그리움', 'NOSTALGIC'), ('그리운', 'NOSTALGIC'), ('추억', 'NOSTALGIC'), ('추억회상', 'NOSTALGIC'), ('과거회상', 'NOSTALGIC'), ('향수', 'NOSTALGIC'),
('자신감', 'CONFIDENT'), ('자신있는', 'CONFIDENT'), ('당당함', 'CONFIDENT'), ('당당한', 'CONFIDENT'), ('파이팅', 'CONFIDENT'),
('위로', 'COMFORT'), ('힐링', 'COMFORT'), ('편안함', 'COMFORT'), ('안정', 'COMFORT'), ('따뜻함', 'COMFORT'),
('외로움', 'LONELY'), ('외로운', 'LONELY'), ('쓸쓸함', 'LONELY'), ('쓸쓸한', 'LONELY'), ('고독', 'LONELY'),
('신남', 'ENERGETIC'), ('신나는', 'ENERGETIC'), ('흥겨움', 'ENERGETIC'), ('즐거움', 'ENERGETIC'), ('행복한', 'ENERGETIC'), ('활기', 'ENERGETIC');

-- 의도적으로 alias 테이블에 넣지 않는 검토 대상(최종 7개와 의미가 겹치지 않거나
-- 모호해서 자동 확정하지 않는 값): 환상, 몽환, 감성, 청량, 강렬함, 슬픔, 슬픈, 행복(단독)

-- ---------------------------------------------------------------------
-- 3. song_moods: 곡 ↔ 감정 N:M 매핑
--    songs.id 타입(CHAR(36))과 정확히 일치시킨다.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS song_moods (
    song_id   CHAR(36)    NOT NULL,
    mood_code VARCHAR(30) NOT NULL,
    PRIMARY KEY (song_id, mood_code),
    CONSTRAINT fk_song_moods_song
        FOREIGN KEY (song_id) REFERENCES songs(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_song_moods_mood
        FOREIGN KEY (mood_code) REFERENCES moods(code)
) ENGINE=InnoDB;

CREATE INDEX idx_song_moods_mood_song
ON song_moods (mood_code, song_id);

-- ---------------------------------------------------------------------
-- 4. generations: 세대 마스터 (정확히 2개. ALL은 검색 필터 전용 상수이므로
--    마스터 테이블에 곡의 실제 세대값으로 넣지 않는다)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS generations (
    code   VARCHAR(20) NOT NULL,
    label  VARCHAR(20) NOT NULL,
    active BOOLEAN     NOT NULL DEFAULT TRUE,
    PRIMARY KEY (code),
    UNIQUE KEY uk_generations_label (label)
) ENGINE=InnoDB;

INSERT IGNORE INTO generations (code, label) VALUES
('SECOND', '2세대'),
('THIRD',  '3세대');

-- ---------------------------------------------------------------------
-- 5. genres: 장르 마스터 (정확히 6개)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS genres (
    code   VARCHAR(20) NOT NULL,
    label  VARCHAR(20) NOT NULL,
    active BOOLEAN     NOT NULL DEFAULT TRUE,
    PRIMARY KEY (code),
    UNIQUE KEY uk_genres_label (label)
) ENGINE=InnoDB;

INSERT IGNORE INTO genres (code, label) VALUES
('DANCE',  '댄스'),
('BALLAD', '발라드'),
('RNB',    'R&B'),
('HIPHOP', '힙합'),
('IDOL',   '아이돌'),
('OST',    'OST');

-- ---------------------------------------------------------------------
-- 6. 기존 songs.mood (쉼표 구분 다중값 문자열) → song_moods 이관
--    주의: 마이그레이션 시점(2026-06-21) 기준 songs.mood 는 전 행 NULL이라
--    아래 쿼리는 실제로 0건을 삽입한다. 향후 songs.mood 가 채워지면
--    재실행 시 그 시점 데이터를 기준으로 정상 이관된다(재실행 안전).
-- ---------------------------------------------------------------------
INSERT IGNORE INTO song_moods (song_id, mood_code)
WITH RECURSIVE split_mood AS (
    SELECT
        id AS song_id,
        TRIM(SUBSTRING_INDEX(mood, ',', 1)) AS token,
        IF(LOCATE(',', mood) > 0, SUBSTRING(mood, LOCATE(',', mood) + 1), NULL) AS rest
    FROM songs
    WHERE mood IS NOT NULL AND TRIM(mood) <> ''
    UNION ALL
    SELECT
        song_id,
        TRIM(SUBSTRING_INDEX(rest, ',', 1)) AS token,
        IF(LOCATE(',', rest) > 0, SUBSTRING(rest, LOCATE(',', rest) + 1), NULL) AS rest
    FROM split_mood
    WHERE rest IS NOT NULL AND TRIM(rest) <> ''
)
SELECT sm.song_id, COALESCE(m.code, ma.mood_code) AS mood_code
FROM split_mood sm
LEFT JOIN moods m ON m.label = sm.token OR m.code = UPPER(sm.token)
LEFT JOIN mood_aliases ma ON ma.alias = sm.token
WHERE sm.token <> '' AND COALESCE(m.code, ma.mood_code) IS NOT NULL;

-- ---------------------------------------------------------------------
-- 7. 검증/검토 대상 조회 (실행 결과는 결과 보고서에 기록)
-- ---------------------------------------------------------------------
-- 7-1. moods / song_moods 적재 현황
-- SELECT * FROM moods ORDER BY code;
-- SELECT mood_code, COUNT(*) AS song_count FROM song_moods GROUP BY mood_code ORDER BY mood_code;

-- 7-2. songs.mood 중 표준 코드/별칭으로 매핑되지 않은 토큰(검토 대상)
-- WITH RECURSIVE split_mood AS (
--     SELECT id AS song_id, TRIM(SUBSTRING_INDEX(mood, ',', 1)) AS token,
--            IF(LOCATE(',', mood) > 0, SUBSTRING(mood, LOCATE(',', mood) + 1), NULL) AS rest
--     FROM songs WHERE mood IS NOT NULL AND TRIM(mood) <> ''
--     UNION ALL
--     SELECT song_id, TRIM(SUBSTRING_INDEX(rest, ',', 1)) AS token,
--            IF(LOCATE(',', rest) > 0, SUBSTRING(rest, LOCATE(',', rest) + 1), NULL) AS rest
--     FROM split_mood WHERE rest IS NOT NULL AND TRIM(rest) <> ''
-- )
-- SELECT DISTINCT sm.token
-- FROM split_mood sm
-- LEFT JOIN moods m ON m.label = sm.token OR m.code = UPPER(sm.token)
-- LEFT JOIN mood_aliases ma ON ma.alias = sm.token
-- WHERE sm.token <> '' AND m.code IS NULL AND ma.mood_code IS NULL;

-- 7-3. 2세대/3세대 외 generation 값 (서비스 대상 외, 임의 변환하지 않음)
-- SELECT generation, COUNT(*) FROM songs WHERE generation IS NOT NULL AND generation NOT IN ('2세대','3세대') GROUP BY generation;

-- 7-4. 6개 장르 라벨과 일치하지 않는 genre 값 (분류 미정, 임의 확정하지 않음)
-- SELECT genre, COUNT(*) FROM songs WHERE genre NOT IN ('댄스','발라드','R&B','힙합','아이돌','OST') GROUP BY genre;
