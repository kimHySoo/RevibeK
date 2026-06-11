현재 Spring Boot 백엔드 프로젝트 전체를 다시 정적 분석해서, 수정 이후 백엔드가 정상적으로 동작할 수 있는지 전체 점검해주세요.

중요:
- 실제 프로젝트 파일을 수정하지 마세요.
- apply_patch를 사용하지 마세요.
- 파일 저장/변경하지 마세요.
- 분석만 수행해주세요.
- 특정 기능 하나만 보지 말고 백엔드 전체를 봐주세요.
- Controller → Service → Mapper → XML → DB 스키마 흐름이 맞는지 확인해주세요.
- 컴파일 오류 가능성, 런타임 오류 가능성, Mapper/DB 불일치, 인증/권한 문제, 외부 API 설정 문제를 모두 점검해주세요.

최근 수정된 것으로 가정하는 내용:
- RadioService에서 saveAsPlaylist 처리 추가
- selectedSongs가 있으면 선택곡만 playlist_songs 저장
- selectedSongs가 없으면 추천곡 전체 playlist_songs 저장
- PlaylistService에 createPlaylistWithSongs 추가
- RadioSelectedSongDto getter 문제 수정
- PreferenceController에서 X-USER-ID/query userId 의존 제거
- RadioController에서 X-USER-ID/query userId fallback 제거
- Authentication.getName() 기반 String userId 사용
- SecurityConfig에서 anyRequest().permitAll() 제거
- /api/preferences, /api/radio, /api/playlists, /api/likes 인증 필수 처리
- RadioMapper.xml의 radio_recommendations 순서 컬럼을 최종 스키마 기준 sort_order에 맞춤

반드시 확인할 항목:

1. 컴파일 가능성
- import 누락
- 존재하지 않는 메서드 호출
- DTO getter/setter/builder 필드 불일치
- 생성자/빌더 필드 불일치
- 타입 불일치
- userId를 Long으로 변환하는 코드가 남아 있는지 확인
- userId는 String UUID 기준인지 확인

2. Controller 점검
- 각 Controller의 요청 경로가 중복되거나 깨지지 않았는지
- Authentication을 사용하는 API가 null 인증을 안전하게 처리하는지
- ResponseEntity 타입이 맞는지
- @RequestBody, @PathVariable, @RequestParam 사용이 Service와 맞는지
- 기존 프론트 API 호출 흐름이 깨질 가능성이 있는지

3. Service 점검
- Controller에서 호출하는 Service 메서드가 실제 존재하는지
- Service 내부에서 Mapper 메서드 호출이 실제 존재하는지
- Transactional이 필요한 저장 흐름에 붙어 있는지
- 예외가 fallback으로 너무 숨겨지는 부분이 있는지
- Radio → Playlist 저장 흐름이 실제로 연결됐는지
- 기존 라디오 세션/추천 저장 기능이 깨지지 않았는지

4. Mapper interface / XML 점검
- Java Mapper interface 메서드와 XML statement id가 일치하는지
- XML namespace가 Mapper interface 경로와 일치하는지
- @Param 이름과 XML #{param} 이름이 일치하는지
- resultType/resultMap이 DTO 필드와 맞는지
- INSERT/SELECT/UPDATE/DELETE SQL 컬럼명이 실제 스키마와 맞는지

5. DB 스키마 점검
- 코드가 사용하는 모든 테이블이 SQL 스키마에 존재하는지
- 코드가 사용하는 모든 컬럼이 SQL 스키마에 존재하는지
- radio_recommendations는 sort_order 기준으로 맞춰졌는지
- playlist_songs는 order_num 기준으로 맞는지
- users.id, songs.id, playlists.id 등 ID 타입이 String UUID 저장에 맞는지
- SQL 파일이 자동 실행되는지, 수동 실행이 필요한지 확인

6. SecurityConfig / 인증 점검
- 로그인/회원가입/OAuth/email mock 등 공개 API가 막히지 않았는지
- /api/preferences/**, /api/radio/**, /api/playlists/**, /api/likes/**, /api/users/me가 인증 필수인지
- Song 조회 API가 프론트 시연에 필요한 경우 공개되어 있는지
- Song 등록/수정/삭제, Analysis, Qdrant, YouTube 수집 API가 적절히 보호되는지
- JWT 필터가 SecurityFilterChain에 정상 등록되어 있는지
- CORS 설정이 프론트 개발 서버와 맞는지

7. 외부 API / fallback 점검
- Google OAuth 설정이 비어 있을 때 서버가 뜨는지
- GMS/DJ 멘트 fallback이 정상인지
- TTS disabled 시 fallback이 정상인지
- FastAPI disabled/mock 흐름이 정상인지
- Qdrant disabled/fallback 흐름이 정상인지
- YouTube disabled/sample 흐름이 정상인지
- 외부 API 실패가 정상 응답처럼 숨겨져 디버깅이 어려운 부분이 있는지

8. 테스트 점검
- 현재 테스트가 context load 외에 부족한지
- 최소한 어떤 API를 Postman/curl로 테스트해야 하는지
- mvn clean compile 전에 확인해야 할 파일이 있는지

답변 형식:

## 1. 최종 판단
아래 중 하나로 명확히 판단해주세요.
- 바로 빌드해도 되는 상태
- 컴파일 전 확인 필요
- 컴파일 오류 가능성 높음
- 런타임 오류 가능성 높음

그리고 이유를 짧게 요약해주세요.

## 2. 가장 위험한 문제 TOP 10
우선순위대로 작성해주세요.

## 3. 컴파일 예상 오류
파일명 / 문제 / 수정 방향 형식으로 작성해주세요.
없으면 “명확한 컴파일 예상 오류 없음”이라고 작성해주세요.

## 4. 런타임 예상 오류
DB, 인증, 외부 API, Mapper 기준으로 작성해주세요.

## 5. Controller → Service → Mapper → DB 흐름 점검 결과
기능별로 작성해주세요.
예:
- Auth
- User
- Preference
- Radio
- Playlist
- Like
- Song
- YouTube
- Analysis/FastAPI
- Qdrant

## 6. DB/Mapper 불일치 결과
테이블/컬럼/Mapper id/@Param 문제를 정리해주세요.

## 7. SecurityConfig 점검 결과
공개 API와 인증 필요 API가 적절한지 정리해주세요.

## 8. 지금 바로 수정해야 할 파일
파일명과 수정 이유만 작성해주세요.
전체 코드는 작성하지 않아도 됩니다.

## 9. 빌드 및 테스트 명령어
아래 명령어를 포함해주세요.

mvn clean compile
mvn test
mvn spring-boot:run

## 10. API 테스트 순서
로그인부터 주요 기능까지 테스트 순서를 작성해주세요.

주의:
- 실제 파일 수정 금지
- DB 직접 변경 금지
- 분석만 수행
- 특정 기능이 아니라 전체 백엔드 기준으로 점검
- 최종적으로 “지금 빌드해도 되는지 / 빌드 전에 고쳐야 할 게 있는지” 명확하게 결론내려 주세요.