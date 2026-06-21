-- 기존 운영/개발 DB에 라디오 TTS 컬럼을 추가하는 마이그레이션.
-- kpop_radio_schema.sql로 새로 초기화하는 환경에서는 이미 컬럼이 포함되어 있으므로 실행할 필요 없다.
-- 모두 NULL 허용이라 기존 radio_sessions 행이 있어도 오류 없이 적용된다.
--
-- MySQL은 `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`를 지원하지 않는다(8.0.45에서도 문법 오류).
-- 그래서 INFORMATION_SCHEMA.COLUMNS로 컬럼 존재 여부를 먼저 확인한 뒤 동적 SQL로 ALTER를 실행해,
-- 이미 컬럼이 있는 DB에 다시 실행해도 "Duplicate column name" 오류 없이 안전하게 재실행할 수 있다.

SET @col_exists = (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'radio_sessions' AND COLUMN_NAME = 'tts_mode'
);
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE radio_sessions ADD COLUMN tts_mode VARCHAR(30) NULL COMMENT ''GOOGLE_TTS | BROWSER_TTS, 생성 당시 합성 결과''',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'radio_sessions' AND COLUMN_NAME = 'tts_audio_url'
);
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE radio_sessions ADD COLUMN tts_audio_url MEDIUMTEXT NULL COMMENT ''Google TTS data URI(base64) 또는 정적 파일 URL. browser/미생성이면 NULL''',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'radio_sessions' AND COLUMN_NAME = 'tts_voice'
);
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE radio_sessions ADD COLUMN tts_voice VARCHAR(100) NULL COMMENT ''합성에 사용된 voice 이름 (예: ko-KR-Chirp3-HD-Vindemiatrix)''',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'radio_sessions' AND COLUMN_NAME = 'tts_audio_encoding'
);
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE radio_sessions ADD COLUMN tts_audio_encoding VARCHAR(30) NULL COMMENT ''합성 오디오 인코딩 (예: MP3, LINEAR16)''',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
