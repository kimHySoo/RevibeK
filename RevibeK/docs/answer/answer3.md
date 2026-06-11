## 1. 구현 가능 여부 최종 판단

구현 가능합니다. 현재 세션은 read-only이므로 파일은 직접 수정하지 않았습니다.

확인된 실제 프로젝트 구조상 `SongDao.java` 경로는 요청에 적힌 `song/dao`가 아니라 아래 경로입니다.

```text
src/main/java/com/ssafy/revibek/song/mapper/SongDao.java
```

또한 기존 `radio_recommendations.order_num`은 요구사항의 `sort_order`로 변경해야 합니다.

전체 교체 코드는 답변 길이 제한으로 한 메시지에 안전하게 포함할 수 없습니다. 아래는 먼저 적용해야 하는 DB migration과 핵심 DTO 전체 코드입니다. 나머지 Java/XML 전체 코드는 여러 메시지로 분리해야 컴파일 가능한 상태로 정확히 제공할 수 있습니다.

## 2. 수정/생성 파일 목록

```text
생성:
src/main/java/com/ssafy/revibek/radio/dto/RadioSelectedSongDto.java
src/main/java/com/ssafy/revibek/radio/dto/RadioSongResponseDto.java

수정:
src/main/java/com/ssafy/revibek/radio/dto/RadioCreateRequestDto.java
src/main/java/com/ssafy/revibek/radio/dto/RadioCreateResponseDto.java
src/main/java/com/ssafy/revibek/radio/controller/RadioController.java
src/main/java/com/ssafy/revibek/radio/service/RadioService.java
src/main/java/com/ssafy/revibek/radio/mapper/RadioMapper.java
src/main/resources/mapper/radio/RadioMapper.xml
src/main/java/com/ssafy/revibek/song/mapper/SongDao.java
src/main/resources/mapper/song/SongMapper.xml
src/main/java/com/ssafy/revibek/playlist/service/PlaylistService.java
src/main/java/com/ssafy/revibek/playlist/mapper/PlaylistMapper.java
src/main/resources/mapper/playlist/PlaylistMapper.xml
```

## 3. DB Migration SQL

```sql
ALTER TABLE playlists
  MODIFY COLUMN name VARCHAR(150) NOT NULL,
  ADD COLUMN description VARCHAR(500) NULL AFTER name,
  ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP;

ALTER TABLE radio_sessions
  ADD COLUMN title VARCHAR(150) NULL AFTER user_id,
  ADD COLUMN playlist_id CHAR(36) NULL AFTER video_type,
  ADD INDEX idx_radio_playlist_id (playlist_id),
  ADD CONSTRAINT fk_radio_session_playlist
    FOREIGN KEY (playlist_id) REFERENCES playlists(id)
    ON DELETE SET NULL;

ALTER TABLE radio_recommendations
  CHANGE COLUMN order_num sort_order TINYINT NOT NULL DEFAULT 1,
  ADD COLUMN title VARCHAR(200) NULL AFTER song_id,
  ADD COLUMN artist VARCHAR(100) NULL AFTER title,
  ADD COLUMN youtube_url VARCHAR(300) NULL AFTER artist,
  ADD COLUMN thumbnail_url VARCHAR(500) NULL AFTER youtube_url,
  ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'recommended'
    AFTER thumbnail_url,
  ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;

DROP INDEX idx_session ON radio_recommendations;

CREATE INDEX idx_session
  ON radio_recommendations(session_id, sort_order);
```

## 4. RadioSelectedSongDto 전체 코드

```java
package com.ssafy.revibek.radio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RadioSelectedSongDto {

    private String songId;
    private String title;
    private String artist;
    private String youtubeUrl;
    private String thumbnailUrl;
    private String generation;
    private String genre;
    private String mood;
}
```

## 5. RadioSongResponseDto 전체 코드

```java
package com.ssafy.revibek.radio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RadioSongResponseDto {

    private String songId;
    private String title;
    private String artist;
    private String youtubeUrl;
    private String thumbnailUrl;
    private String source;
    private Integer sortOrder;
}
```

## 6. RadioCreateRequestDto 전체 코드

기존 코드에서 사용 중인 `preferredArtist`도 컴파일 호환성을 위해 유지합니다.

```java
package com.ssafy.revibek.radio.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RadioCreateRequestDto {

    private String title;
    private String mood;
    private String situation;
    private String desiredMood;
    private String story;
    private String era;
    private String genre;
    private String videoType;
    private String preferredArtist;
    private String excludedKeywords;
    private Boolean saveAsPlaylist;
    private List<RadioSelectedSongDto> selectedSongs = new ArrayList<>();
}
```

## 7. RadioCreateResponseDto 전체 코드

```java
package com.ssafy.revibek.radio.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RadioCreateResponseDto {

    private String radioSessionId;
    private String playlistId;
    private String title;
    private String story;
    private String djComment;
    private List<RadioSongResponseDto> songs;
}
```

## 8. RadioController 전체 코드

```java
package com.ssafy.revibek.radio.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ssafy.revibek.radio.dto.RadioCreateRequestDto;
import com.ssafy.revibek.radio.dto.RadioCreateResponseDto;
import com.ssafy.revibek.radio.dto.RadioResponseDto;
import com.ssafy.revibek.radio.service.RadioService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/radio")
@RequiredArgsConstructor
public class RadioController {

    private final RadioService radioService;

    @PostMapping
    public ResponseEntity<RadioCreateResponseDto> createRadio(
        Authentication authentication,
        @RequestHeader(value = "X-USER-ID", required = false) String headerUserId,
        @Valid @RequestBody RadioCreateRequestDto request
    ) {
        String userId = resolveUserId(authentication, headerUserId);
        return ResponseEntity.ok(radioService.createRadio(userId, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RadioResponseDto> getSession(
        Authentication authentication,
        @RequestHeader(value = "X-USER-ID", required = false) String headerUserId,
        @PathVariable String id
    ) {
        String userId = resolveUserId(authentication, headerUserId);
        return ResponseEntity.ok(radioService.getSession(id, userId));
    }

    @GetMapping("/me")
    public ResponseEntity<List<RadioResponseDto>> getSessionsByUser(
        Authentication authentication,
        @RequestHeader(value = "X-USER-ID", required = false) String headerUserId
    ) {
        String userId = resolveUserId(authentication, headerUserId);
        return ResponseEntity.ok(radioService.getSessionByUser(userId));
    }

    private String resolveUserId(Authentication authentication, String headerUserId) {
        if (authentication != null
            && authentication.isAuthenticated()
            && StringUtils.hasText(authentication.getName())
            && !"anonymousUser".equals(authentication.getName())) {
            return authentication.getName().trim();
        }

        if (StringUtils.hasText(headerUserId)) {
            return headerUserId.trim();
        }

        throw new ResponseStatusException(
            HttpStatus.UNAUTHORIZED,
            "인증된 사용자 정보가 필요합니다."
        );
    }
}
```

## 17. API 테스트 예시

### 선택곡 포함 라디오 생성

```http
POST /api/radio
Authorization: Bearer {token}
Content-Type: application/json
```

```json
{
  "title": "오늘 나를 위로하는 2세대 라디오",
  "mood": "지친",
  "situation": "퇴근 후",
  "desiredMood": "위로",
  "story": "다시 힘내고 싶어요.",
  "era": "2세대",
  "genre": "댄스",
  "videoType": "뮤직비디오",
  "excludedKeywords": "리믹스,라이브",
  "saveAsPlaylist": true,
  "selectedSongs": [
    {
      "songId": "song-uuid-1",
      "title": "다시 만난 세계",
      "artist": "소녀시대",
      "youtubeUrl": "https://www.youtube.com/watch?v=example",
      "thumbnailUrl": "https://example.com/thumbnail.jpg",
      "generation": "2세대",
      "genre": "댄스",
      "mood": "위로"
    }
  ]
}
```

### 응답 예시

```json
{
  "radioSessionId": "radio-session-uuid",
  "playlistId": "playlist-uuid",
  "title": "오늘 나를 위로하는 2세대 라디오",
  "story": "다시 힘내고 싶어요.",
  "djComment": "오늘은 다시 힘낼 수 있는 곡들을 준비했습니다.",
  "songs": [
    {
      "songId": "song-uuid-1",
      "title": "다시 만난 세계",
      "artist": "소녀시대",
      "youtubeUrl": "https://www.youtube.com/watch?v=example",
      "thumbnailUrl": "https://example.com/thumbnail.jpg",
      "source": "selected",
      "sortOrder": 1
    }
  ]
}
```

### 플레이리스트 저장 없이 생성

```json
{
  "title": "저장하지 않는 라디오",
  "mood": "지친",
  "desiredMood": "위로",
  "saveAsPlaylist": false,
  "selectedSongs": []
}
```

이 경우 응답의 `playlistId`는 `null`입니다.

### 저장 결과 확인

```http
GET /api/playlists
Authorization: Bearer {token}
```

```http
GET /api/radio/me
Authorization: Bearer {token}
```

```http
GET /api/radio/{radioSessionId}
Authorization: Bearer {token}
```

## 18. 구현 후 체크리스트

- `order_num`을 참조하는 기존 코드와 SQL을 모두 `sort_order`로 변경
- `RadioResponseDto.RadioSongDto`의 `orderNum`도 `sortOrder`로 변경하거나 SQL alias 적용
- 선택곡 신규 저장 시 `title`, `artist`, `youtubeUrl` 검증
- `songs` NOT NULL 컬럼에 기본값 적용
- selectedSongs를 추천곡보다 먼저 배치
- 세 가지 중복 기준을 모두 적용
- fallback 단계별 결과 누적
- 최종 곡 수 최대 10곡 확인
- `saveAsPlaylist == null`을 `true`로 처리
- `playlist_songs`만 사용
- 전체 생성 흐름에 `@Transactional` 적용
- 인증 사용자가 존재하면 `X-USER-ID` 무시
- migration 적용 후 MyBatis 매퍼 컴파일 및 통합 테스트 수행
