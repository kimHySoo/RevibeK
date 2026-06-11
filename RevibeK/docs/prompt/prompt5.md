현재 Spring Boot 백엔드 프로젝트 전체를 분석해서 아래 7가지 수정 작업을 실제 코드에 반영해주세요.

전제:
- DB 실행/스키마 적용 문제와 application.properties 설정 문제는 별도로 해결된 상태라고 가정합니다.
- 기존 기능을 최대한 유지하면서, 발표 시연에서 중요한 “라디오 생성 → 플레이리스트 저장” 흐름과 인증/권한 문제를 우선 수정해주세요.
- 코드 수정 후 어떤 파일을 수정했는지, 어떤 로직을 추가했는지, 테스트 방법까지 정리해주세요.
- 기존 Mapper XML과 Java interface의 namespace/id 매칭을 깨지 않도록 주의해주세요.
- 불필요한 대규모 리팩토링은 하지 말고, 현재 구조를 최대한 유지한 상태에서 안정적으로 수정해주세요.

수정 목표:

1. RadioService에서 saveAsPlaylist 처리 추가
- Radio 생성 요청 DTO에 saveAsPlaylist, selectedSongs, title 필드가 존재한다면 실제 서비스 로직에서 사용되도록 수정해주세요.
- 라디오 생성 후 radio_sessions, radio_recommendations 저장 흐름은 유지해주세요.
- saveAsPlaylist == true 인 경우 playlists 테이블과 playlist_songs 테이블에도 저장되도록 연결해주세요.

2. selectedSongs가 있으면 선택곡만 playlist_songs 저장
- 요청 DTO의 selectedSongs가 null이 아니고 비어 있지 않다면, 추천곡 전체가 아니라 selectedSongs에 포함된 곡 ID만 플레이리스트에 저장해주세요.
- selectedSongs의 타입과 실제 DTO 구조를 확인한 뒤, songId 목록을 안전하게 추출해주세요.
- 존재하지 않는 songId가 들어올 가능성이 있다면 예외 처리 또는 무시 정책을 코드에 명확히 반영해주세요.

3. selectedSongs가 없으면 추천곡 전체 playlist_songs 저장
- selectedSongs가 null이거나 비어 있으면, RadioService에서 최종 추천된 곡 목록 전체를 playlist_songs에 저장해주세요.
- playlist_songs의 정렬 순서가 필요한 경우 추천 순서대로 position/order 값을 넣어주세요.
- PlaylistMapper 또는 PlaylistService가 이미 있다면 가능한 기존 로직을 재사용해주세요.
- 기존 PlaylistService가 없다면 최소 수정으로 PlaylistMapper를 통해 저장 흐름을 구현해주세요.

4. PreferenceController에서 userId 파라미터/X-USER-ID 의존 제거
- PreferenceController가 query parameter userId 또는 X-USER-ID header를 우선 사용하고 있다면 제거하거나 더 이상 우선하지 않도록 수정해주세요.
- 취향 조회/저장/수정/삭제는 반드시 JWT 인증 정보의 사용자 ID 기준으로만 처리되게 해주세요.
- 클라이언트가 임의로 다른 userId를 넘겨도 다른 사용자의 preference에 접근하지 못하게 해주세요.

5. Authentication.getName()으로 로그인 사용자 ID만 사용
- PreferenceController에서 Authentication authentication을 받아 authentication.getName()을 통해 userId를 가져오도록 수정해주세요.
- 현재 JWT subject가 users.id를 저장하는 구조라면 Long.parseLong(authentication.getName()) 방식으로 통일해주세요.
- authentication이 null이거나 인증되지 않은 경우 적절한 401 응답이 발생하도록 처리해주세요.
- 가능하면 userId 추출 중복 코드는 private method로 정리해주세요.

6. SecurityConfig에서 anyRequest().permitAll() 제거
- SecurityConfig의 anyRequest().permitAll() 설정을 제거하거나 더 엄격한 인증 정책으로 변경해주세요.
- 공개 API와 인증 필요 API를 분리해주세요.
- 기존 로그인/회원가입/OAuth callback/email mock 등 인증 없이 접근해야 하는 API는 permitAll로 유지해주세요.
- 나머지 사용자 데이터 관련 API는 authenticated()로 변경해주세요.

7. /api/preferences, /api/radio, /api/playlists, /api/likes 인증 필수 처리
- 아래 API는 반드시 JWT 인증이 필요하도록 SecurityConfig를 수정해주세요.
    - /api/preferences/**
    - /api/radio/**
    - /api/playlists/**
    - /api/likes/**
- /api/users/me도 인증 필수로 유지하거나 수정해주세요.
- Song 등록/수정/삭제, Analysis batch, Qdrant embed, YouTube 수집 API가 공개되어 있다면 최소한 authenticated()로 보호해주세요.
- 단, 단순 조회 API까지 막으면 프론트 시연이 깨질 수 있으므로 기존 프론트 흐름을 확인해서 필요한 범위만 신중하게 조정해주세요.

수정 시 확인할 주요 파일:
- src/main/java/com/ssafy/revibek/radio/service/RadioService.java
- src/main/java/com/ssafy/revibek/radio/dto/RadioCreateRequestDto.java
- src/main/java/com/ssafy/revibek/playlist/service/PlaylistService.java
- src/main/java/com/ssafy/revibek/playlist/mapper/PlaylistMapper.java
- src/main/resources/mapper/playlist/PlaylistMapper.xml
- src/main/java/com/ssafy/revibek/preference/controller/PreferenceController.java
- src/main/java/com/ssafy/revibek/config/SecurityConfig.java
- JWT 필터 및 인증 관련 클래스

구현 후 반드시 점검할 것:
1. mvn clean compile 실행 기준으로 컴파일 오류가 없어야 합니다.
2. mvn test 실행 시 context load 테스트가 통과해야 합니다.
3. 로그인 후 발급받은 JWT로 /api/users/me 호출이 가능해야 합니다.
4. JWT 없이 /api/preferences, /api/radio, /api/playlists, /api/likes 호출 시 401 또는 403이 나와야 합니다.
5. JWT 포함 후 /api/preferences 저장/조회가 로그인 사용자 기준으로 동작해야 합니다.
6. Radio 생성 요청에서 saveAsPlaylist=true인 경우 playlists와 playlist_songs에 데이터가 저장되어야 합니다.
7. selectedSongs가 있으면 selectedSongs만 playlist_songs에 저장되어야 합니다.
8. selectedSongs가 없으면 Radio 추천 결과 전체가 playlist_songs에 저장되어야 합니다.
9. 기존 radio_sessions와 radio_recommendations 저장 기능이 깨지면 안 됩니다.
10. 수정한 파일 목록과 변경 이유를 마지막에 요약해주세요.

원하는 최종 결과:
- 사용자가 로그인한다.
- 취향을 저장한다.
- 라디오를 생성한다.
- 추천곡이 radio_sessions/radio_recommendations에 저장된다.
- saveAsPlaylist=true이면 같은 추천 결과가 playlists/playlist_songs에도 저장된다.
- 이후 내 플레이리스트 API에서 해당 플레이리스트를 확인할 수 있다.
- 다른 사용자의 userId를 query/header로 조작해서 preference를 조회/수정할 수 없다.
- 주요 사용자 API는 JWT 없이는 접근할 수 없다.

추가 요청:
수정 방향만 설명하지 말고, 실제로 수정이 필요한 파일의 전체 코드를 작성해주세요.

반드시 아래 형식으로 답변해주세요.

1. 수정된 파일 목록
2. 각 파일별 전체 코드
    - RadioService.java 전체 코드
    - RadioCreateRequestDto.java 전체 코드
    - PlaylistService.java 전체 코드
    - PlaylistMapper.java 전체 코드
    - PlaylistMapper.xml 전체 코드
    - PreferenceController.java 전체 코드
    - SecurityConfig.java 전체 코드
    - 필요한 경우 JWT 필터/인증 관련 클래스 전체 코드
3. Mapper XML을 수정했다면 XML 전체 코드
4. 새로 추가한 DTO, 메서드, Mapper statement가 있다면 해당 파일 전체 코드
5. 기존 코드를 생략하지 말고, “변경 부분만”, “이하 동일”, “...” 같은 생략 표현을 쓰지 마세요.
6. 전체 코드를 제공할 수 없는 파일은 그 이유를 먼저 설명하고, 최소한 컴파일 가능한 수준의 대체 코드를 제공해주세요.
7. 마지막에는 실행 명령어와 테스트 순서를 작성해주세요.

중요:
- 설명만 하지 말고 실제 붙여넣을 수 있는 전체 코드 형태로 작성해주세요.
- 기존 프로젝트 패키지 구조와 import를 유지해주세요.
- 전체 코드 기준으로 컴파일 오류가 없도록 작성해주세요.
- 기존 메서드/필드명을 임의로 바꾸지 말고, 현재 프로젝트에 맞춰서 수정해주세요.트에 맞춰서 수정해주세요.