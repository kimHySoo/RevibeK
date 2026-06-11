## 1. 최종 판단

* 지금 바로 고쳐야 하는 파일:
  * `PlaylistController.java`: `PlaylistService` import 누락으로 컴파일 실패
  * `application.properties`: 빈 OAuth registration 값으로 인한 서버 시작 실패 방지
* 빌드 전에 반드시 수정할 파일:
  * `PlaylistController.java`
* 런타임 테스트 전에 확인할 파일:
  * `GlobalExceptionHandler.java`
  * `AnalysisServiceImpl.java`
  * `kpop_radio_schema.sql`의 최종 ALTER 적용 여부
* 후순위 파일:
  * `RadioService.java`: `playlist_id` 미연결은 데이터 일관성 문제지만 핵심 생성·저장 기능은 동작
  * `QdrantService.java`: 비표준 UUID 문제가 있지만 `qdrant.enabled=false` 기본값이므로 후순위

실제 프로젝트 파일은 수정하지 않았으며, 빌드 명령도 파일 생성을 피하기 위해 실행하지 않았습니다.

## 2. 수정 필요 파일 목록

* `src/main/java/com/ssafy/revibek/playlist/controller/PlaylistController.java`
* `src/main/java/com/ssafy/revibek/common/exception/GlobalExceptionHandler.java`
* `src/main/resources/application.properties`
* `src/main/java/com/ssafy/revibek/analysis/service/AnalysisServiceImpl.java`

## 3. 수정 불필요 파일 목록

* `PlaylistService.java`
  * `String` 기반 userId를 정상 사용합니다.
* `RadioMapper.java`
  * `orderNum`은 Java 파라미터 이름이며 DB 컬럼명과 충돌하지 않습니다.
* `RadioMapper.xml`
  * 저장·정렬은 `sort_order`, DTO 매핑은 `AS order_num`으로 일관됩니다.
* `RadioResponseDto.java`
  * `orderNum` 필드는 MyBatis camel-case 매핑과 일치합니다.
* `RadioService.java`
  * 현재 `radio_sessions.playlist_id` 미연결은 핵심 기능을 깨지는 않습니다.
  * DB ALTER 적용을 확인한 뒤 후속 수정하는 것이 안전합니다.
* `SecurityConfig.java`
  * OAuth 활성화 여부, client-id, client-secret을 모두 검사합니다.
* `QdrantService.java`
  * 기본적으로 Qdrant가 비활성화되어 있어 발표 시연 차단 문제는 아닙니다.
* `kpop_radio_schema.sql`
  * 최종 ALTER까지 실행하면 `sort_order`와 `radio_sessions.playlist_id`가 생성됩니다.

## 4. 파일별 수정 후 전체 코드

### src/main/java/com/ssafy/revibek/playlist/controller/PlaylistController.java

```java
package com.ssafy.revibek.playlist.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.revibek.playlist.dto.PlaylistDto;
import com.ssafy.revibek.playlist.dto.PlaylistItemDto;
import com.ssafy.revibek.playlist.service.PlaylistService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/playlists")
@RequiredArgsConstructor
public class PlaylistController {

    private final PlaylistService playlistService;

    @PostMapping
    public ResponseEntity<PlaylistDto> createPlaylist(Authentication authentication,
                                                      @Valid @RequestBody PlaylistDto request) {
        return ResponseEntity.ok(playlistService.createPlaylist(authentication.getName(), request));
    }

    @GetMapping
    public ResponseEntity<List<PlaylistDto>> getMyPlaylists(Authentication authentication) {
        return ResponseEntity.ok(playlistService.getMyPlaylists(authentication.getName()));
    }

    @GetMapping("/{playlistId}")
    public ResponseEntity<PlaylistDto> getPlaylist(Authentication authentication,
                                                   @PathVariable String playlistId) {
        return ResponseEntity.ok(playlistService.getPlaylist(authentication.getName(), playlistId));
    }

    @PostMapping("/{playlistId}/items")
    public ResponseEntity<PlaylistItemDto> addItem(Authentication authentication,
                                                   @PathVariable String playlistId,
                                                   @Valid @RequestBody PlaylistItemDto request) {
        return ResponseEntity.ok(playlistService.addItem(authentication.getName(), playlistId, request));
    }

    @DeleteMapping("/{playlistId}/items/{itemId}")
    public ResponseEntity<Map<String, String>> deleteItem(Authentication authentication,
                                                          @PathVariable String playlistId,
                                                          @PathVariable String itemId) {
        playlistService.deleteItem(authentication.getName(), playlistId, itemId);
        return ResponseEntity.ok(Map.of("message", "플레이리스트 항목 삭제 완료"));
    }

    @DeleteMapping("/{playlistId}")
    public ResponseEntity<Map<String, String>> deletePlaylist(Authentication authentication,
                                                              @PathVariable String playlistId) {
        playlistService.deletePlaylist(authentication.getName(), playlistId);
        return ResponseEntity.ok(Map.of("message", "플레이리스트 삭제 완료"));
    }
}
```

### src/main/java/com/ssafy/revibek/common/exception/GlobalExceptionHandler.java

```java
package com.ssafy.revibek.common.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import com.ssafy.revibek.common.dto.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );

        ErrorResponse response = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "요청값 검증에 실패했습니다.",
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(
            ResponseStatusException exception,
            HttpServletRequest request
    ) {
        int statusCode = exception.getStatusCode().value();
        HttpStatus status = HttpStatus.resolve(statusCode);
        String error = status != null
                ? status.getReasonPhrase()
                : exception.getStatusCode().toString();
        String message = exception.getReason() != null
                ? exception.getReason()
                : error;

        ErrorResponse response = ErrorResponse.of(
                statusCode,
                error,
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(exception.getStatusCode()).body(response);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(
            AuthenticationException exception,
            HttpServletRequest request
    ) {
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException exception,
            HttpServletRequest request
    ) {
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception exception,
            HttpServletRequest request
    ) {
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "서버 내부 오류가 발생했습니다.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
```

### src/main/resources/application.properties

```properties
spring.application.name=RevibeK
spring.profiles.include=${SPRING_PROFILES_INCLUDE:secret}

# dataSource
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/kpop_radio?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8}
spring.datasource.username=${DB_USERNAME:SSAFY}
spring.datasource.password=${DB_PASSWORD:SSAFY}

# mybatis
mybatis.mapper-locations=classpath*:mapper/**/*.xml
mybatis.type-aliases-package=com.ssafy.revibek
mybatis.configuration.map-underscore-to-camel-case=true

# security
spring.security.user.name=${SPRING_SECURITY_USER_NAME:admin}
spring.security.user.password=${SPRING_SECURITY_USER_PASSWORD:1234}
app.cors.allowed-origins=${CORS_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:5173,http://127.0.0.1:3000,http://127.0.0.1:5173}

# jwt
jwt.secret=${JWT_SECRET:revibek-dev-secret-key-change-me-32bytes}
jwt.access-token-expiration-ms=${JWT_ACCESS_TOKEN_EXPIRATION_MS:3600000}
jwt.refresh-token-expiration-ms=${JWT_REFRESH_TOKEN_EXPIRATION_MS:1209600000}

# oauth2 google
app.oauth.google.enabled=${GOOGLE_OAUTH_ENABLED:false}
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID:disabled-google-client-id}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET:disabled-google-client-secret}
spring.security.oauth2.client.registration.google.scope=openid,profile,email
spring.security.oauth2.client.registration.google.redirect-uri=${GOOGLE_REDIRECT_URI:http://localhost:8080/auth/google/callback}

# smtp mail
app.email.verification.mode=${EMAIL_VERIFICATION_MODE:mock}
app.email.verification.mock-code=${EMAIL_VERIFICATION_MOCK_CODE:123456}
spring.mail.host=${SMTP_HOST:}
spring.mail.port=${SMTP_PORT:587}
spring.mail.username=${SMTP_USERNAME:}
spring.mail.password=${SMTP_PASSWORD:}
spring.mail.properties.mail.smtp.auth=${SMTP_AUTH:true}
spring.mail.properties.mail.smtp.starttls.enable=${SMTP_STARTTLS:true}

# gms claude
gms.enabled=${GMS_ENABLED:false}
gms.api.base-url=${GMS_API_BASE_URL:}
gms.api.key=${GMS_API_KEY:${GMS_KEY:}}
gms.api.anthropic-version=${GMS_ANTHROPIC_VERSION:2023-06-01}
gms.api.model=${GMS_MODEL:claude-sonnet-4-6}
gms.api.max-tokens=${GMS_MAX_TOKENS:300}
gms.api.budget-credit=${GMS_BUDGET_CREDIT:30}
gms.api.input-cost-per-1k=${GMS_INPUT_COST_PER_1K:0.03}
gms.api.output-cost-per-1k=${GMS_OUTPUT_COST_PER_1K:0.15}

# google cloud text-to-speech
tts.enabled=${TTS_ENABLED:false}
gcp.tts.base-url=${GCP_TTS_BASE_URL:https://texttospeech.googleapis.com/v1}
gcp.tts.api-key=${GCP_TTS_API_KEY:}
gcp.tts.default-language-code=${TTS_LANGUAGE_CODE:ko-KR}
gcp.tts.default-voice-name=${TTS_VOICE:ko-KR-Chirp3-HD-Vindemiatrix}
gcp.tts.default-audio-encoding=${TTS_AUDIO_ENCODING:MP3}
gcp.tts.default-speaking-rate=${TTS_SPEAKING_RATE:1.0}
gcp.tts.default-pitch=${TTS_PITCH:}

# youtube
youtube.enabled=${YOUTUBE_ENABLED:false}
youtube.api.key=${YOUTUBE_API_KEY:}
youtube.fallback.mode=${YOUTUBE_FALLBACK_MODE:db}

# vector / qdrant
vector.enabled=${VECTOR_ENABLED:true}
vector.provider=${VECTOR_PROVIDER:qdrant}
qdrant.enabled=${QDRANT_ENABLED:false}
qdrant.host=${QDRANT_HOST:localhost}
qdrant.port=${QDRANT_PORT:6334}
qdrant.collection=${QDRANT_COLLECTION:revibek_songs}

# fastapi
fastapi.enabled=${FASTAPI_ENABLED:false}
fastapi.host=${FASTAPI_URL:http://localhost:8000}
fastapi.project.path=${FASTAPI_PROJECT_PATH:../RevibeK_AI}
fastapi.launcher.enabled=${FASTAPI_LAUNCHER_ENABLED:false}
```

### src/main/java/com/ssafy/revibek/analysis/service/AnalysisServiceImpl.java

```java
package com.ssafy.revibek.analysis.service;

import com.ssafy.revibek.analysis.client.FastApiClient;
import com.ssafy.revibek.analysis.dto.AnalyzeRequestDto;
import com.ssafy.revibek.analysis.dto.AnalyzeResponseDto;
import com.ssafy.revibek.song.dto.SongDto;
import com.ssafy.revibek.song.service.SongService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalysisServiceImpl implements AnalysisService {

    private final FastApiClient fastApiClient;
    private final SongService songService;

    @Override
    public AnalyzeResponseDto analyze(SongDto song) {
        AnalyzeRequestDto request = new AnalyzeRequestDto(
                song.getYoutubeId(),
                song.getYoutubeUrl(),
                song.getTitle(),
                song.getDurationSeconds()
        );
        return fastApiClient.analyze(request);
    }

    @Override
    public void analyzeAndSave(SongDto song) {
        AnalyzeResponseDto response = analyze(song);

        if (response == null) {
            System.out.println("[Analysis] FastAPI 응답 없음: " + song.getTitle());
            return;
        }

        switch (response.getStatus()) {
            case "COMPLETED" -> {
                song.setBpm(response.getBpm());
                song.setEnergy(response.getEnergy());
                song.setDanceability(response.getDanceability());
                song.setLoudness(response.getLoudness());
                song.setMusicalKey(response.getMusicalKey());
                song.setMusicalScale(response.getMusicalScale());
                songService.modifySong(song);
                System.out.println("[Analysis] 완료: " + song.getTitle()
                        + " (source=" + response.getSource() + ")");
            }
            case "MOCK", "SKIPPED" -> {
                System.out.println("[Analysis] 저장 생략: " + song.getTitle()
                        + " (" + response.getMessage() + ")");
            }
            case "FAILED" -> {
                System.out.println("[Analysis] 실패: " + song.getTitle()
                        + " (" + response.getMessage() + ")");
            }
            default -> {
                System.out.println("[Analysis] 알 수 없는 상태: " + response.getStatus());
            }
        }
    }
}
```

## 5. DB 수정 필요 여부

**현재 로컬 DB에 sort_order ALTER 적용 여부만 확인 필요**

`radio_sessions.playlist_id` 연결 기능을 추가하려면 해당 컬럼과 FK 적용 여부도 먼저 확인해야 합니다.

## 6. 적용 순서

1. `PlaylistController.java` 수정
2. `application.properties` 수정
3. `GlobalExceptionHandler.java` 수정
4. `AnalysisServiceImpl.java` 수정
5. `radio_recommendations.sort_order` 적용 여부 확인
6. `radio_sessions.playlist_id` 적용 여부 확인
7. `mvn clean compile` 실행
8. `mvn test` 실행
9. 서버 실행 및 API 테스트

## 7. 실행 명령어

```powershell
mvn clean compile
mvn test
mvn spring-boot:run
```

## 8. 확인용 SQL

```sql
SHOW COLUMNS FROM radio_recommendations;
SHOW COLUMNS FROM radio_sessions;

SHOW INDEX FROM radio_recommendations;
SHOW INDEX FROM radio_sessions;

SELECT id, session_id, song_id, sort_order, reason
FROM radio_recommendations
ORDER BY session_id, sort_order;

SELECT id, user_id, playlist_id, mood, recommendation_source, created_at
FROM radio_sessions
ORDER BY created_at DESC;

SELECT rs.id AS session_id,
       rs.playlist_id,
       p.name AS playlist_name
FROM radio_sessions rs
LEFT JOIN playlists p ON p.id = rs.playlist_id
ORDER BY rs.created_at DESC;
```

`order_num`만 존재하고 `sort_order`가 없다면:

```sql
ALTER TABLE radio_recommendations
CHANGE COLUMN order_num sort_order TINYINT NOT NULL DEFAULT 1;
```

## 9. API 테스트 순서

1. `POST /api/auth/signup`으로 로컬 사용자 생성
2. `POST /api/auth/login`으로 JWT 발급
3. 이후 요청에 `Authorization: Bearer {accessToken}` 적용
4. `GET /api/songs`로 추천 가능한 곡 확인
5. `POST /api/preferences` 또는 관련 preference API로 사용자 취향 저장
6. `POST /api/radio`를 `saveAsPlaylist=false`로 호출
7. `radio_sessions`, `radio_recommendations.sort_order` 저장 결과 확인
8. `POST /api/radio`를 `saveAsPlaylist=true`로 호출
9. 응답의 `playlistId`와 `playlists`, `playlist_songs` 저장 결과 확인
10. `GET /api/radio/{id}`와 `GET /api/radio/me` 확인
11. `GET /api/playlists`와 `GET /api/playlists/{playlistId}` 확인
12. JWT 없이 인증 API를 호출하여 401 응답 확인
13. 권한이 없는 리소스 접근 시 403 또는 의도한 오류 응답 확인
14. FastAPI 비활성 상태에서 `/api/analysis/batch` 호출 후 songs 분석값이 변경되지 않는지 확인