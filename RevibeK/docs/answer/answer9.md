## 1. 최종 판단

**컴파일 오류 가능성 높음**

현재 [PlaylistController.java](C:/Users/jaewo/OneDrive/Desktop/RevibeK2/RevibeK/src/main/java/com/ssafy/revibek/playlist/controller/PlaylistController.java:27)에서 `PlaylistService`를 사용하지만 import가 없습니다. 이 상태로는 `mvn clean compile`이 실패합니다.

컴파일 문제 수정 후에도 DB 스키마 적용 상태, Google OAuth 빈 설정, `sort_order` 마이그레이션 문제로 런타임 오류 가능성이 높습니다.

분석 중 파일 수정, DB 변경, Maven 실행은 하지 않았습니다.

## 2. 가장 위험한 문제 TOP 10

1. **PlaylistController 컴파일 오류**
   - `PlaylistService` import 누락.

2. **`radio_recommendations.sort_order` 적용 여부 불명확**
   - Mapper는 `sort_order`를 사용하지만 최초 DDL은 `order_num`으로 생성합니다.
   - SQL 마지막 ALTER까지 수동 실행되어야 정상 동작합니다.

3. **Google OAuth 빈 설정으로 서버 시작 실패 가능성**
   - OAuth 비활성 상태에서도 빈 `client-id`, `client-secret` 등록 속성이 존재합니다.
   - 테스트는 의도적으로 테스트용 OAuth 값을 주입하고 있어 실제 기본 설정 문제를 가립니다.

4. **스키마 SQL 자동 실행 안 됨**
   - `spring.sql.init.*` 설정이 없고 파일명도 기본 `schema.sql`이 아닙니다.
   - DB는 수동 적용이 필요합니다.

5. **Radio에서 생성한 playlistId가 `radio_sessions.playlist_id`에 저장되지 않음**
   - 응답에는 `playlistId`가 있지만 DB 세션과 플레이리스트 연결은 남지 않습니다.

6. **Radio 추천 SQL 오류가 전부 빈 추천으로 숨겨짐**
   - `safeFind...()`에서 모든 예외를 무시하고 `List.of()`를 반환합니다.
   - 컬럼 불일치나 DB 장애도 정상적인 빈 추천처럼 보입니다.

7. **selectedSongs가 DB songs에 없으면 조용히 제외됨**
   - 선택곡이 외부 곡이면 플레이리스트만 생성되고 곡은 하나도 저장되지 않을 수 있습니다.

8. **기존 프론트 인증 흐름 중단 가능성**
   - `X-USER-ID`, query `userId` fallback 제거 후 반드시 `Authorization: Bearer ...`가 필요합니다.
   - `/api/explore`, `/api/ai/**`도 이제 인증 필요입니다.

9. **Qdrant가 비표준 mock ID에서 실패**
   - `UUID.fromString(songId)`를 사용하지만 스키마 mock ID인 `s001-...` 형식은 실제 UUID가 아닙니다.
   - 예외는 fallback으로 숨겨집니다.

10. **테스트가 context load 하나뿐**
   - Controller, 인증, Mapper SQL, Radio→Playlist 트랜잭션 테스트가 없습니다.

## 3. 컴파일 예상 오류

| 파일명 | 문제 | 수정 방향 |
|---|---|---|
| `PlaylistController.java` | `PlaylistService` import 누락으로 `cannot find symbol` 예상 | `com.ssafy.revibek.playlist.service.PlaylistService` import 확인 |

그 외 최근 수정된 DTO getter, builder 필드, `createPlaylistWithSongs()` 호출에서는 명확한 컴파일 불일치를 찾지 못했습니다.

`userId`를 `Long`으로 변환하는 코드도 발견되지 않았습니다. 사용자/곡/플레이리스트 ID는 전반적으로 `String` 기준입니다.

## 4. 런타임 예상 오류

- **DB:** `sort_order` ALTER 미적용 시 라디오 추천 저장/조회 SQL 실패.
- **DB:** 스키마 SQL은 재실행 시 `CREATE TABLE`, `ALTER TABLE`, `DROP INDEX`에서 실패할 수 있습니다.
- **DB:** 선택곡 ID가 `songs.id`에 없으면 플레이리스트에서 조용히 제외됩니다.
- **인증:** 직접 `authentication.getName()`을 호출하는 Playlist/User/Like/UserSong Controller는 인증 객체가 없으면 NPE 가능성이 있습니다.
- **인증:** `ResponseStatusException(401)`이 `GlobalExceptionHandler`의 `RuntimeException` 처리에 잡혀 500으로 변환될 가능성이 있습니다.
- **외부 API:** FastAPI 실패가 `MOCK` 결과가 되어 실제 분석값처럼 songs에 저장됩니다.
- **외부 API:** GMS, YouTube, Qdrant, TTS 실패가 fallback 정상 응답으로 보이므로 장애 탐지가 어렵습니다.
- **설정:** 빈 Google OAuth 등록 정보로 애플리케이션 시작 실패 가능성이 있습니다.

## 5. Controller → Service → Mapper → DB 흐름 점검 결과

- **Auth:** Controller→AuthService→UserMapper→users 연결 정상. Refresh token은 메모리 저장이라 재시작 시 전부 무효화됩니다.
- **User:** String userId 흐름 정상. Controller의 null 인증 방어는 없습니다.
- **Preference:** Authentication 기반 String userId 전환 정상. Mapper/XML/`user_preferences` 일치.
- **Radio:** Radio→추천 SongDao→RadioMapper→DB→PlaylistService 연결됨. 다만 playlistId DB 연결 누락과 예외 은폐 문제가 있습니다.
- **Playlist:** Service/Mapper/XML/`playlist_songs.order_num` 일치. Controller import 누락으로 현재 컴파일 불가.
- **Like:** Service/Mapper/XML/`song_likes` 흐름 일치. 저장과 songs.like_count 갱신은 트랜잭션 처리됨.
- **Song:** 조회/등록/수정/삭제 Mapper 흐름 일치. GET은 공개, 쓰기는 인증 필요.
- **YouTube:** Mapper/테이블 일치. 여러 DB 저장을 하나의 트랜잭션으로 묶지 않아 부분 저장 가능.
- **Analysis/FastAPI:** disabled/mock 흐름 동작. MOCK 분석 결과를 실제 songs 데이터에 저장할 위험이 있습니다.
- **Qdrant:** disabled fallback 동작. 비표준 ID는 UUID 변환 실패 후 fallback됩니다.

## 6. DB/Mapper 불일치 결과

- Mapper namespace와 Java Mapper 경로는 일치합니다.
- 확인한 Mapper interface 메서드와 XML statement id는 일치합니다.
- 주요 `@Param`과 XML 파라미터명도 일치합니다.
- `playlist_songs`는 코드와 스키마 모두 `order_num` 기준입니다.
- `radio_recommendations` Mapper는 최종 `sort_order` 기준입니다.
- 스키마 파일은 초기 `order_num` 생성 후 마지막에 `sort_order`로 변경하므로 **ALTER 실행 여부가 필수**입니다.
- `users.id`, `songs.id`, `playlists.id`는 모두 `CHAR(36)`이며 String UUID 저장에 적합합니다.
- 스키마 파일은 자동 실행되지 않습니다.

## 7. SecurityConfig 점검 결과

- 공개: `/api/auth/**`, OAuth 경로, Swagger, `GET /api/songs/**`
- 인증 필수: Preference, Radio, Playlist, Like, `/api/users/me`, UserSong
- Song 등록/수정/삭제, Analysis, Qdrant, YouTube는 인증 필수
- JWT 필터는 `UsernamePasswordAuthenticationFilter` 앞에 정상 등록
- CORS는 `localhost/127.0.0.1`의 3000, 5173 포트를 허용
- `/api/explore`, `/api/ai/**`, TTS도 `anyRequest().authenticated()`로 인증 필요
- 역할 구분은 없어서 인증된 일반 사용자가 Song 수정, Analysis, Qdrant, YouTube 관리 API를 호출할 수 있습니다.

## 8. 지금 바로 수정해야 할 파일

- `PlaylistController.java`: `PlaylistService` import 누락
- `application.properties`: Google OAuth 빈 설정의 서버 시작 영향 확인
- `kpop_radio_schema.sql`: 최종 스키마 기준 DDL 정리 및 마이그레이션 분리
- `RadioService.java`: DB 예외 은폐, selectedSongs 누락 처리, playlistId 세션 연결
- `RadioMapper.java` / `RadioMapper.xml`: radio session의 playlistId 갱신 기능 필요
- `GlobalExceptionHandler.java`: 인증/권한 예외가 500으로 변환되지 않도록 처리
- `QdrantService.java`: 비표준 ID 처리
- `AnalysisServiceImpl.java`: MOCK 결과 저장 정책 재검토

## 9. 빌드 및 테스트 명령어

아래 명령은 파일 생성 금지 요청 때문에 실행하지 않았습니다.

```powershell
mvn clean compile
mvn test
mvn spring-boot:run
```

실행 전 반드시 `PlaylistController` import와 DB의 `sort_order` 적용 상태를 확인해야 합니다.

## 10. API 테스트 순서

1. 서버 시작 및 OAuth 빈 설정 확인
2. `POST /api/auth/email/send`
3. `POST /api/auth/email/verify`
4. `POST /api/auth/signup`
5. `POST /api/auth/login` 후 access token 확보
6. 토큰 없이 보호 API가 401인지 확인
7. `GET /api/users/me`
8. Preference 저장/조회/수정/삭제
9. 공개 Song 조회 및 인증 Song 쓰기 권한 확인
10. Radio 생성: `saveAsPlaylist=false`
11. Radio 생성: `saveAsPlaylist=true`, selectedSongs 없음
12. Radio 생성: selectedSongs 있음
13. Radio 세션 및 생성된 Playlist 곡 순서 확인
14. Like 추가/조회/삭제
15. UserSong 저장/평점/재생/삭제
16. FastAPI, Qdrant, YouTube, GMS, TTS 각각 disabled와 enabled 실패 흐름 확인

**결론: 지금 바로 빌드하면 안 됩니다. 최소한 `PlaylistController` 컴파일 오류를 수정하고, DB에 `sort_order` 최종 스키마가 적용됐는지 확인한 뒤 빌드해야 합니다.**