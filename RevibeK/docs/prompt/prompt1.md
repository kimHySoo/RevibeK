RevibeK 백엔드 실행 중 아래 오류가 반복 발생했습니다.

에러 로그:
org.springframework.mail.MailAuthenticationException: Authentication failed

상황:
- 이메일 인증번호 발송 과정에서 발생한 것으로 보입니다.
- 서버는 8080 포트에서 실행 중입니다.
- ExceptionHandlerExceptionResolver가 MailAuthenticationException을 처리하고 있습니다.
- 같은 오류가 여러 요청 스레드에서 반복 발생합니다.

요청:
RevibeK 백엔드 전체 구조를 분석한 뒤, 이메일 인증/메일 발송 구조를 중심으로 오류 원인을 찾고 안정적으로 수정해주세요.

반드시 확인할 파일/영역:
1. application.properties / application.yml
2. application-dev.properties / application-prod.properties 존재 여부
3. MailConfig 또는 JavaMailSender 설정
4. MailService / EmailService / EmailAuthService
5. AuthController / UserController 중 이메일 인증 요청 API
6. SecurityConfig
7. GlobalExceptionHandler
8. 환경변수, .env, Infisical 설정 주입 여부

수정 요구사항:
1. Gmail SMTP 사용 시 일반 비밀번호가 아니라 앱 비밀번호를 쓰도록 설정 예시를 작성해주세요.
2. spring.mail.username / spring.mail.password 값이 비어 있거나 잘못됐을 때 원인을 알 수 있게 로그를 개선해주세요.
3. dev 환경에서는 SMTP 인증 실패가 나도 서버가 죽거나 회원가입 흐름이 막히지 않게 해주세요.
4. dev 환경에서는 메일 발송 실패 시 고정 인증코드 또는 콘솔 출력 인증코드로 fallback 처리해주세요.
5. prod 환경에서는 메일 인증 실패 시 명확한 에러 응답을 반환하게 해주세요.
6. 기존 회원가입/로그인/JWT 구조는 삭제하지 말고 최소 수정으로 안정화해주세요.
7. 발표용 프로젝트이므로 실제 SMTP가 없어도 회원가입, 로그인, 이메일 인증 테스트가 가능해야 합니다.

원하는 결과물:
1. 백엔드 구조 분석 요약
2. 오류 원인 분석
3. 수정해야 할 파일 목록
4. 수정된 전체 코드
5. application-dev.properties 예시
6. application-prod.properties 예시
7. 로컬 실행 명령어
8. Postman 또는 curl 테스트 순서
9. 발표용 fallback 인증 흐름 설명

주의:
- Java 21 기준
- Spring Boot 기준
- UTF-8 인코딩 유지
- 기존 RevibeK 패키지 구조 유지
- 코드 일부가 아니라 필요한 파일은 전체 코드로 제시