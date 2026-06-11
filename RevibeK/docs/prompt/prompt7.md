현재 Spring Boot 백엔드 프로젝트 전체를 분석해서, 현재 코드가 사용하는 DB 테이블과 컬럼이 실제 스키마에 모두 존재하는지 확인해주세요.

중요:
- 특정 기능 하나만 보지 말고 프로젝트 전체를 기준으로 확인해주세요.
- 실제 파일을 수정하지 마세요.
- DB를 직접 변경하지 마세요.
- apply_patch를 사용하지 마세요.
- 분석만 수행해주세요.
- src/main/resources/sql 디렉터리의 SQL 파일, Mapper XML, Mapper interface, DTO, Service, Controller를 모두 비교해주세요.
- 최종적으로 “DB 수정이 필요한지 / 필요 없는지”를 명확히 판단해주세요.

확인할 항목:

1. 코드에서 사용하는 모든 테이블 확인
- Mapper XML에서 사용하는 테이블
- Service/DAO/Mapper에서 호출하는 테이블
- SQL 파일에 실제로 존재하는 테이블
- 코드에는 있는데 스키마에는 없는 테이블이 있는지 확인해주세요.

2. 코드에서 사용하는 모든 컬럼 확인
- Mapper XML의 SELECT / INSERT / UPDATE / DELETE에서 사용하는 컬럼
- DTO 필드와 매핑되는 컬럼
- Service에서 기대하는 컬럼
- SQL 스키마에 실제로 존재하는 컬럼
- 컬럼명 불일치가 있는지 확인해주세요.

3. 타입 확인
- Java 코드에서 String으로 다루는 ID가 DB에서 VARCHAR/CHAR 등으로 되어 있는지 확인해주세요.
- Java 코드에서 Integer/Boolean/LocalDateTime 등으로 다루는 값이 DB 타입과 맞는지 확인해주세요.
- UUID 문자열을 저장해야 하는 컬럼이 숫자 타입으로 되어 있지 않은지 확인해주세요.

4. Mapper 매칭 확인
- Java Mapper interface의 메서드명과 Mapper XML의 statement id가 일치하는지 확인해주세요.
- Mapper XML namespace가 Java Mapper interface 경로와 일치하는지 확인해주세요.
- @Param 이름과 XML에서 사용하는 파라미터명이 일치하는지 확인해주세요.

5. 외래키/관계 확인
- 코드상 관계가 필요한 테이블 사이에 참조 컬럼이 존재하는지 확인해주세요.
- 외래키가 실제로 없어도 기능상 문제가 없는지, 데이터 무결성상 추가하는 게 좋은지 구분해주세요.

6. 초기 데이터 확인
- 기능 테스트에 필요한 기본 데이터가 SQL 파일에 포함되어 있는지 확인해주세요.
- 데이터가 없으면 API는 정상이어도 결과가 비어 보일 수 있는 부분을 알려주세요.

7. 자동 실행 여부 확인
- SQL 파일명이 schema.sql 또는 data.sql인지 확인해주세요.
- spring.sql.init.* 설정이 있는지 확인해주세요.
- 애플리케이션 실행만으로 DB가 자동 생성되는지, 수동 실행이 필요한지 판단해주세요.

반드시 비교할 파일:
- src/main/resources/sql/**/*.sql
- src/main/resources/mapper/**/*.xml
- src/main/java/**/mapper/*.java
- src/main/java/**/dao/*.java
- src/main/java/**/dto/*.java
- src/main/java/**/service/*.java
- src/main/java/**/controller/*.java
- src/main/resources/application.properties
- src/main/resources/application-*.properties

답변 형식:

## 1. 최종 판단

아래 중 하나로 명확하게 답해주세요.

- DB 수정 필요
- DB 수정 불필요
- DB 자체는 충분하지만 초기 데이터 또는 실행 설정 필요
- 일부 테이블/컬럼 확인 불가

그리고 이유를 짧게 요약해주세요.

## 2. 코드가 사용하는 테이블 목록

아래 형식으로 작성해주세요.

| 테이블명 | 사용 위치 | 스키마 존재 여부 | 상태 |
|---|---|---|---|

## 3. 테이블별 컬럼 매칭 결과

각 테이블마다 아래 형식으로 작성해주세요.

### 테이블명

코드에서 사용하는 컬럼:
- 컬럼1
- 컬럼2

스키마에 존재하는 컬럼:
- 컬럼1
- 컬럼2

판단:
- 문제 없음 / 컬럼 누락 / 컬럼명 불일치 / 타입 불일치

## 4. Mapper 매칭 결과

아래 형식으로 작성해주세요.

| Mapper | XML namespace 일치 | statement id 일치 | @Param 일치 | 상태 |
|---|---|---|---|---|

## 5. 누락 또는 불일치 항목

문제가 있다면 아래 기준으로 정리해주세요.

- 누락 테이블
- 누락 컬럼
- 컬럼명 불일치
- 타입 불일치
- Mapper namespace 불일치
- Mapper statement id 불일치
- @Param 이름 불일치
- SQL 자동 실행 설정 누락

문제가 없으면 “문제 없음”이라고 작성해주세요.

## 6. DB 수정이 필요하다면 SQL 작성

주의:
- DROP TABLE 사용 금지
- 기존 데이터가 날아가지 않도록 ALTER TABLE 위주로 작성
- 새 테이블이 완전히 없을 때만 CREATE TABLE 작성
- 실제 필요한 SQL만 작성

형식:

```sql
-- 필요한 SQL