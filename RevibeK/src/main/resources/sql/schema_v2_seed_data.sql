-- ============================================================
-- RevibeK schema v2 -- 마스터/참조 데이터 + 기존 demo 데이터 시드
-- 실행 순서: schema_v2_ddl.sql 다음, schema_v2_songs_load.sql 이전
--
-- songs / user_songs 는 이 파일에 포함하지 않는다 (요구사항).
-- users는 원칙적으로 제외 대상이지만, 아래 playlists/radio_sessions 데모 데이터가
-- last/dump/kpop_radio_dump.sql의 demo 사용자(u001~u005)를 FK로 참조하므로
-- (없으면 Error 1452 FK 제약 위반), 3-1번에서 그 5명만 예외적으로 INSERT한다.
-- 운영 DB에 동일 id의 실제 사용자가 이미 있다면 3-1번 INSERT는 건너뛸 것.
-- ============================================================

USE kpop_radio_2;

-- ------------------------------------------------------------
-- 1. 감정/세대/장르 마스터 (정확한 개수 고정: 감정 7 / 세대 2 / 장르 6)
-- ------------------------------------------------------------
INSERT INTO moods (code, label) VALUES
('TIRED',      '지침'),
('EXCITED',    '설렘'),
('NOSTALGIC',  '회상'),
('CONFIDENT',  '자신감'),
('COMFORT',    '위로'),
('LONELY',     '외로움'),
('ENERGETIC',  '신남');

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

-- ------------------------------------------------------------
-- 2. youtube_channels -- 기존 수집 채널 12건
--    원본: last/dump/kpop_radio_dump.sql (구 스키마: id BIGINT, channel_url, is_active)
--    신규 스키마(CHAR(36) id, url, uploads_playlist, subscriber_count)에 맞게 재매핑.
--    uploads_playlist/subscriber_count/last_checked_at은 구 덤프에 값이 없어 NULL 처리.
-- ------------------------------------------------------------
INSERT INTO youtube_channels (url, channel_id, channel_name, uploads_playlist, subscriber_count, last_checked_at, created_at, updated_at) VALUES
('https://www.youtube.com/channel/UCh8AnUKJ2E4JDh4kI61UCHQ', 'UCh8AnUKJ2E4JDh4kI61UCHQ', 'HarmoVerse', NULL, NULL, NULL, '2026-06-02 15:57:55', '2026-06-02 15:57:55'),
('https://www.youtube.com/@chenzie1004', 'UCl-mHvTirLyYXEBoHzyVgLA', 'chenzie', NULL, NULL, NULL, '2026-06-02 15:57:57', '2026-06-02 15:57:57'),
('https://www.youtube.com/@THE_aIDOL', 'UCQCTSiEbB8c0MAXlr5NhtQA', 'aIDOL', NULL, NULL, NULL, '2026-06-02 15:57:59', '2026-06-02 15:57:59'),
('https://www.youtube.com/@EQUINOX.entertainment', 'UCyJGK3bJjqsILlBDX5spswQ', 'EQUINOX Entertainment', NULL, NULL, NULL, '2026-06-02 15:58:02', '2026-06-02 15:58:02'),
('https://www.youtube.com/@Dir.Fevernova', 'UCGP6GoJgmluyOITsI1HsrVQ', 'Dir. Fevernova', NULL, NULL, NULL, '2026-06-02 15:58:04', '2026-06-02 15:58:04'),
('https://www.youtube.com/channel/UCkVBepcU7jd1LjgKKfjz83Q', 'UCkVBepcU7jd1LjgKKfjz83Q', 'ERRDAY GROOVE (얼데이그루브)', NULL, NULL, NULL, '2026-06-02 15:58:06', '2026-06-02 15:58:06'),
('https://www.youtube.com/@A_I_Go', 'UCLg0aLWcVKG9vg1hLOd_BDg', '에이아이고 - AIGO Music', NULL, NULL, NULL, '2026-06-02 15:58:12', '2026-06-02 15:58:12'),
('https://www.youtube.com/@DALLASTUDIOS', 'UCoc_xlqn3sddicPBNMKj9Tw', 'DALLA STUDIOS', NULL, NULL, NULL, '2026-06-02 15:58:16', '2026-06-02 15:58:16'),
('https://www.youtube.com/channel/UC4woL-HW4aiHLp4AjyxS7Og', 'UC4woL-HW4aiHLp4AjyxS7Og', 'CareBoys', NULL, NULL, NULL, '2026-06-02 15:58:16', '2026-06-02 15:58:16'),
('https://www.youtube.com/@TREE-WAVE', 'UCNcWt_LxVO_Hqo-0HcMGb_Q', 'TREE WAVE', NULL, NULL, NULL, '2026-06-02 15:58:18', '2026-06-02 15:58:18'),
('https://www.youtube.com/@i-playlist-hr', 'UCVS5gHrD0tZV8W4YvvGMuSA', 'i플리', NULL, NULL, NULL, '2026-06-02 15:58:23', '2026-06-02 15:58:23'),
('https://www.youtube.com/@뮤잇-Music_it', 'UCdA3zZbviZv96ArWtR12cLw', '뮤잇', NULL, NULL, NULL, '2026-06-02 15:58:23', '2026-06-02 15:58:23');

-- ------------------------------------------------------------
-- 3. youtube_videos_raw -- 기존 1,472건은 이 파일에 포함하지 않음 (TODO 참고)
--    구 스키마가 youtube_channel_id(BIGINT FK) / video_title 컬럼을 사용해
--    신규 channel_id(VARCHAR FK) / title 컬럼과 1:1 자동 변환이 불가능하다.
--    별도 마이그레이션 스크립트(채널 BIGINT id -> channel_id 문자열 매핑)가 필요하다.
--    자세한 내용은 docs/answer/erd2.md "11. 남은 TODO" 참고.
-- ------------------------------------------------------------

-- ------------------------------------------------------------
-- 3-1. users -- demo 사용자 5건 (last/dump/kpop_radio_dump.sql)
--    원래는 users를 이 파일에 포함하지 않는 것이 원칙이었으나, 아래 playlists/
--    radio_sessions 데모 데이터가 이 5명을 FK로 참조하므로(Error 1452 방지),
--    빈 DB(kpop_radio_2)에 데모 데이터를 그대로 살리기 위해 예외적으로 포함한다.
--    운영 DB에 이미 동일 id의 실제 사용자가 있다면 이 INSERT는 건너뛸 것.
-- ------------------------------------------------------------
INSERT INTO users (id, nickname, email, provider, provider_id, password_hash, created_at, updated_at) VALUES
('u001-0000-0000-0000-000000000001','감성덕후','user1@example.com','google','g_001',NULL,'2026-05-22 15:32:41','2026-05-22 15:32:41'),
('u002-0000-0000-0000-000000000002','새벽세시','user2@example.com','kakao','k_002',NULL,'2026-05-22 15:32:41','2026-05-22 15:32:41'),
('u003-0000-0000-0000-000000000003','레트로킹','user3@example.com','local',NULL,'$2a$10$mockHashValue1','2026-05-22 15:32:41','2026-05-22 15:32:41'),
('u004-0000-0000-0000-000000000004','별빛수집가','user4@example.com','google','g_004',NULL,'2026-05-22 15:32:41','2026-05-22 15:32:41'),
('u005-0000-0000-0000-000000000005','추억여행자','user5@example.com','local',NULL,'$2a$10$mockHashValue2','2026-05-22 15:32:41','2026-05-22 15:32:41');

-- ------------------------------------------------------------
-- 4. playlists -- 기존 demo 3건 (last/dump/kpop_radio_dump.sql)
--    주의: user_id가 위 demo 사용자(u001~u003)를 참조함.
-- ------------------------------------------------------------
INSERT INTO playlists (id, user_id, name, mood_tag, is_public, created_at) VALUES
('pl01-0000-0000-0000-000000000001','u001-0000-0000-0000-000000000001','새벽 감성 모음','그리운',1,'2026-05-22 15:32:41'),
('pl02-0000-0000-0000-000000000002','u002-0000-0000-0000-000000000002','출퇴근길 위로 플리','지친',1,'2026-05-22 15:32:41'),
('pl03-0000-0000-0000-000000000003','u003-0000-0000-0000-000000000003','90s 레전드 모음',NULL,0,'2026-05-22 15:32:41');

-- ------------------------------------------------------------
-- 5. radio_sessions -- 기존 demo 3건 (last/dump/kpop_radio_dump.sql)
--    구 덤프는 era/genre/situation/desired_mood/video_type/preferred_artist/
--    excluded_keywords/recommendation_source/tts_*/playlist_id/is_public/published_at
--    컬럼이 없던 구버전 스키마라서, 신규 컬럼은 모두 NULL/기본값으로 채운다.
--    주의: user_id가 demo 사용자(u001/u002/u004)를 참조함.
-- ------------------------------------------------------------
INSERT INTO radio_sessions (id, user_id, mood, story, dj_ment, comfort_text, novel_excerpt, is_public, created_at) VALUES
('rs01-0000-0000-0000-000000000001','u001-0000-0000-0000-000000000001','그리운','오늘 오래된 사진을 보다가 학창시절이 너무 그리워졌어요.','안녕하세요, DJ 리아예요. 감성덕후님의 사연을 들었어요. 그 시절의 기억이 얼마나 소중한지 느껴져요. 오늘은 그 추억을 음악으로 꺼내볼까요?','그리움은 사랑했던 시간이 남긴 가장 따뜻한 흔적이에요. 오늘 밤, 그 시절의 음악이 당신 곁에 있을게요.','\"사람은 누구나 자기만의 시절을 가슴 속에 간직하며 산다.\" — 박완서, 그 많던 싱아는 누가 다 먹었을까',0,'2026-05-22 15:32:41'),
('rs02-0000-0000-0000-000000000002','u002-0000-0000-0000-000000000002','지친','요즘 일이 너무 힘들어서 아무것도 하기 싫어요. 그냥 음악이나 듣고 싶어요.','새벽세시님, 오늘 많이 지치셨군요. 아무것도 안 해도 괜찮아요. 지금 이 순간은 그냥 쉬어가도 충분해요.','지쳐도 괜찮아요. 쉬어가는 것도 앞으로 나아가는 용기니까요. 오늘 밤은 음악에 모든 걸 맡겨보세요.','\"가끔은 아무것도 하지 않는 것이 가장 용감한 일이다.\" — 헤르만 헤세, 데미안',0,'2026-05-22 15:32:41'),
('rs03-0000-0000-0000-000000000003','u004-0000-0000-0000-000000000004','외로운','혼자 있는 밤이 너무 길게 느껴져요. 누군가 옆에 있으면 좋겠어요.','별빛수집가님, 혼자인 밤이 유독 길게 느껴질 때가 있죠. 오늘 밤은 DJ 리아가 함께할게요. 이 음악들이 조용히 옆에 앉아 있을 거예요.','혼자라는 느낌은 때로 마음이 더 넓어지는 시간이기도 해요. 이 노래들이 그 공간을 채워줄 거예요.','\"외로움은 혼자 있는 것이 아니라, 이해받지 못한다는 느낌이다.\" — 프리드리히 니체',0,'2026-05-22 15:32:41');

-- ------------------------------------------------------------
-- 6. 그 외 테이블 (playlist_songs, radio_recommendations, radio_likes, song_likes,
--    reviews, follows, plans, challenges, challenge_participants, user_preferences):
--    last/dump/kpop_radio_dump.sql 기준 기존 행 0건 (실제 운영 데이터 없음).
--    따라서 시드 INSERT 없이 빈 테이블로 생성한다.
-- ------------------------------------------------------------

-- ------------------------------------------------------------
-- 7. analyzed_songs / embedding_songs: 신규 테이블이며, RevibeK_AI/backup
--    (약 1,116개)과 RevibeK_AI/song_embeddings(약 249개) 파일을 그대로
--    INSERT문으로 옮기는 것은 이 작업 범위에 포함하지 않는다.
--    별도 동기화 배치/스크립트로 적재할 것을 권장한다 (erd2.md "11. 남은 TODO" 참고).
-- ------------------------------------------------------------
