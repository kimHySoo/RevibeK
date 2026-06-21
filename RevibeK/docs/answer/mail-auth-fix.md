# Gmail SMTP `MailAuthenticationException` 수정 정리

## 1. 문제

```
Resolved [org.springframework.mail.MailAuthenticationException: Authentication failed]
```

`POST /api/auth/email/send` 호출 시 Gmail SMTP 인증이 실패하면서 예외가 발생했고,
`GlobalExceptionHandler`의 `RuntimeException` 핸들러가 이를 그대로 500 에러로 응답해
회원가입 흐름 전체가 막혔다.

### 원인
- `application.properties`는 `spring.profiles.include=secret`으로 `application-secret.properties`를 항상 포함한다.
- `application-secret.properties`에 `app.email.verification.mode=smtp`가 설정되어 있어 기본값(`mock`)을 덮어쓰고 실제 SMTP 발송을 시도한다.
- `spring.mail.password`에 들어있는 Gmail 앱 비밀번호가 만료/거부 상태라 Gmail이 인증을 거부했다.
- `EmailVerificationService.sendVerificationCode()`가 `mailSender.send()`의 예외를 잡지 않아 그대로 컨트롤러까지 전파되었고, `GlobalExceptionHandler`가 이를 500으로 응답했다.

## 2. 수정한 파일

| 파일 | 변경 내용 |
|---|---|
| [EmailVerificationService.java](../../src/main/java/com/ssafy/revibek/user/service/EmailVerificationService.java) | `MailException`(`MailAuthenticationException` 포함)을 캐치해서 로그로 남기고, `fail-open` 옵션에 따라 흐름을 계속 진행하거나 안전한 메시지의 `EmailSendFailedException`을 던지도록 수정 |
| [EmailSendFailedException.java](../../src/main/java/com/ssafy/revibek/common/exception/EmailSendFailedException.java) | 메일 발송 실패를 나타내는 전용 예외 클래스 신규 추가 |
| [GlobalExceptionHandler.java](../../src/main/java/com/ssafy/revibek/common/exception/GlobalExceptionHandler.java) | `EmailSendFailedException` 핸들러 추가, 503(SERVICE_UNAVAILABLE)로 응답해 일반 500과 구분 |
| [application.properties](../../src/main/resources/application.properties) | `app.email.verification.fail-open` 옵션 추가, Gmail 앱 비밀번호 필요성에 대한 주석 추가 |
| [application-secret.properties](../../src/main/resources/application-secret.properties) | Gmail 앱 비밀번호 관련 주석 추가, fail-open 재정의 가이드 주석 추가 |

## 3. 동작 방식

### 3.1 인증 모드 (`app.email.verification.mode`)
- `mock` (기본값): 메일을 보내지 않고 `mock-code`(기본 `123456`)를 인증코드로 사용한다. 콘솔에 `[MOCK] 이메일 인증코드 발송 - email=..., code=...` 로그가 남는다. **발표/로컬 환경 권장.**
- `smtp`: 실제 Gmail SMTP로 메일을 발송한다. `spring.mail.*` 설정이 모두 필요하다.

### 3.2 발송 실패 안전장치 (`app.email.verification.fail-open`)
- `true` (기본값): SMTP 인증/발송이 실패해도 예외를 던지지 않는다. 인증코드는 이미 서버 메모리에 저장되어 있으므로, 콘솔 로그(`[FAIL-OPEN] ...`)로 코드가 출력되고 `/api/auth/email/send`는 200을 반환한다. 사용자는 콘솔에 출력된 코드로 `/api/auth/email/verify`를 그대로 진행할 수 있다.
- `false`: 발송 실패 시 `EmailSendFailedException`이 발생하고 HTTP 503 + 안전한 한국어 메시지(`"이메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요."`)로 응답한다. 더 이상 500이나 원본 스택트레이스 메시지가 노출되지 않는다.

### 3.3 Gmail 앱 비밀번호
Gmail SMTP를 쓰려면 `spring.mail.password`에 **일반 로그인 비밀번호가 아니라 앱 비밀번호**를 넣어야 한다.
1. Google 계정 → 보안 → 2단계 인증 활성화
2. https://myaccount.google.com/apppasswords 접속해서 앱 비밀번호 발급 (공백 없이 16자리)
3. 발급받은 값을 `spring.mail.password`(또는 `SMTP_PASSWORD` 환경변수)에 설정

현재 `application-secret.properties`에 들어있는 비밀번호가 거부되고 있다면 위 절차로 재발급해야 한다.

## 4. 실행 방법

### 발표/데모용 (메일 발송 없이 동작)
`application-secret.properties`의 `app.email.verification.mode=smtp`를 주석 처리하거나 `mock`으로 바꾸면 기본값(`mock`)이 적용된다. 또는 환경변수로 덮어쓴다.

```powershell
$env:EMAIL_VERIFICATION_MODE = "mock"
.\mvnw.cmd spring-boot:run
```

### 실제 Gmail 발송 테스트
1. 위 3.3 절차로 유효한 앱 비밀번호를 발급받아 `application-secret.properties`(또는 `SMTP_PASSWORD` 환경변수)에 반영
2. `app.email.verification.mode=smtp` 유지
3. 서버 기동 후 아래로 테스트

```powershell
curl -X POST http://localhost:8080/api/auth/email/send `
  -H "Content-Type: application/json" `
  -d '{"email":"받는사람@example.com"}'
```

## 5. 테스트 방법

1. **정상(mock) 흐름**: `EMAIL_VERIFICATION_MODE=mock` 상태에서 `/api/auth/email/send` 호출 → 200 응답, 콘솔에 `[MOCK]` 로그로 코드 확인 → `/api/auth/email/verify`에 같은 코드로 호출 → 200 응답 → `/api/auth/signup` 정상 진행 확인.
2. **SMTP 인증 실패 + fail-open=true (기본값)**: 일부러 잘못된 비밀번호를 둔 채 `mode=smtp`로 `/api/auth/email/send` 호출 → 더 이상 500이 아니라 200 응답이 오고, 콘솔에 `[FAIL-OPEN]` 로그로 코드가 출력되는지 확인 → 해당 코드로 `/api/auth/email/verify`까지 정상 진행되는지 확인.
3. **SMTP 인증 실패 + fail-open=false**: `EMAIL_VERIFICATION_FAIL_OPEN=false`로 설정 후 동일하게 실패를 유도 → 응답이 HTTP 503, 바디 메시지가 `"이메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요."`인지 확인 (원본 예외 메시지나 스택트레이스가 노출되지 않아야 한다).
4. **정상 SMTP 발송**: 유효한 앱 비밀번호로 `/api/auth/email/send` 호출 → 실제 수신 메일함에서 인증코드 메일 수신 확인.
