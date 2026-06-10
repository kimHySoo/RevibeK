-- songs 테이블에 Essentia 분석 필드 추가
ALTER TABLE songs
    ADD COLUMN beats_count         INT        NULL    COMMENT '비트 수'            AFTER musical_scale,
    ADD COLUMN beats_confidence    FLOAT      NULL    COMMENT '비트 신뢰도 (0~1)'  AFTER beats_count,
    ADD COLUMN key_strength        FLOAT      NULL    COMMENT '조성 강도 (0~1)'    AFTER beats_confidence,
    ADD COLUMN spectral_centroid   FLOAT      NULL    COMMENT '스펙트럼 중심 (Hz)' AFTER key_strength,
    ADD COLUMN zero_crossing_rate  FLOAT      NULL    COMMENT '영점 교차율'        AFTER spectral_centroid,
    ADD COLUMN is_analyzed         TINYINT(1) NOT NULL DEFAULT 0 COMMENT '분석 완료 여부' AFTER zero_crossing_rate;

-- 분석 여부 조회용 인덱스
CREATE INDEX idx_is_analyzed ON songs (is_analyzed);
