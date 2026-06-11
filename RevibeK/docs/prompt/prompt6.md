현재 Spring Boot 백엔드 프로젝트 전체를 읽고 분석한 뒤, 아래 수정 요구사항을 반영하려면 각 파일을 어떻게 수정해야 하는지 “수정 후 전체 코드”로 작성해주세요.

가장 중요:

* 실제 프로젝트 파일을 직접 수정하지 마세요.
* apply_patch를 사용하지 마세요.
* 파일을 저장하거나 변경하지 마세요.
* 답변으로만 수정 후 전체 코드를 출력해주세요.
* 설명만 하지 말고, 내가 그대로 복사해서 붙여넣을 수 있는 전체 코드를 주세요.
* “변경 부분만”, “이하 동일”, “생략”, “...” 같은 표현을 절대 쓰지 마세요.
* 현재 프로젝트의 실제 파일 내용을 먼저 읽고, 기존 패키지명/import/클래스명/메서드명/DTO 필드명/Mapper 메서드명/XML namespace를 유지한 상태로 작성해주세요.
* 기존 기능을 삭제하지 말고 최소 수정으로 작성해주세요.
* Mapper XML namespace/id와 Java Mapper interface 메서드명이 깨지지 않게 해주세요.
* 컴파일 오류가 나지 않도록 필요한 import까지 포함한 전체 코드를 작성해주세요.

전제:

* DB 실행/스키마 적용 문제와 application.properties 설정 문제는 별도로 해결된 상태라고 가정합니다.
* userId는 Long이 아니라 String UUID 기반입니다.
* 따라서 Authentication.getName()을 Long.parseLong() 하지 마세요.
* 반드시 String userId = authentication.getName(); 형태로 처리해주세요.
* 기존 radio_sessions, radio_recommendations 저장 기능은 반드시 유지해야 합니다.
* PlaylistMapper.java와 PlaylistMapper.xml은 기존 statement를 최대한 재사용해주세요.
* 기존 JWT 필터 구조는 불필요하게 바꾸지 마세요.

수정 요구사항:

1. RadioService.java

* PlaylistService를 주입한 수정 후 전체 코드를 작성해주세요.
* 라디오 생성 후 기존 radio_sessions, radio_recommendations 저장 로직은 유지해주세요.
* request.getSaveAsPlaylist()가 true이면 플레이리스트를 생성하도록 작성해주세요.
* request.getSelectedSongs()가 null이 아니고 비어 있지 않으면 selectedSongs에 포함된 songId만 playlist_songs에 저장하도록 작성해주세요.
* selectedSongs가 null이거나 비어 있으면 최종 추천곡 전체 songId를 playlist_songs에 저장하도록 작성해주세요.
* 플레이리스트 생성 후 응답 DTO에 playlistId를 설정할 수 있으면 설정해주세요.
* 추천곡 저장, DJ 멘트 생성, GMS/TTS/fallback 로직 등 기존 기능을 삭제하지 마세요.

2. RadioController.java

* X-USER-ID header 또는 query userId fallback이 남아 있다면 제거한 수정 후 전체 코드를 작성해주세요.
* 라디오 생성/조회 등 사용자 기준 API는 Authentication에서 가져온 userId만 사용하도록 작성해주세요.
* userId는 String UUID이므로 Long 변환하지 마세요.
* 인증 정보가 없으면 401이 발생하도록 작성해주세요.
* 기존 API 경로와 요청/응답 구조는 최대한 유지해주세요.

3. RadioCreateRequestDto.java

* saveAsPlaylist, selectedSongs, title 필드가 정상적으로 서비스에서 사용될 수 있도록 수정 후 전체 코드를 작성해주세요.
* 기존 필드는 유지해주세요.
* Lombok을 쓰고 있다면 프로젝트 기존 스타일에 맞춰 @Data, @Getter, @Setter 등을 사용해주세요.

4. RadioSelectedSongDto.java

* 현재 getter가 없어 songId 추출이 어렵다면 Lombok @Data, @NoArgsConstructor를 추가한 수정 후 전체 코드를 작성해주세요.
* 기존 필드는 유지해주세요.
* selectedSongs에서 songId를 안전하게 읽을 수 있게 해주세요.

5. RadioCreateResponseDto.java 또는 라디오 생성 응답 DTO

* playlistId를 응답에 포함할 필요가 있다면 playlistId 필드를 추가한 수정 후 전체 코드를 작성해주세요.
* 기존 응답 필드는 유지해주세요.
* 기존 프론트 응답 구조가 깨지지 않도록 최소 수정해주세요.

6. PlaylistService.java

* createPlaylistWithSongs(String userId, String title, String mood, List<String> songIds) 메서드가 포함된 수정 후 전체 코드를 작성해주세요.
* 이미 비슷한 메서드가 있으면 기존 메서드를 유지하면서 확장해주세요.
* songIds는 null 제거, blank 제거, trim, 중복 제거를 수행해주세요.
* 존재하지 않는 songId는 저장하지 말고 무시하도록 작성해주세요.
* 저장 순서대로 playlist_songs.order_num에 1부터 저장해주세요.
* PlaylistMapper의 기존 insertPlaylist, insertPlaylistItem 메서드를 재사용해주세요.
* 곡 존재 여부를 확인할 수 있는 Mapper/Service가 이미 있으면 재사용해주세요.
* 곡 존재 여부 확인 메서드가 없다면 현재 프로젝트 구조에 맞게 최소 수정한 전체 코드를 함께 작성해주세요.

7. PlaylistMapper.java

* 기존 insertPlaylist, insertPlaylistItem 메서드가 그대로 사용 가능하면 수정 불필요라고 명시해주세요.
* 수정이 필요하다면 수정 후 전체 코드를 작성해주세요.
* PlaylistMapper.xml의 statement id와 반드시 일치하게 작성해주세요.

8. PlaylistMapper.xml

* 기존 XML statement가 그대로 사용 가능하면 수정 불필요라고 명시해주세요.
* 수정이 필요하다면 수정 후 전체 XML 코드를 작성해주세요.
* namespace와 Java Mapper interface 경로가 일치해야 합니다.
* playlist_songs의 order_num 컬럼명을 실제 스키마와 맞춰주세요.

9. PreferenceController.java

* X-USER-ID header와 query parameter userId에 의존하는 로직을 제거한 수정 후 전체 코드를 작성해주세요.
* preference 조회/저장/수정/삭제는 반드시 Authentication authentication에서 가져온 로그인 사용자 ID만 사용하도록 작성해주세요.
* 반드시 String userId = authentication.getName(); 형태로 처리해주세요.
* Long.parseLong(authentication.getName())은 절대 사용하지 마세요.
* authentication이 null이거나 인증되지 않은 경우 ResponseStatusException(HttpStatus.UNAUTHORIZED) 또는 401 응답이 발생하도록 작성해주세요.
* userId 추출 로직은 private method로 정리해주세요.
* 기존 API 경로와 요청/응답 DTO 구조는 최대한 유지해주세요.

10. SecurityConfig.java

* anyRequest().permitAll()을 제거한 수정 후 전체 코드를 작성해주세요.
* 공개 API와 인증 필요 API를 분리해주세요.
* 다음 API는 반드시 authenticated()로 보호해주세요.

    * /api/preferences/**
    * /api/radio/**
    * /api/playlists/**
    * /api/likes/**
    * /api/users/me
* Song 등록/수정/삭제 API는 authenticated()로 보호해주세요.
* Analysis batch, Qdrant embed, YouTube 수집 API가 공개되어 있다면 authenticated()로 보호해주세요.
* 단순 Song 조회 API는 프론트 시연이 깨지지 않도록 공개 유지가 필요한지 현재 Controller를 보고 판단해주세요.
* 로그인, 회원가입, OAuth callback, 이메일 인증 mock 등 인증 없이 접근해야 하는 API는 permitAll로 유지해주세요.
* 기존 JWT 필터 연결 구조를 유지해주세요.

11. JWT 필터/인증 관련 클래스

* SecurityConfig 수정 과정에서 JWT 필터나 인증 관련 클래스 수정이 필요하면 해당 파일의 수정 후 전체 코드도 작성해주세요.
* 수정이 필요 없다면 “수정 불필요”라고 명시해주세요.

답변 형식:

## 1. 수정 필요 파일 목록

수정이 필요한 파일과 수정이 불필요한 파일을 구분해서 작성해주세요.

## 2. 파일별 수정 후 전체 코드

각 파일마다 아래 형식으로 작성해주세요.

### src/main/java/.../RadioService.java

```java
전체 코드
```

### src/main/resources/mapper/playlist/PlaylistMapper.xml

```xml
전체 XML 코드
```

## 3. 수정 불필요 파일

수정하지 않아도 되는 파일이 있다면 그 이유를 작성해주세요.

## 4. 수동 적용 순서

내가 직접 복사/붙여넣기 할 순서를 작성해주세요.

예시:

1. RadioSelectedSongDto.java 수정
2. RadioCreateResponseDto.java 수정
3. PlaylistService.java 수정
4. RadioService.java 수정
5. RadioController.java 수정
6. PreferenceController.java 수정
7. SecurityConfig.java 수정
8. mvn clean compile 실행

## 5. 실행 명령어

아래 명령어를 포함해주세요.

```powershell
mvn clean compile
mvn test
mvn spring-boot:run
```

## 6. 테스트 순서

다음 테스트 순서를 작성해주세요.

1. 로그인 후 JWT 발급
2. JWT 없이 /api/preferences, /api/radio, /api/playlists, /api/likes 호출 시 401 또는 403 확인
3. JWT로 /api/users/me 호출 확인
4. JWT로 preference 저장 및 조회
5. saveAsPlaylist=true, selectedSongs 포함 라디오 생성
6. selectedSongs만 playlist_songs에 저장됐는지 확인
7. selectedSongs 없이 라디오 생성
8. 추천곡 전체가 playlist_songs에 저장됐는지 확인
9. radio_sessions, radio_recommendations 저장 여부 확인
10. /api/playlists에서 생성된 플레이리스트 확인

마지막 주의:

* 실제 파일을 수정하지 마세요.
* 전체 코드를 답변으로만 주세요.
* 생략하지 마세요.
* Long.parseLong(authentication.getName()) 사용 금지입니다.
* userId는 String UUID입니다.
* 기존 Mapper XML namespace/id를 깨지 마세요.
* 기존 기능을 삭제하지 마세요.
* 컴파일 가능한 전체 코드로 작성해주세요.

