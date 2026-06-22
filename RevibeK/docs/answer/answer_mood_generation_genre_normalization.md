# RevibeK 감정·세대·장르 표준화 마이그레이션 결과 보고서

작업일: 2026-06-21

## 1. 기존 문제 요약

- `songs.mood`(VARCHAR(50))는 쉼표 구분 다중값 문자열 구조로 설계되어 있었고, 추천 쿼리는 `mood = #{mood}` 정확 일치 비교에 의존했다.
- `songs.genre`, `songs.generation`, `radio_sessions.mood/era/genre`, `user_preferences.preferred_*` 등에서 같은 의미의 감정/세대/장르가 서로 다른 한글 어휘로 흩어져 있을 위험이 있었다.
- `songs.id`(CHAR(36))는 `radio_recommendations`, `user_songs`, `song_likes`, `playlist_songs`, `score_logs`, Qdrant point ID에서 광범위하게 참조되어, 임의 재생성이 금지된다.

**실제 조사 결과(가정과 다른 부분 — 정직하게 기록):**
- 로컬 MySQL(`kpop_radio`, localhost:3306)에 직접 접속해 확인한 결과, `songs` 1,114행 중 **`mood`는 전 행 NULL**, **`genre`는 전 행 `'미분류'`** 였다. 즉 "환상, 외로움" 같은 다중값 문자열은 현재 데이터에 존재하지 않았다.
- `songs.generation`은 991행 NULL, 나머지는 1~5세대가 각각 13/23/30/28/29건 존재했다(2·3세대 데이터는 실재함).
- `songs.id`, `songs.youtube_id` 모두 **중복 0건** — 사용자가 우려한 "중복 PK" 문제는 현재 데이터셋에는 존재하지 않았다.
- `radio_sessions`(19행)는 이미 목표 한글 라벨(`지침`, `2세대`, `댄스` 등)을 그대로 사용 중이었다. `user_preferences`는 0행.
- Flyway/Liquibase는 도입되어 있지 않으며, `src/main/resources/sql/migration_add_*.sql` 수동 SQL 파일 컨벤션을 사용 중이었다(신규 도구 도입하지 않음).

## 2. 최종 감정 코드 목록 (정확히 7개)

| 코드 | 한글 라벨 |
|---|---|
| TIRED | 지침 |
| EXCITED | 설렘 |
| NOSTALGIC | 회상 |
| CONFIDENT | 자신감 |
| COMFORT | 위로 |
| LONELY | 외로움 |
| ENERGETIC | 신남 |

## 3. 최종 세대 코드 목록

| 코드 | 한글 라벨 | 비고 |
|---|---|---|
| SECOND | 2세대 | 곡의 실제 세대값으로 저장 가능 |
| THIRD | 3세대 | 곡의 실제 세대값으로 저장 가능 |
| ALL | 전체 | 검색 필터 해제 조건. `generations` 마스터 테이블에는 넣지 않음 |

## 4. 최종 장르 코드 목록 (정확히 6개)

| 코드 | 한글 라벨 |
|---|---|
| DANCE | 댄스 |
| BALLAD | 발라드 |
| RNB | R&B |
| HIPHOP | 힙합 |
| IDOL | 아이돌 |
| OST | OST |

## 5. 기존 값 → 표준 코드 매핑표

### 감정 (`MoodNormalizer`, `mood_aliases` 테이블과 동일)

| 표준 코드 | 매핑되는 기존 값 |
|---|---|
| TIRED | 지침, 지친, 피곤함, 피곤한, 무기력, 힘듦 |
| EXCITED | 설렘, 설레는, 설레임 |
| NOSTALGIC | 회상, 그리움, 그리운, 추억, 추억회상, 과거회상, 향수 |
| CONFIDENT | 자신감, 자신있는, 당당함, 당당한, 파이팅 |
| COMFORT | 위로, 힐링, 편안함, 안정, 따뜻함 |
| LONELY | 외로움, 외로운, 쓸쓸함, 쓸쓸한, 고독 |
| ENERGETIC | 신남, 신나는, 흥겨움, 즐거움, 행복한, 활기 |

**검토 대상(자동 매핑하지 않음, `MoodNormalizer.isReviewCandidate()`로 식별 가능):** 환상, 몽환, 감성, 청량, 강렬함, 슬픔, 슬픈, 행복(단독)

### 세대 (`GenerationNormalizer`)

| 표준 코드 | 매핑되는 기존 값 |
|---|---|
| SECOND | 2세대, 2 세대, second, GEN2 |
| THIRD | 3세대, 3 세대, third, GEN3 |
| ALL | 전체, all |

**서비스 대상 외(자동 변환하지 않음):** 1세대, 4세대, 5세대 — 실제 DB에 13/28/29건 존재, 검토 대상으로 분리(7절 참고).

### 장르 (`GenreNormalizer`)

| 표준 코드 | 매핑되는 기존 값 |
|---|---|
| DANCE | 댄스, Dance, dance, 댄스곡, EDM, 일렉트로닉 댄스 |
| BALLAD | 발라드, Ballad, 발라드곡, 팝 발라드 |
| RNB | R&B, RnB, RNB, r&b, 알앤비, 소울 |
| HIPHOP | 힙합, Hip-Hop, HipHop, 랩, Rap |
| IDOL | 아이돌, Idol, 아이돌 음악, 아이돌곡 |
| OST | OST, O.S.T, 드라마 OST, 영화 OST, 애니메이션 OST, 사운드트랙 |

## 6. 수정한 파일 전체 목록

**DB / SQL**
- `RevibeK/RevibeK/src/main/resources/sql/migration_add_mood_generation_genre_normalization.sql` (신규)

**백엔드 Java (신규)**
- `RevibeK/RevibeK/src/main/java/com/ssafy/revibek/mood/MoodCode.java`
- `RevibeK/RevibeK/src/main/java/com/ssafy/revibek/mood/MoodNormalizer.java`
- `RevibeK/RevibeK/src/main/java/com/ssafy/revibek/mood/GenerationCode.java`
- `RevibeK/RevibeK/src/main/java/com/ssafy/revibek/mood/GenerationNormalizer.java`
- `RevibeK/RevibeK/src/main/java/com/ssafy/revibek/mood/GenreCode.java`
- `RevibeK/RevibeK/src/main/java/com/ssafy/revibek/mood/GenreNormalizer.java`

**백엔드 Java (수정)**
- `RevibeK/RevibeK/src/main/java/com/ssafy/revibek/radio/service/RadioService.java` — `recommendSongs()` 폴백 체인에 moodCode 기반 4단계 추가, ALL 세대 시 세대 조건 생략
- `RevibeK/RevibeK/src/main/java/com/ssafy/revibek/song/mapper/SongDao.java` — `findRecommendedSongsByMoodCode*` 4종, `selectMoodCodesBySongId` 추가
- `RevibeK/RevibeK/src/main/java/com/ssafy/revibek/qdrant/QdrantService.java` — `SongDao` 의존성 추가, payload에 `mood_codes`/`generation`/`genre_code` 추가

**MyBatis**
- `RevibeK/RevibeK/src/main/resources/mapper/song/SongMapper.xml` — `findRecommendedSongsByMoodCode*` 4종, `selectMoodCodesBySongId` 추가(기존 쿼리는 그대로 보존)

**프론트엔드 (신규)**
- `RevibeK_FE/src/constants/radioOptions.js`
- `RevibeK_FE/src/constants/__tests__/radioOptions.test.js`

**프론트엔드 (수정)**
- `RevibeK_FE/src/components/radio/MoodSelector.vue`
- `RevibeK_FE/src/components/radio/GenreChips.vue`
- `RevibeK_FE/src/components/radio/EraChips.vue`
(세 컴포넌트 모두 라벨 배열을 공통 constants에서 가져오도록 교체. emit 값은 기존과 동일한 한글 라벨 유지 — API payload/필드명 무변경)

**테스트 (신규/수정)**
- `RevibeK/RevibeK/src/test/java/com/ssafy/revibek/mood/MoodNormalizerTest.java` (신규)
- `RevibeK/RevibeK/src/test/java/com/ssafy/revibek/mood/GenerationNormalizerTest.java` (신규)
- `RevibeK/RevibeK/src/test/java/com/ssafy/revibek/mood/GenreNormalizerTest.java` (신규)
- `RevibeK/RevibeK/src/test/java/com/ssafy/revibek/radio/service/RadioServiceTest.java` (수정 — moodCode 우선/폴백/ALL세대 테스트 3건 추가)
- `RevibeK/RevibeK/src/test/java/com/ssafy/revibek/qdrant/QdrantServiceTest.java` (수정 — `QdrantService` 생성자에 `SongDao` 인자 추가됨에 따라 테스트 생성 코드 보정)

**변경하지 않은 것(의도적)**
- `songs.mood`, `songs.genre`, `songs.generation`, `songs.era` 컬럼 자체 — 삭제/UPDATE 없음
- `radio_sessions.mood/era/genre/desired_mood` 컬럼명 및 저장 형식
- 기존 9종 레거시 mood/era/genre 매퍼 쿼리 — 전부 보존
- `RadioRequestDto`/`RadioCreateRequestDto`/FE API payload 필드명

## 7. 생성한 DB 테이블 및 인덱스

| 테이블 | 설명 |
|---|---|
| `moods` | 감정 마스터 (code PK, label UNIQUE) — 7행 |
| `mood_aliases` | 감정 동의어 → 코드 (SQL 이관 쿼리용 보조 테이블) — 36행 |
| `song_moods` | 곡↔감정 N:M (PK(song_id, mood_code), FK→songs/moods ON DELETE CASCADE) |
| `generations` | 세대 마스터 (code PK, label UNIQUE) — 2행(SECOND, THIRD만) |
| `genres` | 장르 마스터 (code PK, label UNIQUE) — 6행 |

인덱스: `idx_song_moods_mood_song (mood_code, song_id)` 생성 완료.

## 8. 데이터 마이그레이션 결과 (실제 DB 적용)

마이그레이션 SQL을 로컬 `kpop_radio` DB에 **실제로 적용**했다(`mysql ... < migration_add_mood_generation_genre_normalization.sql`, 에러 없이 종료).

적용 후 실제 조회 결과:

```
SELECT * FROM moods ORDER BY code;
-> COMFORT/위로, CONFIDENT/자신감, ENERGETIC/신남, EXCITED/설렘, LONELY/외로움, NOSTALGIC/회상, TIRED/지침 (7행)

SELECT COUNT(*) FROM song_moods;
-> 0
```

`songs.mood`가 마이그레이션 시점 기준 전 행 NULL이었기 때문에, 쉼표 분리 → song_moods 이관 쿼리는 **실제로 0건을 삽입**했다. 이는 데이터가 비어 있어서 발생한 정상적인 결과이며, 가짜 데이터를 채워넣지 않았다. 향후 `songs.mood`가 채워지면 동일 SQL을 재실행해도 안전하게(IGNORE 기반) 그 시점 데이터를 이관한다.

미매핑 토큰 조회(7-2번 검증 SQL) 결과도 0건(이관 대상 자체가 없으므로 당연한 결과).

## 9. 2·3세대 외 데이터 / 미매핑 장르 목록 (실제 DB 조회)

```
SELECT DISTINCT generation FROM songs WHERE generation NOT IN ('2세대','3세대');
-> 1세대 (13건), 4세대 (28건), 5세대 (29건)   -- 검토 대상, 임의 변환하지 않음

SELECT DISTINCT genre FROM songs WHERE genre NOT IN ('댄스','발라드','R&B','힙합','아이돌','OST');
-> 미분류 (1114건, 전체)                      -- 분류 근거 없음, 임의 확정하지 않음
```

장르는 전체 곡이 `'미분류'`라서 6개 코드 중 하나로 자동 분류할 근거 데이터(가사/태그 등)가 없다. 이번 작업에서는 `genres` 마스터 테이블과 `GenreNormalizer` 유틸만 준비했고, 실제 곡별 장르 분류는 추후 별도 작업(메타데이터 수집 또는 수동 분류)이 필요하다.

## 10. 중복 UUID 분석 결과

```
SELECT id, COUNT(*) FROM songs GROUP BY id HAVING COUNT(*) > 1;        -> 0건
SELECT youtube_id, COUNT(*) FROM songs GROUP BY youtube_id HAVING COUNT(*) > 1; -> 0건
```

현재 DB에는 중복 PK/중복 youtube_id가 존재하지 않는다. 사용자가 우려한 "중복 UUID" 문제는 현재 데이터셋에는 실재하지 않음을 확인했다(가짜 충돌 사례를 만들어 보고하지 않음).

## 11. era/generation 충돌 목록

`era`(90s/00s/10s/20s/미분류/NULL)와 `generation`(1~5세대/NULL)은 각각 다른 축(연대 vs 세대)을 표현하며 의미가 겹치지 않는다(예: `era=00s`이면서 `generation=2세대`). 동일 의미 중복 표현 사례는 발견되지 않았으므로, 두 컬럼 모두 그대로 유지했다.

## 12. 기존 ID 유지 / 신규 곡 upsert 정책

이번 작업 범위에서는 곡 데이터 일괄 적재(원본 파일 → DB)를 수행하지 않았다(이관 대상이 없어 실행할 적재 작업 자체가 없었음). 신규 곡 적재 시 따라야 할 정책은 다음과 같이 정의해 두었으며, 추후 적재 스크립트 작성 시 이 순서를 따른다:
1. `youtube_id`로 기존 곡 검색(`SongDao.selectSongByYoutubeId` 이미 존재)
2. 없으면 정규화된 `youtube_url`로 검색
3. 없으면 `title + artist + type`을 보조 기준으로 검색
4. 기존 곡이면 `songs.id` 유지, 신규 곡이면 새 UUID 발급
5. `song_moods`/Qdrant upsert는 확정된 `songs.id`로 수행

`songs` 전체 삭제, UUID 일괄 교체는 수행하지 않았다(애초에 불필요했음 — 중복이 없었으므로).

## 13. 추천 쿼리 변경 내용

`SongMapper.xml`에 `song_moods` JOIN 기반 신규 쿼리 4종(`findRecommendedSongsByMoodCodeEraGenre/MoodCodeEra/MoodCodeGenre/MoodCode`)을 추가했다. `RadioService.recommendSongs()`의 폴백 순서는 다음과 같이 확장되었다(괄호 안은 신규):

1. **(신규) moodCode+era+generation+genre** → 2. 레거시 mood+era+generation+genre
2. **(신규) moodCode+era+generation** → 4. 레거시 mood+era+generation
3. **(신규) moodCode+genre** → 6. 레거시 mood+genre
4. **(신규) moodCode** → 8. 레거시 mood
5. era+genre → 10. era → 11. genre → 12. 사용자 선호 → 13. 인기곡 (기존과 동일, 변경 없음)

세대가 "전체"(ALL)로 정규화되면 신규 moodCode 단계 중 세대를 조건으로 쓰는 1·2단계는 건너뛰고, 세대 무관 단계(moodCode+genre, moodCode 단독)부터 시도한다. 레거시 단계는 영향받지 않는다(기존 동작 100% 보존).

## 14. 프론트 선택지 변경 내용

`RevibeK_FE/src/constants/radioOptions.js`에 `MOOD_OPTIONS`(7개)/`GENERATION_OPTIONS`(SECOND/THIRD/ALL)/`GENRE_OPTIONS`(6개)를 신설했다. 기존에 `MoodSelector.vue`/`GenreChips.vue`/`EraChips.vue` 3곳에 중복 하드코딩되어 있던 한글 라벨 배열을 이 constants에서 가져오도록 교체했다. **emit되는 값은 기존과 동일한 한글 라벨**이라 API payload/필드명에 변화가 없다(회귀 위험 없음). 코드값(value)은 constants에 함께 정의해 두어, 추후 코드 기반 전송으로 전환할 때 재사용할 수 있다.

## 15. Qdrant 동기화 결과

`QdrantService.buildPayload()`에 다음 필드를 추가했다(기존 `title/genre/era/type/youtube_id`는 그대로 유지):
- `mood_codes`: 해당 곡의 `song_moods` 코드 목록 (없으면 필드 자체를 생략)
- `generation`: `songs.generation`이 정확히 `2세대`/`3세대`일 때만 `SECOND`/`THIRD`로 코드화(그 외는 생략, ALL 저장 금지)
- `genre_code`: `GenreNormalizer`로 매칭되면 코드화(현재 전 곡이 `'미분류'`라 실질적으로 항상 생략됨)

재색인은 기존 `QdrantService.upsertSongs()` / `EmbeddingQdrantSyncService` 배치 경로를 그대로 사용한다(별도 파이프라인 신설 없음) — 다음 upsert 호출 시 새 payload 필드가 자동으로 반영된다. point ID는 여전히 `UUID.fromString(song.getId())`로, MySQL `songs.id`와 동일하게 유지된다.

## 16. songs.mood fallback 유지 방식

기존 9종 레거시 mood/era/genre 쿼리는 `SongMapper.xml`에서 삭제하지 않았다. `RadioService.recommendSongs()`는 신규 moodCode 단계에서 결과가 없을 때만 레거시 단계를 호출하므로, `song_moods`가 비어 있는 현재 상태에서도 추천 흐름은 기존과 동일하게 동작한다(레거시 단계가 그대로 모든 결과를 책임짐).

## 17. 추가한 테스트와 결과

| 테스트 | 건수 | 결과 |
|---|---|---|
| `MoodNormalizerTest` | 5 | 통과 |
| `GenerationNormalizerTest` | 5 | 통과 |
| `GenreNormalizerTest` | 6 | 통과 |
| `RadioServiceTest` (신규 3건 포함 전체) | 10 | 통과 (moodCode 우선/레거시 폴백/ALL세대 케이스 포함) |
| `QdrantServiceTest` (생성자 보정) | 5 | 통과 |
| `radioOptions.test.js` (FE) | 3 | 통과 |

## 18. 백엔드 컴파일 및 테스트 결과

```
cd RevibeK/RevibeK
./mvnw.cmd -DskipTests compile   -> BUILD SUCCESS
./mvnw.cmd test                  -> Tests run: 50, Failures: 0, Errors: 0, Skipped: 0 / BUILD SUCCESS
```

## 19. 프론트 테스트/빌드 결과

```
cd RevibeK_FE
npm run test -- --run   -> Test Files 4 passed (4), Tests 14 passed (14)
npm run build            -> built in 617ms (성공)
```

## 20. 실제 DB 적용 여부

**적용함.** 로컬 `kpop_radio`(localhost:3306, 사용자 `ssafy`)에 `migration_add_mood_generation_genre_normalization.sql`을 직접 실행했고, 위 8절의 조회 결과로 적용을 확인했다. additive-only(CREATE TABLE IF NOT EXISTS / INSERT IGNORE)라 기존 1,114개 곡 데이터나 다른 테이블에는 영향이 없었다.

## 21. 사용자가 직접 실행해야 할 SQL/명령

- 다른 환경(스테이징/운영 등)에도 동일하게 적용하려면:
  ```
  mysql -h <host> -P <port> -u <user> -p kpop_radio < RevibeK/RevibeK/src/main/resources/sql/migration_add_mood_generation_genre_normalization.sql
  ```
- 향후 실제로 곡별 감정/장르 태깅 데이터가 들어오면(예: `UPDATE songs SET mood='위로, 외로움' WHERE id=...`), 위 SQL을 재실행하면 `song_moods`에 자동 반영된다(재실행 안전).
- `qdrant.enabled=true`인 환경에서 신규 payload 필드를 실제 컬렉션에 반영하려면, 기존 운영 절차대로 `upsertSongs()`(또는 `/api/qdrant/embed` 재색인 엔드포인트)를 한 번 더 호출해야 한다. 이번 작업에서는 Qdrant 서버 자체에 접속해 실제 upsert를 실행하지 않았다(코드 변경만 했고, 외부 Qdrant 서버 가동 여부를 확인하지 않았기 때문 — 실행 안 한 것을 실행했다고 적지 않음).

## 22. 아직 남아 있는 제한사항

- `songs.genre`가 전 곡 `'미분류'`라서, 6개 장르 코드로의 실제 곡별 분류는 이번 작업에서 채워지지 않았다(분류 근거 데이터 부재).
- `songs.mood`가 전 곡 NULL이라서, `song_moods`에 실제 감정 태깅 데이터가 없다. 추천 시스템은 현재도(과거와 동일하게) era/generation/genre/선호/인기도 기반 폴백으로만 동작한다.
- 1·4·5세대 곡(70건)은 서비스 대상(2·3세대) 밖이지만 삭제하지 않고 그대로 두었다 — 별도 정책 결정(노출 제외 여부 등)이 필요하다.
- Qdrant 실 서버에 대한 재색인(신규 payload 필드 반영)은 코드만 준비되었고 실제로 실행하지 않았다.

## 23. 추후 songs.mood/genre 등 deprecated 컬럼 삭제 전 필요한 조건

1. 실제 곡별 mood/genre 태깅 데이터가 `song_moods`/표준 genre 코드로 충분히 채워져, 레거시 컬럼 없이도 추천 결과가 동일하거나 더 나음을 확인.
2. `SongMapper.xml`의 9종 레거시 mood/era/genre 쿼리 사용 빈도를 로깅해, 신규 moodCode 경로로 완전히 대체되었는지 확인.
3. `radio_sessions`/`user_preferences`에 남아있는 한글 문자열 의존 코드가 모두 표준 코드 기반으로 전환되었는지 확인.
4. 위 조건이 충족된 뒤, 별도 마이그레이션으로 `songs.mood` 컬럼을 DROP(이번 작업 범위 아님, 즉시 삭제하지 않음).
