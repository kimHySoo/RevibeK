## 1. 최종 판단

### 고쳐야 할 파일
- `src/main/resources/mapper/radio/RadioMapper.xml`
  - `INSERT` 대상 컬럼을 `order_num`에서 `sort_order`로 변경
  - 조회 컬럼을 `rr.sort_order AS order_num`으로 변경
  - 정렬 컬럼을 `rr.sort_order`로 변경

`application.properties`에 `mybatis.configuration.map-underscore-to-camel-case=true`가 설정되어 있으므로 `order_num` 별칭은 DTO의 `orderNum` 필드에 매핑됩니다.

### 수정 불필요 파일
- `src/main/java/com/ssafy/revibek/radio/mapper/RadioMapper.java`
  - `@Param("orderNum")`과 XML의 `#{orderNum}`이 일치합니다.
- `src/main/java/com/ssafy/revibek/radio/dto/RadioResponseDto.java`
  - 추천곡 순서 필드는 `orderNum`이며 SQL 별칭으로 정상 매핑됩니다.
- `src/main/java/com/ssafy/revibek/radio/service/RadioService.java`
  - `insertRecommendation(sessionId, songId, i + 1, reason)` 호출이 Mapper 인터페이스와 일치합니다.
- `src/main/resources/sql/kpop_radio_schema.sql`
  - 초기 테이블 생성 시 `order_num`을 사용하지만, 후반부 `ALTER TABLE`에서 최종 컬럼명을 `sort_order`로 변경합니다.
  - 최종 스키마 상태는 `sort_order`입니다.
  - `playlist_songs.order_num`은 별도 테이블 컬럼이므로 변경 대상이 아닙니다.

### DB 수정 필요 여부
- 최종 `kpop_radio_schema.sql`이 정상 적용된 DB라면 DB 수정은 필요 없습니다.
- 실제 프로젝트 파일은 수정하지 않았습니다.

## 2. 수정 대상 파일별 전체 코드

### `src/main/resources/mapper/radio/RadioMapper.xml`

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
            sort_order,
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
            rr.sort_order AS order_num,
            rr.reason
        FROM radio_recommendations rr
        JOIN songs s ON s.id = rr.song_id
        WHERE rr.session_id = #{sessionId}
        ORDER BY rr.sort_order
    </select>

</mapper>
```