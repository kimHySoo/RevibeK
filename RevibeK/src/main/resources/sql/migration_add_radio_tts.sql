-- 기존 운영/개발 DB에 라디오 TTS 컬럼을 추가하는 마이그레이션.
-- kpop_radio_schema.sql로 새로 초기화하는 환경에서는 이미 컬럼이 포함되어 있으므로 실행할 필요 없다.
-- 모두 NULL 허용이라 기존 radio_sessions 행이 있어도 오류 없이 적용된다.
ALTER TABLE radio_sessions
  ADD COLUMN tts_mode           VARCHAR(30)  NULL COMMENT 'GOOGLE_TTS | BROWSER_TTS, 생성 당시 합성 결과',
  ADD COLUMN tts_audio_url      MEDIUMTEXT   NULL COMMENT 'Google TTS data URI(base64) 또는 정적 파일 URL. browser/미생성이면 NULL',
  ADD COLUMN tts_voice          VARCHAR(100) NULL COMMENT '합성에 사용된 voice 이름 (예: ko-KR-Chirp3-HD-Vindemiatrix)',
  ADD COLUMN tts_audio_encoding VARCHAR(30)  NULL COMMENT '합성 오디오 인코딩 (예: MP3, LINEAR16)';
