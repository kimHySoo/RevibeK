@'
# RevibeK 백엔드 전체 점검 및 디버깅 요청

현재 RevibeK 프로젝트의 백엔드 전체 상태를 점검하고, 실행/디버깅 관점에서 문제 가능성을 분석해주세요.

이번 요청에서는 코드를 수정하지 말고, 현재 백엔드가 어디까지 구현되어 있고 무엇이 부족한지 전체적으로 분석만 해주세요.

단순히 기능을 하나씩 나열하지 말고, 백엔드 전체 흐름이 실제로 실행 가능한지와 디버깅 시 어디서 문제가 날 가능성이 높은지를 중심으로 봐주세요.

확인 범위:
- src/main/java 전체
- src/main/resources/mapper 전체
- src/main/resources/sql 전체
- application.properties 또는 application.yml
- build.gradle 또는 pom.xml

중점 점검:
1. 컴파일 오류 가능성
2. Bean 생성 오류 가능성
3. MyBatis Mapper namespace/id/resultMap 불일치
4. DB schema와 Mapper SQL 불일치
5. 인증/OAuth/JWT/userId 처리 흐름
6. Radio API와 Playlist 저장 연결
7. selectedSongs 처리 흐름
8. Like API 연결 상태
9. YouTube/FastAPI/Qdrant 연동 상태
10. 실행 시 예상 오류와 디버깅 우선순위

답변 형식:
1. 전체 결론
2. 백엔드 전체 구조 요약
3. 실행 전 반드시 확인할 문제
4. 기능 흐름 연결 상태
5. DB / Mapper 디버깅 결과
6. 인증 / OAuth / JWT 디버깅 결과
7. 외부 API 디버깅 결과
8. 예상 오류 TOP 10
9. 지금 바로 디버깅해야 할 파일 TOP 5
10. 실제 테스트 순서
11. 다음 작업 순서
12. 최종 판단

중요:
- 코드는 수정하지 마세요.
- 전체 코드를 작성하지 마세요.
- 현재 프로젝트 파일 기준으로 판단해주세요.
- 추측하지 말고 실제 파일명, 클래스명, 테이블명을 기준으로 말해주세요.
- 기능 목록만 나열하지 말고 전체 백엔드 흐름과 디버깅 가능성을 중심으로 평가해주세요.
  '@ | Out-File -FilePath .\docs\prompt\prompt4.md -Encoding utf8