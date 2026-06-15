# RevibeK radio_recommendations order_num / sort_order 불일치 확인 및 수정 코드 제안 요청

현재 프로젝트는 `RevibeK` Maven Wrapper 기반 Spring Boot 백엔드 프로젝트입니다.

이번 작업의 목적은 프론트엔드 구현 전에 반드시 확인해야 하는 백엔드 문제인
`radio_recommendations.order_num` 과 `RadioMapper.xml`의 `sort_order` 불일치를 확인하고,
직접 수정하지 말고 **내가 직접 고칠 수 있도록 수정 후 전체 코드 전문을 제안받는 것**입니다.

중요: 이번 작업에서는 실제 코드를 수정하지 마세요.
수정이 필요한 파일을 확인하고, `docs/answer/answer12.md`에 수정해야 할 파일별 **수정 후 전체 코드 전문**을 작성해주세요.

---

## 1. 작업 목표

현재 분석 결과 다음 문제가 확인되었습니다.

```text
DB schema: radio_recommendations.order_num
RadioMapper.xml: sort_order 사용
```

이 불일치 때문에 라디오 추천 저장 또는 라디오 상세 조회에서 SQL 오류가 발생할 가능성이 있습니다.

따라서 아래 작업을 수행해주세요.

1. `kpop_radio_schema.sql`에서 `radio_recommendations` 테이블 컬럼 확인
2. `RadioMapper.xml`에서 `sort_order` 사용 여부 확인
3. `radio_recommendations`를 사용하는 Java/Mapper/XML 전체 확인
4. 어떤 파일을 고쳐야 하는지 판단
5. 실제 파일은 수정하지 말고, 수정 후 전체 코드 전문을 `answer12.md`에 작성

---

## 2. 반드시 확인할 파일

아래 파일을 먼저 확인해주세요.

```text
src/main/resources/sql/kpop_radio_schema.sql
src/main/resources/mapper/radio/RadioMapper.xml
src/main/java/com/ssafy/revibek/radio/mapper/RadioMapper.java
src/main/java/com/ssafy/revibek/radio/dto/RadioSongResponseDto.java
src/main/java/com/ssafy/revibek/radio/dto/RecommendedSongResponseDto.java
src/main/java/com/ssafy/revibek/radio/service/RadioService.java
```

필요하다면 `radio_recommendations`를 사용하는 전체 파일을 검색해서 함께 확인해주세요.

검색 키워드:

```text
sort_order
order_num
radio_recommendations
orderNum
sortOrder
```

---

## 3. 수정 방향

DB 스키마인 `kpop_radio_schema.sql`을 기준으로 판단해주세요.

권장 방향:

```text
DB 스키마의 radio_recommendations.order_num을 유지하고,
RadioMapper.xml에서 sort_order를 order_num으로 바꾸는 방향
```

즉, DB 스키마 파일을 바꾸는 것이 아니라
Mapper SQL을 DB 스키마에 맞추는 방식으로 수정안을 제안해주세요.

---

## 4. 확인해야 할 항목

아래 항목을 반드시 확인해주세요.

1. `radio_recommendations` 테이블에 실제 컬럼이 `order_num`인지 확인
2. `RadioMapper.xml`에서 `sort_order`를 사용하는 SQL이 있는지 확인
3. `INSERT INTO radio_recommendations` 구문에서 잘못된 컬럼명이 있는지 확인
4. `SELECT` 구문에서 잘못된 컬럼명이 있는지 확인
5. `ORDER BY` 구문에서 잘못된 컬럼명이 있는지 확인
6. DTO 필드명이 `orderNum`인지 확인
7. MyBatis alias가 필요한지 확인
8. Mapper interface의 메서드명과 XML id가 일치하는지 확인
9. RadioService에서 추천곡 순서값이 정상 전달되는지 확인
10. 라디오 상세 조회 시 추천곡 순서가 `order_num` 기준으로 정렬되는지 확인

---

## 5. 코드 수정 금지

중요합니다.

이번 작업에서는 실제 프로젝트 파일을 수정하지 마세요.

아래 행동은 하지 마세요.

```text
직접 RadioMapper.xml 수정 금지
직접 Java 파일 수정 금지
직접 SQL 파일 수정 금지
직접 application.properties 수정 금지
```

대신 `docs/answer/answer12.md`에 아래 내용을 작성해주세요.

```text
1. 어떤 파일을 고쳐야 하는지
2. 왜 고쳐야 하는지
3. 어떤 부분이 문제인지
4. 수정 후 전체 코드는 어떻게 되어야 하는지
```

---

## 6. 수정 후 전체 코드 전문 요청

수정이 필요한 파일은 파일별로 **수정 후 전체 코드 전문**을 작성해주세요.

부분 코드나 diff만 작성하지 마세요.

아래 형식으로 작성해주세요.

````text
## 수정 후 전체 코드 전문

### 1. src/main/resources/mapper/radio/RadioMapper.xml

```xml
여기에 수정 후 RadioMapper.xml 전체 코드 전문
````

### 2. src/main/java/com/ssafy/revibek/radio/mapper/RadioMapper.java

```java
수정이 필요하다면 전체 코드 전문
수정이 필요 없다면 "수정 필요 없음"이라고 작성
```

### 3. src/main/java/com/ssafy/revibek/radio/dto/RadioSongResponseDto.java

```java
수정이 필요하다면 전체 코드 전문
수정이 필요 없다면 "수정 필요 없음"이라고 작성
```

### 4. 기타 수정이 필요한 파일

```java
수정이 필요한 경우 전체 코드 전문
```

````

수정이 필요 없는 파일은 전체 코드를 넣지 않아도 됩니다.  
하지만 `RadioMapper.xml`은 문제가 확인되면 반드시 수정 후 전체 코드 전문을 포함해주세요.

---

## 7. 테스트 명령어 제안

실제 수정은 하지 않지만, 내가 코드를 반영한 뒤 확인할 수 있도록 테스트 명령어를 작성해주세요.

가능하면 아래 명령 기준으로 작성해주세요.

```powershell
.\mvnw.cmd clean compile
.\mvnw.cmd test
````

또한 DB 기준 확인 SQL도 작성해주세요.

예시:

```sql
DESC radio_recommendations;
SELECT * FROM radio_recommendations ORDER BY order_num;
```

---

## 8. 결과 저장

결과는 반드시 아래 파일에 저장해주세요.

```text
docs/answer/answer12.md
```

UTF-8 인코딩으로 저장해주세요.

---

## 9. answer12.md 형식

아래 형식으로 정리해주세요.

```text
# RevibeK radio_recommendations order_num / sort_order 불일치 확인 및 수정 코드 제안 결과

## 1. 전체 결론

## 2. 확인한 DB 스키마
- radio_recommendations 컬럼 구조
- order_num 존재 여부
- sort_order 존재 여부

## 3. 확인한 Mapper SQL
- sort_order 사용 위치
- order_num 사용 위치
- INSERT 문제 여부
- SELECT 문제 여부
- ORDER BY 문제 여부

## 4. 문제 원인

## 5. 수정해야 할 파일 목록

## 6. 수정 방향

## 7. 수정 후 전체 코드 전문
수정이 필요한 파일별로 전체 코드 전문 작성

## 8. 수정이 필요 없는 파일

## 9. 내가 직접 수정할 때 적용 순서

## 10. 수정 후 실행할 테스트 명령어

## 11. DB 확인 SQL

## 12. 남은 주의사항

## 13. 최종 판단
```

---

## 10. 최종 지시

이번 작업의 핵심은 아래 하나입니다.

```text
radio_recommendations.order_num 과 RadioMapper.xml의 sort_order 불일치 확인
```

실제 코드는 수정하지 말고, 내가 직접 고칠 수 있도록
수정이 필요한 파일의 **수정 후 전체 코드 전문**을 `docs/answer/answer12.md`에 작성해주세요.

결과는 반드시 `docs/answer/answer12.md` 파일로 UTF-8 인코딩으로 저장해주세요.
