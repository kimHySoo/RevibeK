ALTER TABLE youtube_videos_raw
ADD COLUMN is_analyzed TINYINT(1) NOT NULL DEFAULT 0 COMMENT '분석 완료 여부';

