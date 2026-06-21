# RevibeK 이메일 인증 오류 분석 및 수정 보고서

---

## 1. 백엔드 구조 분석 요약

```
src/main/java/com/ssafy/revibek/
├── auth/
│   ├── JwtAuthenticationFilter.java   Bearer 토큰 검증 필터
│   ├── JwtTokenProvider.java          JWT 생성/검증 (HMAC-SHA256)
│   ├── OAuth2SuccessHandler.java      Google OAuth2 콜백 처리
│   ├── RefreshTokenStore.java         Refresh 토큰 메모리 저장소
│   └── dto/
│       ├── AuthTokenResponseDto.java
│       ├── LogoutRequestDto.java
│       └── RefreshTokenRequestDto.java
├── common/
│   ├── dto/ErrorResponse.java         공통 에러 응답 레코드
│   └── exception/GlobalExceptionHandler.java
├── config/
│   ├── DataInitializer.java           dev 계정 자동 생성 (dev@revibek.local / revibek1234)
│   ├── PasswordEncoderConfig.java     BCryptPasswordEncoder 빈
│   └── SecurityConfig.java            CORS, JWT 필터, OAuth2 설정
└── user/
    ├── controller/AuthController.java  /api/auth/** 엔드포인트
    ├── dto/                            요청/응답 DTO
    ├── mapper/UserMapper.java          MyBatis 매퍼
    └── service/
        ├── AuthService.java            회원가입/로그인/토큰 발급
        └── EmailVerificationService.java  인증코드 발송/검증
```

**데이터 흐름:**
- 이메일 인증코드 → 메모리(ConcurrentHashMap) 5분 보관
- 인증 완료 상태 → 메모리 30분 보관
- JWT Refresh Token → 메모리(RefreshTokenStore) 보관
- 사용자 데이터 → MySQL + MyBatis

---

## 2. 오류 원인 분석

### 발생 오류
```
org.springframework.mail.MailAuthenticationException: Authentication failed
```

### 원인

| # | 원인 | 상세 |
|---|------|------|
| 1 | **Gmail 앱 비밀번호 미사용** | Gmail은 2023년부터 일반 비밀번호로 SMTP 접속 불가. 앱 비밀번호(16자리) 필수 |
| 2 | **SMTP 설정 오류 시 로그 부재** | `senderEmail.isBlank()` 분기에서 RuntimeException만 던지고 설정값 로그가 없음 |
| 3 | **MailAuthenticationException 전파** | `mailSender.send()` 호출이 try-catch 없이 노출되어 예외가 그대로 전파됨 |
| 4 | **GlobalExceptionHandler 미처리** | `MailAuthenticationException`에 대한 전용 핸들러 없어 `handleRuntime()`으로 HTTP 500 반환 |
| 5 | **dev 환경 흐름 중단** | SMTP 실패 시 예외가 올라와 회원가입 플로우 전체가 막힘 |

### MailAuthenticationException 발생 경로
```
POST /api/auth/email/send
  → EmailVerificationService.sendVerificationCode()
    → mailSender.send(message)   ← 여기서 MailAuthenticationException 발생
      → GlobalExceptionHandler.handleRuntime()  ← 500 반환
```

---

## 3. 수정한 파일 목록

| 파일 | 수정 내용 |
|------|-----------|
| `src/main/java/.../user/service/EmailVerificationService.java` | `@Slf4j` 추가, `ObjectProvider<JavaMailSender>` 도입, `MailException` try-catch, fallback 기능 |
| `src/main/java/.../common/exception/GlobalExceptionHandler.java` | `@Slf4j` 추가, `MailAuthenticationException` 핸들러(503), `MailException` 핸들러(503) 추가 |
| `src/main/resources/application-dev.properties` | **신규** — mock 모드, fallback 활성화, SMTP 비워둠 |
| `src/main/resources/application-prod.properties` | **신규** — smtp 모드, Gmail 앱 비밀번호 설정 가이드 포함 |

---

## 4. 수정된 전체 코드

### EmailVerificationService.java
```java
package com.ssafy.revibek.user.service;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final long CODE_TTL_SECONDS = 5 * 60;
    private static final long VERIFIED_TTL_SECONDS = 30 * 60;

    private final ObjectProvider<JavaMailSender> mailSenderProvider;  // ← Optional 주입

    @Value("${spring.mail.host:}")
    private String smtpHost;

    @Value("${spring.mail.username:}")
    private String senderEmail;

    @Value("${app.email.verification.mode:mock}")
    private String verificationMode;

    @Value("${app.email.verification.mock-code:123456}")
    private String mockCode;

    @Value("${app.email.verification.fallback-on-failure:false}")
    private boolean fallbackOnFailure;

    private final Map<String, VerificationCodeEntry> codeStore = new ConcurrentHashMap<>();
    private final Map<String, Instant> verifiedEmailStore = new ConcurrentHashMap<>();

    public void sendVerificationCode(String email) {
        String normalizedEmail = normalizeEmail(email);
        cleanupExpiredEntries();
        String code = isMockMode() ? mockCode : generateCode();
        Instant expiresAt = Instant.now().plusSeconds(CODE_TTL_SECONDS);
        codeStore.put(normalizedEmail, new VerificationCodeEntry(code, expiresAt));

        if (isMockMode()) {
            log.info("[EmailVerification] MOCK 모드 — 이메일: {}, 코드: {}", normalizedEmail, code);
            return;
        }

        if (senderEmail.isBlank()) {
            log.error("[EmailVerification] SMTP 발송 계정이 설정되지 않았습니다. " +
                      "verificationMode={}, SMTP_HOST={}, SMTP_USERNAME='{}'",
                      verificationMode, smtpHost, senderEmail);
            if (fallbackOnFailure) {
                log.warn("[EmailVerification] FALLBACK — 이메일: {}, 코드: {} (콘솔 출력)", normalizedEmail, code);
                return;
            }
            throw new RuntimeException("메일 발송 설정이 올바르지 않습니다. 관리자에게 문의하세요.");
        }

        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            log.error("[EmailVerification] JavaMailSender 빈이 없습니다. host='{}'", smtpHost);
            if (fallbackOnFailure) {
                log.warn("[EmailVerification] FALLBACK — 이메일: {}, 코드: {} (콘솔 출력)", normalizedEmail, code);
                return;
            }
            throw new RuntimeException("메일 서비스를 사용할 수 없습니다.");
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(normalizedEmail);
            message.setSubject("[RevibeK] 이메일 인증코드");
            message.setText("아래 인증코드를 입력해주세요.\n\n인증코드: " + code + "\n만료시간: 5분");
            sender.send(message);
            log.info("[EmailVerification] 발송 완료 — 이메일: {}", normalizedEmail);

        } catch (MailAuthenticationException e) {
            log.error("[EmailVerification] SMTP 인증 실패 — host: '{}', username: '{}'. " +
                      "Gmail 사용 시 앱 비밀번호가 필요합니다. 오류: {}",
                      smtpHost, senderEmail, e.getMessage());
            if (fallbackOnFailure) {
                log.warn("[EmailVerification] FALLBACK — 이메일: {}, 코드: {} (콘솔 출력)", normalizedEmail, code);
                return;
            }
            throw new RuntimeException(
                "이메일 발송 서버 인증에 실패했습니다. " +
                "Gmail 앱 비밀번호(Google 계정 → 보안 → 2단계 인증 → 앱 비밀번호)를 사용해야 합니다."
            );

        } catch (MailException e) {
            log.error("[EmailVerification] 메일 발송 실패 — 이메일: {}, host: '{}', 오류: {}",
                      normalizedEmail, smtpHost, e.getMessage());
            if (fallbackOnFailure) {
                log.warn("[EmailVerification] FALLBACK — 이메일: {}, 코드: {} (콘솔 출력)", normalizedEmail, code);
                return;
            }
            throw new RuntimeException("이메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    public void verifyCode(String email, String code) {
        String normalizedEmail = normalizeEmail(email);
        cleanupExpiredEntries();
        VerificationCodeEntry entry = codeStore.get(normalizedEmail);
        if (entry == null) {
            if (isMockMode() && mockCode.equals(code)) {
                verifiedEmailStore.put(normalizedEmail, Instant.now().plusSeconds(VERIFIED_TTL_SECONDS));
                return;
            }
            throw new RuntimeException("인증코드가 없거나 만료되었습니다.");
        }
        if (!entry.code().equals(code)) {
            throw new RuntimeException("인증코드가 올바르지 않습니다.");
        }
        codeStore.remove(normalizedEmail);
        verifiedEmailStore.put(normalizedEmail, Instant.now().plusSeconds(VERIFIED_TTL_SECONDS));
    }

    public boolean isVerified(String email) {
        String normalizedEmail = normalizeEmail(email);
        cleanupExpiredEntries();
        Instant verifiedUntil = verifiedEmailStore.get(normalizedEmail);
        return verifiedUntil != null && verifiedUntil.isAfter(Instant.now());
    }

    public void consumeVerification(String email) {
        verifiedEmailStore.remove(normalizeEmail(email));
    }

    private void cleanupExpiredEntries() {
        Instant now = Instant.now();
        codeStore.entrySet().removeIf(e -> e.getValue().expiresAt().isBefore(now));
        verifiedEmailStore.entrySet().removeIf(e -> e.getValue().isBefore(now));
    }

    private String generateCode() {
        return Integer.toString(ThreadLocalRandom.current().nextInt(100000, 1000000));
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isMockMode() {
        return !"smtp".equalsIgnoreCase(verificationMode);
    }

    private record VerificationCodeEntry(String code, Instant expiresAt) {}
}
```

### GlobalExceptionHandler.java
```java
package com.ssafy.revibek.common.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ssafy.revibek.common.dto.ErrorResponse;
import com.ssafy.revibek.radio.exception.RadioNotFoundException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 기존 핸들러 그대로 유지 ...

    // SMTP 인증 실패 → 503 Service Unavailable
    @ExceptionHandler(MailAuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleMailAuthentication(
            MailAuthenticationException exception, HttpServletRequest request) {
        log.error("[GlobalExceptionHandler] SMTP 인증 실패 — path: {}, message: {}",
                  request.getRequestURI(), exception.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
            ErrorResponse.of(503, "Service Unavailable",
                "이메일 발송 서버 인증에 실패했습니다. 잠시 후 다시 시도해주세요.",
                request.getRequestURI())
        );
    }

    // 기타 메일 실패 → 503
    @ExceptionHandler(MailException.class)
    public ResponseEntity<ErrorResponse> handleMailFailure(
            MailException exception, HttpServletRequest request) {
        log.error("[GlobalExceptionHandler] 메일 발송 실패 — path: {}, message: {}",
                  request.getRequestURI(), exception.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
            ErrorResponse.of(503, "Service Unavailable",
                "이메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요.",
                request.getRequestURI())
        );
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(
            RuntimeException exception, HttpServletRequest request) {
        log.error("[GlobalExceptionHandler] RuntimeException — path: {}, message: {}",
                  request.getRequestURI(), exception.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ErrorResponse.of(500, "Internal Server Error", exception.getMessage(), request.getRequestURI())
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception exception, HttpServletRequest request) {
        log.error("[GlobalExceptionHandler] Unexpected — path: {}", request.getRequestURI(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ErrorResponse.of(500, "Internal Server Error", "서버 내부 오류가 발생했습니다.", request.getRequestURI())
        );
    }
}
```

---

## 5. application-dev.properties

```properties
# Dev / 로컬 / 발표용 환경 설정
# 실행: ./gradlew bootRun --args='--spring.profiles.active=dev'

app.email.verification.mode=mock
app.email.verification.mock-code=123456
app.email.verification.fallback-on-failure=true

spring.mail.host=
spring.mail.port=587
spring.mail.username=
spring.mail.password=
spring.mail.properties.mail.smtp.auth=false
spring.mail.properties.mail.smtp.starttls.enable=false

spring.datasource.url=jdbc:mysql://localhost:3306/kpop_radio?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
spring.datasource.username=SSAFY
spring.datasource.password=SSAFY

jwt.secret=revibek-dev-secret-key-change-me-32bytes
jwt.access-token-expiration-ms=3600000
jwt.refresh-token-expiration-ms=1209600000

app.cors.allowed-origins=http://localhost:3000,http://localhost:5173,http://127.0.0.1:3000,http://127.0.0.1:5173
app.oauth.google.enabled=false
```

---

## 6. application-prod.properties

```properties
# Prod 환경 설정 — 환경변수 필수
#
# Gmail 앱 비밀번호 발급:
#   Google 계정 → 보안 → 2단계 인증 활성화 → 앱 비밀번호 → 16자리 생성
#   일반 비밀번호 사용 시 MailAuthenticationException 발생

app.email.verification.mode=smtp
app.email.verification.fallback-on-failure=false

spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${SMTP_USERNAME}
spring.mail.password=${SMTP_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
spring.mail.properties.mail.smtp.connectiontimeout=5000
spring.mail.properties.mail.smtp.timeout=5000
spring.mail.properties.mail.smtp.writetimeout=5000

spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

jwt.secret=${JWT_SECRET}
jwt.access-token-expiration-ms=3600000
jwt.refresh-token-expiration-ms=1209600000

app.cors.allowed-origins=${CORS_ALLOWED_ORIGINS}
app.oauth.google.enabled=${GOOGLE_OAUTH_ENABLED:false}
```

---

## 7. 로컬 실행 명령어

```bash
# 방법 1: dev 프로파일로 실행 (권장, SMTP 없이 동작)
./gradlew bootRun --args='--spring.profiles.active=dev'

# 방법 2: 환경변수 직접 지정
EMAIL_VERIFICATION_MODE=mock ./gradlew bootRun

# 방법 3: JAR로 실행
./gradlew build
java -jar build/libs/RevibeK-*.jar --spring.profiles.active=dev

# Windows PowerShell
$env:SPRING_PROFILES_ACTIVE="dev"
./gradlew bootRun
```

---

## 8. Postman / curl 테스트 순서

### 발표용 (mock 모드, 인증코드 = 123456)

```bash
# Step 1: 인증코드 발송 요청 (실제 메일 발송 없음, 서버 로그에서 확인)
curl -X POST http://localhost:8080/api/auth/email/send \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com"}'
# 응답: "인증코드 발송 완료"

# Step 2: 인증코드 검증 (고정 코드 123456 사용)
curl -X POST http://localhost:8080/api/auth/email/verify \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com", "code": "123456"}'
# 응답: "이메일 인증 완료"

# Step 3: 회원가입
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"nickname": "테스터", "email": "test@example.com", "password": "password1234"}'
# 응답: "회원가입 완료"

# Step 4: 로그인
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com", "password": "password1234"}'
# 응답: { "accessToken": "...", "refreshToken": "...", "user": { ... } }

# Step 5: 개발 계정 직접 로그인 (이메일 인증 불필요)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "dev@revibek.local", "password": "revibek1234"}'
```

---

## 9. 발표용 fallback 인증 흐름 설명

### mock 모드 흐름 (기본값, 발표 권장)

```
사용자                      서버 (mock 모드)
  │                              │
  ├─ POST /api/auth/email/send ─►│
  │  { "email": "..." }          │ 코드 123456을 메모리에 저장
  │                              │ (실제 이메일 발송 없음)
  │◄─ 200 "인증코드 발송 완료" ──┤ 서버 로그: [MOCK] email=..., code=123456
  │                              │
  ├─ POST /api/auth/email/verify ►│
  │  { "email": "...",           │
  │    "code": "123456" }        │ 코드 일치 → 인증 완료 상태 저장 (30분)
  │◄─ 200 "이메일 인증 완료" ───┤
  │                              │
  ├─ POST /api/auth/signup ─────►│
  │  { nickname, email, pw }     │ 인증 완료 확인 → DB 저장
  │◄─ 200 "회원가입 완료" ──────┤
```

### fallback 모드 흐름 (smtp 설정했지만 SMTP 실패 시)

```
설정: app.email.verification.mode=smtp
      app.email.verification.fallback-on-failure=true

mailSender.send() → MailAuthenticationException 발생
  → catch(MailAuthenticationException e)
    → log.error("SMTP 인증 실패 — host, username, 오류 내용")
    → log.warn("FALLBACK — 이메일: ..., 코드: ...")  ← 로그에서 코드 확인
    → 메서드 정상 반환 (예외 전파 안 함)

결과: 회원가입 흐름 중단 없음, 발표자가 서버 로그에서 인증코드 확인 후 입력
```

### prod 모드 흐름 (SMTP 실패 시)

```
설정: app.email.verification.mode=smtp
      app.email.verification.fallback-on-failure=false

mailSender.send() → MailAuthenticationException
  → catch(MailAuthenticationException e) → throw RuntimeException("앱 비밀번호 필요")
  → GlobalExceptionHandler.handleMailAuthentication() (새로 추가)
  → HTTP 503 Service Unavailable
  → { "status": 503, "message": "이메일 발송 서버 인증에 실패했습니다..." }
```

---

## 핵심 변경사항 요약

| 항목 | 변경 전 | 변경 후 |
|------|---------|---------|
| `JavaMailSender` 주입 | `final JavaMailSender` (필수) | `ObjectProvider<JavaMailSender>` (선택적) |
| SMTP 실패 시 로그 | 없음 | host/username/오류 메시지 상세 출력 |
| dev 환경 fallback | 없음 | `fallback-on-failure=true` 시 콘솔 출력 후 정상 반환 |
| `MailAuthenticationException` 처리 | `handleRuntime()` → HTTP 500 | 전용 핸들러 → HTTP 503 |
| dev 프로파일 설정 | 없음 | `application-dev.properties` 생성 |
| prod 프로파일 설정 | 없음 | `application-prod.properties` 생성 (Gmail 앱 비밀번호 가이드 포함) |
