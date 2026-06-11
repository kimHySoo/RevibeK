현재 Spring Boot 백엔드 프로젝트 전체 점검 결과를 바탕으로, 빌드/런타임 위험이 큰 문제를 우선 수정하려고 합니다.

중요:

* 실제 프로젝트 파일을 직접 수정하지 마세요.
* apply_patch를 사용하지 마세요.
* 파일을 저장하거나 변경하지 마세요.
* 수정 방향만 설명하지 말고, 수정이 필요한 파일의 “수정 후 전체 코드”를 답변으로 작성해주세요.
* “변경 부분만”, “이하 동일”, “생략”, “...” 같은 표현을 쓰지 마세요.
* 현재 프로젝트의 실제 파일을 읽고, 기존 패키지명/import/클래스명/메서드명/DTO 필드명/Mapper 메서드명/XML namespace를 유지해주세요.
* 대규모 리팩토링 없이 최소 수정으로 작성해주세요.
* 컴파일 오류가 나지 않도록 필요한 import까지 포함한 전체 코드를 작성해주세요.
* userId는 String UUID 기반입니다. Long.parseLong(authentication.getName()) 사용 금지입니다.

현재 점검 결과 요약:

1. PlaylistController.java

* PlaylistService를 사용하지만 import가 누락되어 있습니다.
* 현재 상태로는 mvn clean compile이 실패할 가능성이 높습니다.
* 가장 먼저 수정해야 합니다.

2. radio_recommendations.sort_order

* 최종 스키마는 radio_recommendations.sort_order 기준입니다.
* Mapper도 sort_order 기준으로 맞춘 상태인지 다시 확인해야 합니다.
* RadioMapper.xml / RadioMapper.java / RadioResponseDto / RadioService 사이에 order_num, sort_order, orderNum 매핑 불일치가 없는지 확인해주세요.
* 필요한 경우 수정 후 전체 코드를 작성해주세요.

3. GlobalExceptionHandler.java

* 인증/권한 예외가 401/403이 아니라 500으로 변환될 가능성이 있습니다.
* ResponseStatusException, AuthenticationException, AccessDeniedException이 적절한 HTTP 상태로 내려가도록 수정이 필요한지 확인해주세요.
* 수정이 필요하면 전체 코드를 작성해주세요.

4. application.properties / OAuth 설정

* Google OAuth client-id, client-secret이 비어 있을 때 서버 시작 실패 가능성이 있습니다.
* SecurityConfig에서 OAuth 설정이 비어 있으면 oauth2Login을 활성화하지 않도록 되어 있는지 확인해주세요.
* application.properties 수정이 필요하면 수정 후 전체 내용을 작성해주세요.
* 단, 민감한 secret 값은 절대 작성하지 말고 placeholder로 처리해주세요.

5. RadioService.java

* saveAsPlaylist=true일 때 playlistId는 응답에 들어가지만 radio_sessions.playlist_id에는 저장되지 않는다고 점검됐습니다.
* 스키마에 radio_sessions.playlist_id가 있다면, 생성된 playlistId를 radio_sessions에도 연결하는 것이 필요한지 판단해주세요.
* 발표 시연에 필요한 수준이면 수정해주세요.
* 수정이 필요하면 RadioService.java, RadioMapper.java, RadioMapper.xml의 수정 후 전체 코드를 작성해주세요.
* 기존 radio_sessions, radio_recommendations 저장 기능은 절대 깨지면 안 됩니다.

6. QdrantService.java

* UUID.fromString(songId)를 사용하고, mock song id가 s001 같은 비표준 형식이면 실패할 수 있습니다.
* 발표 시연 전에 반드시 고쳐야 하는 문제인지 판단해주세요.
* 지금 단계에서 최소 수정이 필요하다면 수정 후 전체 코드를 작성해주세요.
* Qdrant가 disabled/fallback 구조라면 “후순위”로 분류해도 됩니다.

7. AnalysisServiceImpl.java

* FastAPI disabled/mock 결과가 실제 songs 데이터에 저장될 위험이 있다고 점검됐습니다.
* 발표 시연 전에 반드시 고쳐야 하는 문제인지 판단해주세요.
* 지금 단계에서 수정이 필요하면 수정 후 전체 코드를 작성해주세요.
* 후순위면 수정 불필요로 분류해주세요.

수정 우선순위:

1. 반드시 컴파일을 막는 문제
2. 서버 실행을 막는 문제
3. 핵심 시연 기능을 깨는 문제
4. 인증/권한 응답이 잘못 나가는 문제
5. 외부 API fallback 관련 문제
6. 나중에 개선해도 되는 문제

반드시 확인할 파일:

* src/main/java/com/ssafy/revibek/playlist/controller/PlaylistController.java
* src/main/java/com/ssafy/revibek/playlist/service/PlaylistService.java
* src/main/java/com/ssafy/revibek/radio/mapper/RadioMapper.java
* src/main/resources/mapper/radio/RadioMapper.xml
* src/main/java/com/ssafy/revibek/radio/dto/RadioResponseDto.java
* src/main/java/com/ssafy/revibek/radio/service/RadioService.java
* src/main/java/com/ssafy/revibek/common/exception/GlobalExceptionHandler.java
* src/main/java/com/ssafy/revibek/config/SecurityConfig.java
* src/main/resources/application.properties
* src/main/java/com/ssafy/revibek/qdrant/QdrantService.java
* src/main/java/com/ssafy/revibek/analysis/service/AnalysisServiceImpl.java
* src/main/resources/sql/kpop_radio_schema.sql

답변 형식:

## 1. 최종 판단

아래 형식으로 답해주세요.

* 지금 바로 고쳐야 하는 파일:
* 빌드 전에 반드시 수정할 파일:
* 런타임 테스트 전에 확인할 파일:
* 후순위 파일:

## 2. 수정 필요 파일 목록

수정이 필요한 파일만 목록으로 작성해주세요.

## 3. 수정 불필요 파일 목록

수정하지 않아도 되는 파일과 이유를 작성해주세요.

## 4. 파일별 수정 후 전체 코드

수정이 필요한 파일은 반드시 전체 코드로 작성해주세요.

예시:

### src/main/java/com/ssafy/revibek/playlist/controller/PlaylistController.java

```java
전체 코드
```

### src/main/resources/mapper/radio/RadioMapper.xml

```xml
전체 XML 코드
```

application.properties를 수정해야 한다면:

### src/main/resources/application.properties

```properties
전체 properties 내용
```

## 5. DB 수정 필요 여부

아래 중 하나로 명확히 답해주세요.

* DB 수정 SQL 필요
* DB 수정 SQL 불필요
* 현재 로컬 DB에 sort_order ALTER 적용 여부만 확인 필요

DB SQL이 필요하다면 DROP TABLE 없이 ALTER TABLE 중심으로 작성해주세요.

## 6. 적용 순서

내가 수동으로 복사/붙여넣기 할 순서를 작성해주세요.

예시:

1. PlaylistController.java 수정
2. GlobalExceptionHandler.java 수정
3. RadioMapper.xml 확인/수정
4. 필요한 경우 RadioService.java 수정
5. mvn clean compile 실행
6. mvn test 실행
7. API 테스트

## 7. 실행 명령어

아래 명령어를 포함해주세요.

```powershell
mvn clean compile
mvn test
mvn spring-boot:run
```

## 8. 확인용 SQL

radio_recommendations 컬럼 확인 SQL과 저장 결과 확인 SQL을 작성해주세요.

예:

```sql
SHOW COLUMNS FROM radio_recommendations;

SELECT id, session_id, song_id, sort_order, reason
FROM radio_recommendations
ORDER BY session_id, sort_order;
```

## 9. API 테스트 순서

로그인/JWT 발급부터 핵심 기능 시연까지 테스트 순서를 작성해주세요.

주의:

* 실제 파일 수정 금지
* 전체 코드로 답변
* 생략 금지
* Long.parseLong(authentication.getName()) 사용 금지
* userId는 String UUID
* 컴파일을 막는 문제부터 우선 수정
* 불필요하게 Qdrant/FastAPI 구조를 대규모 리팩토링하지 말 것
