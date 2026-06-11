현재 Spring Boot 백엔드 프로젝트 전체를 분석한 DB 점검 결과를 바탕으로, DB 컬럼 불일치 문제를 수정하기 위한 대상 파일과 수정 후 전체 코드를 작성해주세요.

중요:
- 실제 프로젝트 파일을 직접 수정하지 마세요.
- apply_patch를 사용하지 마세요.
- 파일을 저장하거나 변경하지 마세요.
- 수정 방향만 설명하지 말고, 수정이 필요한 파일의 전체 코드를 답변으로 작성해주세요.
- “변경 부분만”, “이하 동일”, “생략”, “...” 같은 표현을 쓰지 마세요.
- 현재 프로젝트의 실제 파일을 읽고, 기존 패키지명/import/클래스명/메서드명/DTO 필드명/Mapper 메서드명/XML namespace를 유지해주세요.
- 대규모 리팩토링 없이 최소 수정으로 작성해주세요.
- 컴파일 오류가 나지 않도록 필요한 import까지 포함해서 작성해주세요.

현재 DB 점검 결과:
- 전체 테이블과 대부분의 컬럼은 정상입니다.
- 코드에서 사용하는 테이블은 모두 스키마에 존재합니다.
- Mapper interface와 Mapper XML의 namespace/id/@Param 매칭은 정상입니다.
- 문제는 radio_recommendations 테이블의 순서 컬럼명입니다.

문제:
- 최종 스키마 kpop_radio_schema.sql 기준 radio_recommendations 테이블에는 sort_order 컬럼이 있습니다.
- 현재 RadioMapper.xml은 radio_recommendations.order_num 컬럼을 사용합니다.
- 따라서 라디오 추천곡 저장/조회 시 Unknown column 'order_num' 오류가 발생할 수 있습니다.

수정 원칙:
- DB 스키마를 order_num으로 되돌리지 마세요.
- 최종 스키마 기준인 sort_order에 맞춰 코드와 Mapper XML을 수정해주세요.
- 가능하면 Java 메서드 파라미터명 orderNum은 유지해도 됩니다.
- 단, XML에서 INSERT/SELECT/ORDER BY에 사용하는 실제 DB 컬럼명은 sort_order로 맞춰주세요.
- Java DTO 필드명이 orderNum이라면 SQL에서 sort_order AS order_num 또는 sort_order AS orderNum 방식으로 매핑이 깨지지 않게 처리해주세요.
- 기존 radio_sessions, radio_recommendations 저장/조회 기능이 깨지면 안 됩니다.
- 기존 RadioMapper interface 메서드명은 최대한 유지해주세요.

반드시 확인할 파일:
- src/main/java/com/ssafy/revibek/radio/mapper/RadioMapper.java
- src/main/resources/mapper/radio/RadioMapper.xml
- src/main/java/com/ssafy/revibek/radio/dto/RadioResponseDto.java
- src/main/java/com/ssafy/revibek/radio/service/RadioService.java
- src/main/resources/sql/kpop_radio_schema.sql

수정해야 할 가능성이 높은 부분:

1. RadioMapper.xml
- insertRecommendation statement에서 order_num 컬럼을 사용하고 있다면 sort_order로 변경해주세요.
- selectRecommendationBySessionId statement에서 order_num을 조회하거나 ORDER BY order_num을 사용한다면 sort_order로 변경해주세요.
- DTO 매핑이 orderNum 필드를 기대한다면 alias를 사용해주세요.
  예:
  sort_order AS order_num
  또는
  sort_order AS orderNum
  현재 프로젝트의 MyBatis 매핑 스타일에 맞게 선택해주세요.

2. RadioMapper.java
- @Param("orderNum")은 유지해도 됩니다.
- XML에서 #{orderNum}으로 값을 받아 DB 컬럼 sort_order에 넣는 방식이면 Java interface 수정은 필요 없을 수 있습니다.
- 수정이 필요하면 수정 후 전체 코드를 작성해주세요.
- 수정이 필요 없으면 “수정 불필요”라고 명시해주세요.

3. RadioResponseDto.java
- 추천곡 순서 필드가 orderNum, order, rank 등 어떤 이름인지 확인해주세요.
- XML alias와 DTO 필드명이 일치하는지 확인해주세요.
- 수정이 필요하면 수정 후 전체 코드를 작성해주세요.
- 수정이 필요 없으면 “수정 불필요”라고 명시해주세요.

4. RadioService.java
- radioMapper.insertRecommendation(sessionId, songId, i + 1, reason) 호출부가 현재 RadioMapper.java와 맞는지 확인해주세요.
- Java 파라미터명은 orderNum이어도 DB 컬럼은 sort_order로 저장되게 해주세요.
- 수정이 필요하면 수정 후 전체 코드를 작성해주세요.
- 수정이 필요 없으면 “수정 불필요”라고 명시해주세요.

5. kpop_radio_schema.sql
- radio_recommendations 테이블의 컬럼명이 sort_order인지 확인해주세요.
- 스키마가 이미 sort_order라면 SQL 파일은 수정하지 마세요.
- SQL 파일 수정이 필요 없으면 “수정 불필요”이라고 명시해주세요.

답변 형식:

## 1. 최종 판단
- 고쳐야 할 파일
- 수정 불필요 파일
- DB 수정 필요 여부

## 2. 수정 대상 파일별 전체 코드

수정이 필요한 파일은 반드시 전체 코드로 작성해주세요.

### src/main/resources/mapper/radio/RadioMapper.xml

```xml
전체 XML 코드