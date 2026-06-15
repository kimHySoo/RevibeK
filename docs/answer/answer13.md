# 라디오 생성 시 Qdrant 추천 결과를 "플레이리스트"로 자동 생성하는 기능 설계

`POST /api/radio` 호출 시 추천곡을 단순 리스트(`recommendedSongs`)로만 내려주는 현재 구조를 확장해, **Qdrant 유사곡 검색 결과를 활용해 추천곡을 구성하고, 그 결과를 곡 단위가 아니라 "플레이리스트" 단위로 자동 생성**하는 기능을 어떻게 설계할지 정리한다. (기존 [answer12.md](answer12.md)에서 설계한 배치 추가 API를 그대로 재사용한다.)

## 1. 현재 흐름 분석

### 1.1 추천곡 산출 (`RadioService.recommendSongs`)
- 현재는 **Qdrant를 전혀 사용하지 않고**, `SongDao`의 mood/era/genre 조합 쿼리를 단계별 fallback으로 호출해 `List<SongDto>`를 얻는다 (`safeFindByMoodEraGenre` → ... → `safeFindTopScore`).
- 결과를 `RecommendedSongResponseDto` 리스트로 변환해 `recommendedSongs`로 응답하고, `radio_recommendations` 테이블에 세션별로 저장한다.

### 1.2 Qdrant 서비스 (`QdrantService`)
- `searchSimilar(String songId, int limit)` : 특정 곡(`songId`) 벡터 기준 유사곡 N개의 **songId 리스트**를 반환 (`qdrant.enabled=false`면 빈 리스트).
- 곡 하나를 "시드(seed)"로 줘야 동작하는 구조이며, mood/genre 같은 텍스트 조건으로 직접 벡터 검색하는 기능은 없음.

### 1.3 플레이리스트 (`PlaylistService`)
- `createPlaylist(userId, PlaylistDto)` : 플레이리스트 생성.
- (answer12에서 설계) `addItems(userId, playlistId, List<songId>)` : 여러 곡을 한 번에 추가, 중복/존재하지 않는 곡은 `skipped`로 분리.

## 2. 설계 목표

1. `POST /api/radio` 호출 한 번으로:
   - DB 추천 + **Qdrant 유사곡 검색으로 추천 풀(pool)을 확장**하고,
   - 추천곡들을 **자동 생성된 플레이리스트에 곡 단위로 저장**한다.
2. 응답에는 기존 `recommendedSongs`(라디오 결과 화면 표시용)와 함께 **새로 생성된 `playlistId`**를 내려준다.
3. FE는 라디오 결과 화면에서 "이 라디오 플레이리스트 보기 / 더 담기" 같은 진입점을 제공할 수 있다.

## 3. Qdrant를 추천 흐름에 결합하는 방법

mood/genre 텍스트 조건만으로는 Qdrant를 직접 쿼리할 수 없으므로, **"DB 추천 결과의 1순위 곡을 시드로 사용해 Qdrant 유사곡을 확장"**하는 2단계 방식을 제안한다.

```
1) recommendSongs(...) // 기존 DB fallback 체인 → seedSongs (예: limit=5)
2) seedSongs[0](가장 점수 높은 곡)을 시드로 qdrantService.searchSimilar(seedId, expandLimit) 호출
3) 반환된 songId들을 songService.getSongById()로 SongDto 변환
4) seedSongs + 유사곡(중복 제거) → 최종 추천 풀 (예: 최대 8~10곡)
```

- Qdrant가 비활성화(`qdrant.enabled=false`)거나 검색 결과가 없으면 `searchSimilar()`가 빈 리스트를 반환하므로, **자동으로 기존 DB 추천만 사용하는 fallback이 된다** (코드 분기 추가 불필요).
- `recommendationSource`에 Qdrant 확장 여부를 표시하고 싶다면 `"DB_MOOD_ERA_GENRE+QDRANT_EXPANDED"`처럼 접미사를 붙이는 정도로 충분하다(선택 사항).

### 3.1 `RadioService` 변경 스케치
```java
private List<SongDto> expandWithQdrant(List<SongDto> seedSongs, String excludedKeywords, int totalLimit) {
    if (seedSongs.isEmpty()) return seedSongs;

    List<SongDto> result = new ArrayList<>(seedSongs);
    Set<String> seenIds = result.stream().map(SongDto::getId).collect(Collectors.toSet());

    String seedId = seedSongs.get(0).getId();
    List<String> similarIds = qdrantService.searchSimilar(seedId, totalLimit);

    for (String id : similarIds) {
        if (result.size() >= totalLimit) break;
        if (seenIds.contains(id)) continue;
        SongDto song = songService.getSongById(id);
        if (song != null) {
            result.add(song);
            seenIds.add(id);
        }
    }
    return result;
}
```
- `createRadio()`에서 `recommendSongs(...)`로 얻은 `recommendationResult.songs()`를 위 메서드로 한 번 감싸서 확장한다.
- `totalLimit`은 `DEFAULT_RECOMMENDATION_LIMIT`보다 크게(예: 8~10) 잡아 "플레이리스트"로서 의미 있는 분량을 만든다.

## 4. 추천곡 → 플레이리스트 자동 생성

`createRadio()` 마지막 단계, `recommendedSongs`(`RecommendedSongResponseDto` 리스트)를 만든 직후에 플레이리스트를 생성하고 곡을 채운다.

```java
PlaylistDto playlist = playlistService.createPlaylist(userId, PlaylistDto.builder()
    .name(buildPlaylistName(request))     // 예: "그리운 2세대 댄스 라디오 - 2026.06.14"
    .moodTag(request.getMood())
    .isPublic(false)
    .build());

List<String> songIds = recommendedSongs.stream()
    .map(RecommendedSongResponseDto::getSongId)
    .filter(StringUtils::hasText)
    .toList();

playlistService.addItems(userId, playlist.getId(), songIds);
```

- `addItems()`는 answer12에서 설계한 배치 추가 메서드(중복/미존재 곡은 `skipped`로 건너뛰고 나머지는 정상 추가)를 그대로 사용한다.
- 라디오 세션 1회 생성 = 플레이리스트 1개 자동 생성. (재생성 시마다 새 플레이리스트가 생기는 점은 5장에서 보완 옵션 제시)

### 4.1 `buildPlaylistName` 예시
```java
private String buildPlaylistName(RadioCreateRequestDto request) {
    String mood = StringUtils.hasText(request.getMood()) ? request.getMood() : "감성";
    String era = StringUtils.hasText(request.getEra()) ? request.getEra() : "";
    String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
    return String.format("%s %s 라디오 - %s", mood, era, date).replaceAll("\\s+", " ").trim();
}
```

## 5. DTO / Controller 변경

### 5.1 `RadioCreateResponseDto`에 `playlistId` 추가
```java
public class RadioCreateResponseDto {
    ...
    private String playlistId;   // 자동 생성된 플레이리스트 ID
    ...
}
```

### 5.2 응답 예시
```json
{
  "radioSessionId": "f3a1...",
  "userId": "u001-...",
  "mood": "그리운",
  "era": "2세대",
  "genre": "댄스",
  "djMent": "안녕하세요, DJ 리아예요. ...",
  "recommendationSource": "DB_MOOD_ERA_GENRE+QDRANT_EXPANDED",
  "playlistId": "pl09-0000-0000-0000-000000000099",
  "tts": { "mode": "google-tts", "text": "...", "audioUrl": null },
  "recommendedSongs": [
    { "songId": "s011-...", "title": "캔디 (AI 리마스터)", "artist": "H.O.T", "youtubeUrl": "...", "score": 85.4, "reason": "..." },
    { "songId": "s014-...", "title": "...", "artist": "...", "youtubeUrl": "...", "score": 78.1, "reason": "..." }
  ]
}
```

### 5.3 `RadioService` 의존성 추가
- `PlaylistService`, `QdrantService`, `SongService`(또는 `SongDao`로 ID→SongDto 조회) 주입 추가.
- 순환 의존성 없음: `PlaylistService`/`QdrantService`는 `RadioService`를 참조하지 않음.

## 6. FE 연동 변화

- **Radio Create → Result 흐름**은 동일하지만, 결과 화면(`RadioResultPage`)에 `playlistId`를 store에 저장.
- `RecommendedSongCard`별 "플레이리스트에 추가" 버튼은 그대로 두되, 화면 상단에 **"이 라디오의 플레이리스트 보기"** 버튼을 추가해 `playlistState.currentSession.playlistId`로 `/playlists/:id`로 이동.
- 추천곡이 늘어났으므로(기본 5 → 8~10곡), `RecommendedSongCard` 리스트 영역은 스크롤/캐러셀 형태로 표시하는 것을 권장.

## 7. 고려/보완 사항

1. **매 생성마다 새 플레이리스트 생성됨**: 라디오를 여러 번 만들면 플레이리스트가 계속 누적된다.
   - 보완책: 플레이리스트 이름에 라디오 세션 ID/생성일을 포함해 식별 가능하게 하거나, "라디오 히스토리"(`GET /api/radio/me`)에서 각 세션과 연결된 `playlistId`를 함께 보여주면 자연스러운 아카이브가 된다.
   - 만약 "최신 라디오 결과 1개만 유지"를 원한다면, 세션 생성 시 이전에 자동 생성된 라디오 플레이리스트를 삭제(`deletePlaylist`)하거나 재사용(기존 플레이리스트의 아이템을 비우고 새로 채움)하는 정책이 필요 — 이 경우 `radio_sessions` 테이블에 `playlist_id` 컬럼을 추가해 1:1로 추적하는 것을 권장.
2. **Qdrant 비활성화 환경**: `qdrant.enabled=false`(기본값)이면 `searchSimilar()`가 빈 리스트를 반환 → 추천 풀이 DB 추천(기본 5곡) 그대로 유지됨. 플레이리스트는 5곡으로 생성되며 기능 자체는 정상 동작.
3. **`addItems`의 `skipped` 처리**: 신규 생성된 플레이리스트이므로 중복 곡은 발생하지 않지만, `recommendedSongs`에 `songId`가 비어있는 항목(예: DB에 없는 곡)은 자동으로 `skipped` 처리되어 안전하게 무시된다.
4. **트랜잭션 범위**: `createRadio()` 전체가 `@Transactional`이므로, 플레이리스트 생성·아이템 추가까지 같은 트랜잭션에 포함된다. 라디오 세션 저장은 성공했는데 플레이리스트만 실패하는 상황을 막기 위함.

## 8. 정리 (구현 체크리스트)

- [ ] `RadioService`에 `PlaylistService`, `QdrantService`, `SongService` 의존성 추가
- [ ] `expandWithQdrant()` 메서드 추가 후 `recommendSongs()` 결과에 적용
- [ ] `recommendedSongs` 생성 직후 `playlistService.createPlaylist()` + `playlistService.addItems()` 호출
- [ ] `RadioCreateResponseDto`에 `playlistId` 필드 추가
- [ ] (선택) `radio_sessions` 테이블/매퍼에 `playlist_id` 컬럼 추가해 세션-플레이리스트 1:1 추적
- [ ] FE: `RadioResultPage`에서 `playlistId` 저장 및 "플레이리스트 보기" 버튼/이동 추가
- [ ] `addItems` 배치 API(answer12)가 아직 없다면 선행 구현 필요
