# RevibeK radio_recommendations order_num / sort_order 불일치 확인 및 수정 코드 제안 결과

## 1. 전체 결론

- `src/main/resources/sql/kpop_radio_schema.sql`의 `CREATE TABLE radio_recommendations` 구문은 처음에 `order_num` 컬럼을 생성합니다.
- 그러나 같은 파일 후반부의 `ALTER TABLE radio_recommendations` 구문이 `order_num`을 `sort_order`로 변경합니다.
- 따라서 **`kpop_radio_schema.sql`을 처음부터 끝까지 정상 실행한 최종 스키마는 `sort_order` 기준**이며, 현재 `RadioMapper.xml`의 `sort_order` 사용과 일치합니다.
- 반면 `src/main/resources/sql/dump/kpop_radio_dump.sql`의 `radio_recommendations` 테이블은 `order_num` 기준입니다. 이 dump로 실제 DB를 생성했다면 현재 `RadioMapper.xml` 실행 시 `Unknown column 'sort_order'` 오류가 발생합니다.
- 프로젝트 설정에는 Spring SQL 자동 초기화 파일 지정이나 Flyway/Liquibase 마이그레이션 설정이 없습니다. 소스만으로는 현재 로컬 DB가 어느 SQL 파일/단계로 생성되었는지 확정할 수 없으므로, 실제 수정 전 반드시 `DESC radio_recommendations;`로 확인해야 합니다.
- 실제 DB에 `order_num`만 존재하고 `sort_order`가 없다면, 요청한 방향대로 **`RadioMapper.xml` 한 파일만** 아래 전체 코드로 수정하면 됩니다.
- 실제 DB에 `sort_order`가 존재한다면 현재 `RadioMapper.xml`은 이미 올바르므로 수정하면 안 됩니다.

## 2. 확인한 DB 스키마

### `kpop_radio_schema.sql`의 최초 테이블 생성 구조

```sql
CREATE TABLE radio_recommendations (
  id          CHAR(36)    NOT NULL DEFAULT (UUID()),
  session_id  CHAR(36)    NOT NULL,
  song_id     CHAR(36)    NOT NULL,
  order_num   TINYINT     NOT NULL DEFAULT 1 COMMENT '추천 순서',
  reason      VARCHAR(200) NULL    COMMENT '추천 이유 (DJ 멘트 생성에 재활용)',
  PRIMARY KEY (id),
  FOREIGN KEY (session_id) REFERENCES radio_sessions(id) ON DELETE CASCADE,
  FOREIGN KEY (song_id)    REFERENCES songs(id) ON DELETE CASCADE,
  INDEX idx_session (session_id, order_num)
);
```

### 같은 파일 후반부의 변경 구문

```sql
ALTER TABLE radio_recommendations
  CHANGE COLUMN order_num sort_order TINYINT NOT NULL DEFAULT 1,
  ADD COLUMN title VARCHAR(200) NULL AFTER song_id,
  ADD COLUMN artist VARCHAR(100) NULL AFTER title,
  ADD COLUMN youtube_url VARCHAR(300) NULL AFTER artist,
  ADD COLUMN thumbnail_url VARCHAR(500) NULL AFTER youtube_url,
  ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'recommended'
    AFTER thumbnail_url,
  ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;

DROP INDEX idx_session ON radio_recommendations;

CREATE INDEX idx_session
  ON radio_recommendations(session_id, sort_order);
```

### 판정

| 확인 대상 | 판정 |
|---|---|
| 최초 `CREATE TABLE` 직후 | `order_num` 존재, `sort_order` 없음 |
| `kpop_radio_schema.sql` 전체 실행 후 | `sort_order` 존재, `order_num` 없음 |
| `kpop_radio_dump.sql` 기준 | `order_num` 존재, `sort_order` 없음 |
| 현재 실제 DB | 소스만으로 확정 불가, `DESC` 확인 필요 |

즉, "`kpop_radio_schema.sql` 기준 실제 컬럼은 `order_num`"이라고 단정할 수 없습니다. 해당 파일 전체 실행 결과를 기준으로 하면 실제 최종 컬럼은 `sort_order`입니다.

## 3. 확인한 Mapper SQL

현재 `src/main/resources/mapper/radio/RadioMapper.xml`에서 `sort_order`는 세 곳에 사용됩니다.

1. `insertRecommendation`
   - `INSERT INTO radio_recommendations`의 대상 컬럼으로 `sort_order` 사용
   - Java 파라미터 `#{orderNum}` 값을 저장
2. `selectRecommendationBySessionId`
   - `rr.sort_order AS order_num`으로 조회
3. `selectRecommendationBySessionId`
   - `ORDER BY rr.sort_order`로 추천 순서 정렬

### 문제 여부

| SQL 항목 | 최종 `kpop_radio_schema.sql` 기준 | 실제 DB가 `order_num` 기준일 때 |
|---|---|---|
| INSERT의 `sort_order` | 정상 | 오류 |
| SELECT의 `rr.sort_order` | 정상 | 오류 |
| ORDER BY의 `rr.sort_order` | 정상 | 오류 |
| `#{orderNum}` 파라미터 | 정상 | 정상 |

`order_num`은 현재 Mapper에서 조회 alias로만 사용됩니다. `mybatis.configuration.map-underscore-to-camel-case=true` 설정에 따라 `order_num`은 `RadioResponseDto.RadioSongDto.orderNum`에 정상 매핑됩니다.

## 4. 문제 원인

문제의 직접 원인은 Java 필드명이 아니라 **실제 DB를 생성한 SQL 경로에 따라 컬럼명이 달라지는 스키마 기준 불일치**입니다.

- `kpop_radio_schema.sql` 전체 실행 DB: `sort_order`
- `kpop_radio_dump.sql` 실행 DB 또는 후반 ALTER 미실행 DB: `order_num`
- 현재 Mapper: `sort_order`

따라서 실제 DB가 `order_num`인데 Mapper가 `sort_order`를 사용하면 추천곡 저장, 조회, 정렬이 모두 실패합니다. 반대로 최종 스키마가 `sort_order`인데 Mapper를 `order_num`으로 바꾸면 동일한 오류가 반대 방향으로 발생합니다.

## 5. 수정해야 할 파일 목록

### 실제 DB에 `order_num`만 존재하는 경우

| 파일 | 수정 여부 | 이유 |
|---|---|---|
| `src/main/resources/mapper/radio/RadioMapper.xml` | 수정 필요 | INSERT, SELECT, ORDER BY의 실제 컬럼을 `order_num`으로 맞춰야 함 |
| `src/main/resources/sql/kpop_radio_schema.sql` | 수정하지 않음 | 요청상 SQL 파일 수정 금지. 단, 전체 실행 시 다시 `sort_order`가 된다는 주의 필요 |
| `src/main/java/com/ssafy/revibek/radio/mapper/RadioMapper.java` | 수정 필요 없음 | `@Param("orderNum")`과 XML의 `#{orderNum}`이 일치 |
| `src/main/java/com/ssafy/revibek/radio/dto/RadioResponseDto.java` | 수정 필요 없음 | 실제 조회 DTO의 필드가 `orderNum`이며 alias/camel-case 매핑 가능 |
| `src/main/java/com/ssafy/revibek/radio/dto/RadioSongResponseDto.java` | 수정 필요 없음 | `sortOrder` 필드가 있으나 현재 해당 Mapper 조회 흐름에서 사용되지 않음 |
| `src/main/java/com/ssafy/revibek/radio/dto/RecommendedSongResponseDto.java` | 수정 필요 없음 | 추천 저장 순서 필드를 보유하지 않으며 Service가 반복 인덱스로 순서를 전달 |
| `src/main/java/com/ssafy/revibek/radio/service/RadioService.java` | 수정 필요 없음 | `i + 1`을 `insertRecommendation`의 `orderNum`으로 정상 전달 |
| `src/main/resources/application.properties` | 수정 필요 없음 | underscore-to-camel-case 설정이 이미 활성화됨 |

### 실제 DB에 `sort_order`가 존재하는 경우

수정해야 할 파일이 없습니다. 현재 Mapper와 최종 스키마가 이미 일치합니다.

## 6. 수정 방향

요청한 방향은 실제 DB의 `radio_recommendations` 컬럼이 `order_num`인 것이 확인된 경우에만 적용합니다.

1. `RadioMapper.xml`의 INSERT 대상 컬럼 `sort_order`를 `order_num`으로 변경
2. 조회 컬럼 `rr.sort_order AS order_num`을 `rr.order_num AS order_num`으로 변경
3. 정렬 컬럼 `ORDER BY rr.sort_order`를 `ORDER BY rr.order_num`으로 변경
4. Java Mapper의 `@Param("orderNum")`, XML의 `#{orderNum}`, DTO의 `orderNum`은 유지

MyBatis alias는 엄밀히 말해 필수는 아닙니다. `map-underscore-to-camel-case=true`이므로 `rr.order_num`만 조회해도 `orderNum`에 매핑됩니다. 다만 매핑 의도를 명확히 하고 설정 변경에 대한 영향을 줄이기 위해 `rr.order_num AS order_num`을 유지하는 안을 제안합니다.

## 7. 수정 후 전체 코드 전문

아래 코드는 **실제 DB의 `DESC radio_recommendations` 결과에 `order_num`만 있고 `sort_order`가 없을 때 적용할 조건부 수정안**입니다.

### 1. `src/main/resources/mapper/radio/RadioMapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.ssafy.revibek.radio.mapper.RadioMapper">

    <!-- 세션 저장 -->
    <insert id="insertRadioSession">
        INSERT INTO radio_sessions (id, user_id, mood, story)
        VALUES (#{id}, #{userId}, #{mood}, #{story})
    </insert>

    <insert id="insertRadioSessionWithMent">
        INSERT INTO radio_sessions (
        id,
        user_id,
        mood,
        story,
        era,
        genre,
        situation,
        desired_mood,
        video_type,
        preferred_artist,
        excluded_keywords,
        recommendation_source,
        dj_ment
        ) VALUES (
        #{id},
        #{userId},
        #{mood},
        #{story},
        #{era},
        #{genre},
        #{situation},
        #{desiredMood},
        #{videoType},
        #{preferredArtist},
        #{excludedKeywords},
        #{recommendationSource},
        #{djMent}
        )
    </insert>

    <update id="updateRadioSessionDjMent">
        UPDATE radio_sessions
        SET dj_ment = #{djMent}
        WHERE id = #{id}
        AND user_id = #{userId}
    </update>

    <!-- 세션 조회 -->
    <select id="selectRadioSessionByIdAndUserId"
            resultType="com.ssafy.revibek.radio.dto.RadioResponseDto">
        SELECT
        id,
        mood,
        story,
        era,
        genre,
        situation,
        desired_mood,
        video_type,
        preferred_artist,
        excluded_keywords,
        recommendation_source,
        dj_ment,
        comfort_text,
        novel_excerpt,
        created_at
        FROM radio_sessions
        WHERE id = #{id}
        AND user_id = #{userId}
    </select>

    <!-- 유저의 세션 목록 -->
    <select id="selectRadioSessionByUserId" parameterType="string"
            resultType="com.ssafy.revibek.radio.dto.RadioResponseDto">
        SELECT
        id,
        mood,
        story,
        era,
        genre,
        situation,
        desired_mood,
        video_type,
        preferred_artist,
        excluded_keywords,
        recommendation_source,
        dj_ment,
        comfort_text,
        novel_excerpt,
        created_at
        FROM radio_sessions
        WHERE user_id = #{userId}
        ORDER BY created_at DESC
    </select>

    <!-- 추천곡 저장 -->
    <insert id="insertRecommendation">
        INSERT INTO radio_recommendations (
        session_id,
        song_id,
        order_num,
        reason
        )
        VALUES (
        #{sessionId},
        #{songId},
        #{orderNum},
        #{reason}
        )
    </insert>

    <!-- 세션별 추천곡 조회 -->
    <select id="selectRecommendationBySessionId" parameterType="string"
            resultType="com.ssafy.revibek.radio.dto.RadioResponseDto$RadioSongDto">
        SELECT
        rr.song_id,
        s.title,
        s.artist,
        rr.order_num AS order_num,
        rr.reason
        FROM radio_recommendations rr
        JOIN songs s ON s.id = rr.song_id
        WHERE rr.session_id = #{sessionId}
        ORDER BY rr.order_num
    </select>

</mapper>
```

### 2. `src/main/java/com/ssafy/revibek/radio/mapper/RadioMapper.java`

수정 필요 없음.

### 3. `src/main/java/com/ssafy/revibek/radio/dto/RadioSongResponseDto.java`

수정 필요 없음.

### 4. 기타 수정이 필요한 파일

없음.

## 8. 수정이 필요 없는 파일

### `RadioMapper.java`

- XML namespace와 Java interface 전체 이름이 일치합니다.
- Java 메서드명과 XML statement id가 모두 일치합니다.
  - `insertRadioSession`
  - `insertRadioSessionWithMent`
  - `updateRadioSessionDjMent`
  - `selectRadioSessionByIdAndUserId`
  - `selectRadioSessionByUserId`
  - `insertRecommendation`
  - `selectRecommendationBySessionId`
- `@Param("orderNum")`과 XML의 `#{orderNum}`이 일치합니다.

### `RadioResponseDto.java`

- 라디오 상세 조회에 실제로 사용되는 중첩 DTO `RadioResponseDto.RadioSongDto`의 순서 필드는 `orderNum`입니다.
- SQL의 `order_num`은 underscore-to-camel-case 설정으로 `orderNum`에 정상 매핑됩니다.

### `RadioSongResponseDto.java`

- 순서 필드는 `sortOrder`입니다.
- 다만 현재 `RadioMapper.xml`, `RadioMapper.java`, `RadioService.getSession`, `RadioService.getSessionByUser` 조회 흐름에서는 이 DTO를 사용하지 않습니다.
- 이번 `radio_recommendations` 저장/상세 조회 오류를 해결하기 위해 변경할 필요는 없습니다.

### `RecommendedSongResponseDto.java`

- `orderNum` 또는 `sortOrder` 필드가 없습니다.
- 생성 응답용 추천곡 정보이며, 저장 순서는 `RadioService`가 목록 인덱스로 계산합니다.

### `RadioService.java`

- 추천곡 목록 순서대로 반복하면서 `i + 1`을 `radioMapper.insertRecommendation(..., orderNum, ...)`에 전달합니다.
- 첫 추천곡은 1, 다음 추천곡은 2와 같은 방식으로 정상 전달됩니다.
- 상세 조회와 사용자별 세션 조회 모두 `selectRecommendationBySessionId`를 호출하므로 XML의 `ORDER BY`가 최종 응답 순서를 결정합니다.

### `application.properties`

- `mybatis.configuration.map-underscore-to-camel-case=true`가 이미 설정되어 있습니다.

## 9. 내가 직접 수정할 때 적용 순서

1. 먼저 실제 DB에서 아래 쿼리를 실행합니다.

   ```sql
   DESC radio_recommendations;
   ```

2. 결과에 `order_num`만 있고 `sort_order`가 없으면 이 문서의 `RadioMapper.xml` 전체 코드로 교체합니다.
3. 결과에 `sort_order`가 있으면 현재 Mapper를 유지하고 수정하지 않습니다.
4. 수정 후 Maven compile/test를 실행합니다.
5. 라디오 생성 API를 호출해 추천곡 저장을 확인합니다.
6. 라디오 상세 조회 API를 호출해 추천곡이 순서대로 반환되는지 확인합니다.

## 10. 수정 후 실행할 테스트 명령어

```powershell
.\mvnw.cmd clean compile
.\mvnw.cmd test
```

추가 검색 확인:

```powershell
rg -n "sort_order|order_num|radio_recommendations|orderNum|sortOrder" src/main
```

## 11. DB 확인 SQL

### 실제 컬럼과 인덱스 확인

```sql
DESC radio_recommendations;
SHOW COLUMNS FROM radio_recommendations;
SHOW INDEX FROM radio_recommendations;
```

### `order_num` 기준 DB에 조건부 수정안을 적용한 후 확인

```sql
SELECT
    id,
    session_id,
    song_id,
    order_num,
    reason
FROM radio_recommendations
ORDER BY session_id, order_num;
```

### 특정 세션 상세 조회 순서 확인

```sql
SELECT
    rr.song_id,
    s.title,
    s.artist,
    rr.order_num,
    rr.reason
FROM radio_recommendations rr
JOIN songs s ON s.id = rr.song_id
WHERE rr.session_id = '확인할-세션-ID'
ORDER BY rr.order_num;
```

### 컬럼 존재 여부를 안전하게 판별하는 쿼리

```sql
SELECT
    column_name
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'radio_recommendations'
  AND column_name IN ('order_num', 'sort_order')
ORDER BY column_name;
```

## 12. 남은 주의사항

1. `kpop_radio_schema.sql`은 중간 상태와 최종 상태의 컬럼명이 다릅니다. 파일을 다시 전체 실행하면 `order_num`이 `sort_order`로 변경되므로, Mapper를 `order_num`으로 수정한 상태와 다시 불일치할 수 있습니다.
2. `kpop_radio_dump.sql`과 `kpop_radio_schema.sql`의 최종 구조가 서로 다릅니다. 장기적으로는 둘 중 하나를 단일 기준 스키마로 정해야 합니다.
3. `bin/src/main/resources` 아래에는 오래된 복사본이 있고 `order_num`을 사용하지만, Maven의 일반 소스 경로는 `src/main/resources`입니다. `bin` 파일은 수정 대상이 아닙니다.
4. `playlist_songs.order_num`은 별도 테이블의 정상 컬럼이므로 이번 변경 대상이 아닙니다.
5. 현재 Mapper는 `radio_recommendations`의 추가 스냅샷 컬럼인 `title`, `artist`, `youtube_url`, `thumbnail_url`, `source`를 INSERT하지 않습니다. 이 컬럼들은 최종 스키마에서 nullable 또는 기본값이 있으므로 이번 순서 컬럼 문제와는 별개입니다.
6. 실제 DB 접속 결과를 확인하지 않은 채 Mapper만 변경하면 반대 방향의 `Unknown column` 오류를 만들 수 있습니다.

## 13. 최종 판단

- **`kpop_radio_schema.sql` 전체 실행 결과를 기준으로 판단하면 현재 `RadioMapper.xml`은 이미 `sort_order`와 일치하며 수정 필요가 없습니다.**
- **실제 로컬 DB가 `kpop_radio_dump.sql` 또는 ALTER 미실행 상태여서 `order_num`만 보유한 경우에는 `RadioMapper.xml`만 이 문서의 전체 코드로 수정해야 합니다.**
- Java Mapper의 메서드/파라미터, 활성 조회 DTO의 `orderNum`, MyBatis camel-case 설정, `RadioService`의 `i + 1` 순서 전달은 정상입니다.
- 이번 작업에서는 요청에 따라 실제 프로젝트 소스, SQL, 설정 파일은 수정하지 않았으며, 조건부 수정 제안만 작성했습니다.
