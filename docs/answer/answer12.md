# Qdrant 유사곡 결과(youtubeUrl)를 플레이리스트에 저장하는 기능 설계

`docs/prompt/architecture.md` 분석 내용을 기준으로, `GET /api/qdrant/similar/{songId}` 응답(`VectorSearchResponseDto.results`, 즉 `SongDto[]`)에 들어있는 `youtubeUrl`(과 해당 곡)을 플레이리스트에 저장하는 기능을 어떤 구조로 만들면 좋을지 정리한다.

## 1. 현재 구조 확인

### 1.1 Qdrant 유사곡 응답 (`VectorSearchResponseDto`)
```json
GET /api/qdrant/similar/{songId}?limit=10
{
  "source": "qdrant",
  "message": "Qdrant vector search result.",
  "results": [
    {
      "id": "s011-0000-0000-0000-000000000011",
      "title": "캔디 (AI 리마스터)",
      "artist": "H.O.T",
      "genre": "댄스",
      "era": "2세대",
      "youtubeUrl": "https://www.youtube.com/watch?v=dummy011",
      "youtubeId": "dummy011",
      ...
    }
  ]
}
```
- `results`의 각 항목은 `songs` 테이블에 실제로 존재하는 `SongDto`이며, `id`(=songId)와 `youtubeUrl`을 모두 포함한다.

### 1.2 플레이리스트 저장 구조 (DB)
- `playlist_songs` 테이블은 `playlist_id`, `song_id`, `order_num`만 저장한다(`PlaylistMapper.xml`).
- `youtube_url`은 `playlist_songs`에 직접 저장되지 않고, 조회 시 `songs` 테이블과 JOIN해서 내려준다(`selectPlaylistItems`).
- 즉 **"url을 저장한다"는 것은 실제로는 "그 url을 가진 songId를 playlist_songs에 추가한다"**는 의미가 된다. (URL을 별도 컬럼으로 저장하는 방식은 기존 스키마와 맞지 않고, song 정보가 바뀌면 정합성이 깨지므로 권장하지 않음.)

### 1.3 기존 곡 추가 API
```json
POST /api/playlists/{playlistId}/items
{ "songId": "s011-0000-0000-0000-000000000011" }
```
- `PlaylistService.addItem()`이 `songId` 존재 여부 확인 → 중복 체크 → `order_num` 계산 → insert.
- 한 번에 1곡만 추가 가능 (반복 호출 필요).

## 2. 요구사항 해석

"Qdrant 결과값에서 url 부분을 playlist에 저장" = Qdrant 유사곡 추천 리스트를 보여주고, 사용자가 그중 마음에 드는 곡(들)을 플레이리스트에 담는 기능. 핵심은 **Qdrant 응답의 `results[].id`(songId)를 그대로 `POST /api/playlists/{playlistId}/items`에 전달**하면 되고, `youtubeUrl`은 DB JOIN으로 자동으로 따라온다.

다만 Qdrant 결과는 보통 여러 곡(N개)을 한 번에 보여주므로, "전체 담기" UX를 고려하면 **여러 곡을 한 번에 추가하는 배치 API**가 있는 게 좋다. 두 가지 방식을 비교한다.

## 3. 설계 옵션

### 옵션 A. 백엔드 변경 없이 FE에서 반복 호출 (최소 구현)
- FE: `GET /api/qdrant/similar/{songId}` → 결과 리스트 렌더링 → 사용자가 곡 선택 → 선택된 곡마다 `POST /api/playlists/{playlistId}/items` 를 `Promise.all`로 반복 호출.
- 장점: 백엔드 변경 0, 가장 빠르게 구현 가능.
- 단점: N번 API 호출(트랜잭션 단위가 곡별로 분리됨), 일부만 실패했을 때 처리(이미 담긴 곡 중복 에러 등)를 FE에서 곡별로 핸들링해야 함.

### 옵션 B. 배치 추가 API 신설 (권장)
`PlaylistController`에 다건 추가 엔드포인트를 추가한다.

```
POST /api/playlists/{playlistId}/items/batch
```

**Request Body**
```json
{
  "songIds": [
    "s011-0000-0000-0000-000000000011",
    "s012-0000-0000-0000-000000000012"
  ]
}
```
- Qdrant 응답의 `results[].id` 값들을 FE에서 그대로 모아 보내면 됨.

**Response Body**
```json
{
  "added": [
    { "id": "ps05-...", "playlistId": "pl01-...", "songId": "s011-...", "title": "...", "youtubeUrl": "...", "orderNum": 4, "addedAt": "..." }
  ],
  "skipped": [
    { "songId": "s012-0000-0000-0000-000000000012", "reason": "이미 플레이리스트에 추가된 곡입니다." }
  ]
}
```

장점:
- "Qdrant 추천 결과 전체 담기" 같은 1-클릭 UX 가능.
- 곡별 중복/존재하지 않음 등의 실패를 건너뛰고(skip) 나머지는 정상 처리 → 트랜잭션을 곡 단위로 분리해 일부 실패가 전체를 막지 않음.
- 기존 `addItem()` 로직을 재사용하므로 변경 범위가 작음.

## 4. 옵션 B 상세 구현 구조

### 4.1 DTO 추가 (`playlist/dto`)

`PlaylistItemBatchRequestDto`
```java
package com.ssafy.revibek.playlist.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistItemBatchRequestDto {

    @NotEmpty(message = "songIds는 1개 이상이어야 합니다.")
    private List<String> songIds;
}
```

`PlaylistItemBatchResponseDto`
```java
package com.ssafy.revibek.playlist.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistItemBatchResponseDto {

    private List<PlaylistItemDto> added;
    private List<SkippedItem> skipped;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkippedItem {
        private String songId;
        private String reason;
    }
}
```

### 4.2 Service 추가 (`PlaylistService`)
`addItem(userId, playlistId, PlaylistItemDto)`을 곡별로 호출하고, `IllegalArgumentException`(존재하지 않는 곡 / 이미 추가된 곡)을 잡아 `skipped`에 모으는 래퍼 메서드를 추가한다.

```java
@Transactional
public PlaylistItemBatchResponseDto addItems(String userId, String playlistId, List<String> songIds) {
    List<PlaylistItemDto> added = new ArrayList<>();
    List<PlaylistItemBatchResponseDto.SkippedItem> skipped = new ArrayList<>();

    for (String songId : songIds) {
        try {
            PlaylistItemDto item = addItem(userId, playlistId,
                PlaylistItemDto.builder().songId(songId).build());
            added.add(item);
        } catch (IllegalArgumentException e) {
            skipped.add(PlaylistItemBatchResponseDto.SkippedItem.builder()
                .songId(songId)
                .reason(e.getMessage())
                .build());
        }
    }

    return PlaylistItemBatchResponseDto.builder()
        .added(added)
        .skipped(skipped)
        .build();
}
```
- 각 곡 추가는 기존 `addItem()`을 그대로 호출하므로 `order_num` 계산, 중복 체크, 존재 검증 로직을 재사용한다.
- 곡 하나의 실패가 전체 요청을 롤백하지 않도록, 곡 단위 예외를 여기서 잡아서 `skipped`로 분리한다(메서드 전체를 `@Transactional`로 감싸되, catch로 처리하므로 부분 성공이 가능).

> 만약 "한 곡이라도 실패하면 전체 롤백"을 원한다면 catch 없이 그대로 던지고 `@Transactional`이 전체 롤백하도록 두면 된다. 다만 Qdrant 추천 결과를 "여러 개 골라 담기" UX에서는 일부 중복/실패는 흔하므로 skip 방식이 사용자 경험상 더 적합하다.

### 4.3 Controller 추가 (`PlaylistController`)
```java
@PostMapping("/{playlistId}/items/batch")
public ResponseEntity<PlaylistItemBatchResponseDto> addItems(
        Authentication authentication,
        @PathVariable String playlistId,
        @Valid @RequestBody PlaylistItemBatchRequestDto request) {
    return ResponseEntity.ok(
        playlistService.addItems(authentication.getName(), playlistId, request.getSongIds())
    );
}
```
- `SecurityConfig`에서 `/api/playlists/**`는 이미 `authenticated`로 설정되어 있어 별도 보안 설정 불필요.
- 매퍼 XML/테이블 변경 없음 — 기존 `playlist_songs`, `selectPlaylistItems` 그대로 사용.

### 4.4 FE 연동 흐름
1. Explore/곡 상세 화면에서 `GET /api/qdrant/similar/{songId}?limit=10` 호출 → `results`(SongDto[]) 렌더링(제목/아티스트/`youtubeUrl` 썸네일·임베드).
2. 사용자가 체크박스 등으로 담을 곡을 선택(또는 "전체 추천곡 담기" 버튼).
3. `AddToPlaylistModal`에서 플레이리스트 선택(`GET /api/playlists`, 없으면 `POST /api/playlists`).
4. 선택된 곡들의 `id`(songId)만 모아 `POST /api/playlists/{playlistId}/items/batch` 호출.
   ```js
   await playlistApi.addItemsBatch(playlistId, {
     songIds: selectedResults.map(song => song.id)
   })
   ```
5. 응답의 `skipped`가 있으면 "이미 담긴 곡 N개는 제외되었습니다" 같은 토스트로 안내, `added`는 플레이리스트 상세를 다시 조회(`GET /api/playlists/{playlistId}`)하거나 응답값으로 즉시 리스트에 반영.

## 5. 정리
- `playlist_songs`는 `song_id`만 저장하고 `youtube_url`은 `songs` 테이블 JOIN으로 내려주는 구조이므로, Qdrant 결과의 `youtubeUrl`을 별도 컬럼으로 저장할 필요는 없고 **해당 곡의 `id`를 플레이리스트에 추가**하는 것으로 충분하다.
- 단일 곡 추가는 기존 `POST /api/playlists/{playlistId}/items`를 그대로 사용 가능(옵션 A).
- 여러 곡을 한 번에 담는 UX가 필요하면 `POST /api/playlists/{playlistId}/items/batch`(옵션 B, `PlaylistItemBatchRequestDto`/`PlaylistItemBatchResponseDto`, `PlaylistService.addItems()`)를 추가하는 것을 권장한다. 기존 `addItem()` 로직과 DB 스키마를 그대로 재사용하면서, 일부 곡의 중복/존재하지 않음 오류를 `skipped`로 분리해 나머지는 정상 처리할 수 있다.
