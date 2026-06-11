## 1. 수정된 파일 목록

실제 파일 수정은 수행되지 않았습니다.

현재 작업공간 권한이 `read-only`이고 승인 정책이 `never`라서 `apply_patch`가 다음 사유로 거부되었습니다.

```text
writing is blocked by read-only sandbox
```

`mvn clean compile`도 `target` 디렉터리를 생성해야 하므로 실행이 차단되었습니다.

## 2. 필요한 수정 파일

권한이 허용되면 다음 파일을 수정해야 합니다.

- `RadioService.java`
  - `PlaylistService` 주입
  - `saveAsPlaylist=true`일 때 플레이리스트 생성
  - `selectedSongs`가 있으면 선택곡 ID 사용
  - 없으면 추천곡 전체 ID 사용
  - 응답의 `playlistId` 설정

- `RadioSelectedSongDto.java`
  - 현재 getter가 없어 `songId`를 추출할 수 없음
  - `@Data`, `@NoArgsConstructor` 추가 필요

- `PlaylistService.java`
  - `createPlaylistWithSongs(...)` 추가
  - 곡 존재 여부 확인
  - 존재하지 않는 곡 ID는 무시
  - 중복 제거 후 순서대로 `order_num` 저장

- `PreferenceController.java`
  - `X-USER-ID`, `userId` 파라미터 제거
  - `authentication.getName()`만 사용
  - 인증정보가 없으면 `401`

- `RadioController.java`
  - 현재 남아 있는 `X-USER-ID`, `userId` fallback 제거 필요

- `SecurityConfig.java`
  - `/api/preferences/**` 인증 추가
  - `anyRequest().permitAll()` 제거
  - Song 조회만 공개
  - Song 변경, Analysis, Qdrant, YouTube API 인증 처리

## 3. Mapper XML

`PlaylistMapper.java`와 `PlaylistMapper.xml`은 수정할 필요가 없습니다.

기존 statement를 그대로 재사용할 수 있습니다.

```java
int insertPlaylist(PlaylistDto playlist);

int insertPlaylistItem(
    @Param("playlistId") String playlistId,
    @Param("songId") String songId,
    @Param("orderNum") int orderNum
);
```

## 4. 확인된 주요 문제

`RadioSelectedSongDto`에는 getter가 없어 현재 구조로는 아래 코드가 컴파일되지 않습니다.

```java
song.getSongId()
```

JWT subject와 프로젝트 전체 사용자 ID 타입은 `String`입니다. UUID 기반이므로 요청에 적힌 `Long.parseLong(authentication.getName())`을 적용하면 오히려 오류가 발생합니다.

현재 `RadioController`도 인증정보가 없을 때 헤더와 쿼리 파라미터를 신뢰하므로 함께 수정해야 합니다.

## 5. 적용할 핵심 로직

```java
if (Boolean.TRUE.equals(request.getSaveAsPlaylist())) {
    List<String> songIds = request.getSelectedSongs() != null
            && !request.getSelectedSongs().isEmpty()
        ? request.getSelectedSongs().stream()
            .filter(song -> song != null && StringUtils.hasText(song.getSongId()))
            .map(RadioSelectedSongDto::getSongId)
            .map(String::trim)
            .distinct()
            .toList()
        : recommendedSongs.stream()
            .map(RecommendedSongResponseDto::getSongId)
            .filter(StringUtils::hasText)
            .map(String::trim)
            .distinct()
            .toList();

    PlaylistDto playlist = playlistService.createPlaylistWithSongs(
        userId,
        firstNonBlank(request.getTitle(), "Radio Playlist"),
        request.getMood(),
        songIds
    );

    playlistId = playlist.getId();
}
```

권한 제한 때문에 전체 코드를 실제 프로젝트 기준으로 수정·검증한 상태로 제공할 수 없습니다. 쓰기 가능한 작업공간으로 다시 실행해야 안전하게 전체 파일 수정과 컴파일 검증을 완료할 수 있습니다.

## 6. 실행 및 테스트 순서

```powershell
mvn clean compile
mvn test
mvn spring-boot:run
```

검증 순서:

1. 로그인 후 JWT 발급
2. JWT 없이 `/api/preferences`, `/api/radio`, `/api/playlists`, `/api/likes` 호출
3. JWT로 `/api/users/me` 호출
4. JWT로 preference 저장 및 조회
5. `saveAsPlaylist=true`, `selectedSongs` 포함 라디오 생성
6. 선택곡만 `playlist_songs`에 저장됐는지 확인
7. `selectedSongs` 없이 라디오 생성
8. 추천곡 전체가 추천 순서대로 저장됐는지 확인
9. `radio_sessions`, `radio_recommendations` 저장 여부 확인
10. `/api/playlists`에서 생성된 플레이리스트 확인
