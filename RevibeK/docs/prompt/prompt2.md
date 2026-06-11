# 라디오 API + 사용자 선택 곡 리스트 + 플레이리스트 저장 구조 전체 코드 요청

현재 RevibeK 프로젝트에서 라디오 생성 API를 개선하려고 합니다.

기존에는 라디오 API가 사용자의 mood, story, era, genre 등을 받아서 추천곡을 생성하는 구조였습니다.

이제는 아래 구조로 확장하고 싶습니다.

---

# 목표

사용자가 라디오를 생성할 때 아래 정보를 입력합니다.

1. 라디오 사연
2. 현재 감정
3. 듣고 싶은 분위기
4. 선호 세대
5. 장르
6. 영상 타입
7. 사용자가 직접 선택한 곡 리스트

그리고 백엔드는 이 정보를 기반으로:

1. 라디오 세션 생성
2. 라디오 사연 저장
3. 선택한 곡 리스트 저장
4. 필요하면 추천곡 추가
5. 최종 라디오 곡 목록 생성
6. 생성된 곡 목록을 사용자 플레이리스트로 저장

하는 구조로 구현해주세요.

---

# 핵심 질문

현재 코드 기준으로 아래가 가능한지 먼저 분석해주세요.

1. 라디오 API에서 사용자가 직접 선택한 곡 리스트를 받을 수 있는가?
2. RadioCreateRequestDto에 곡 리스트 필드를 추가할 수 있는가?
3. radio_sessions 테이블에 라디오 사연과 입력값을 저장하고 있는가?
4. radio_recommendations 또는 유사 테이블에 곡 목록을 저장하고 있는가?
5. playlist API가 이미 구현되어 있다면 라디오 생성 결과를 플레이리스트로 저장할 수 있는가?
6. 사용자가 직접 고른 곡 리스트와 추천곡 리스트를 합쳐서 하나의 라디오 플레이리스트로 만들 수 있는가?
7. 기존 플레이리스트 API와 충돌 없이 연결 가능한가?

---

# 원하는 최종 흐름

아래 흐름으로 구현해주세요.

```text
POST /api/radio
→ X-USER-ID 헤더 또는 userId 확인
→ 사용자의 라디오 입력값 수신
→ story / mood / desiredMood / era / genre / videoType 수신
→ 사용자가 선택한 곡 리스트 selectedSongs 수신
→ user_preferences 조회
→ selectedSongs를 우선 라디오 곡 목록에 포함
→ 부족하면 mood / desiredMood 기반 추천곡 추가
→ radio_sessions 저장
→ radio_recommendations 저장
→ playlists 테이블에 라디오용 플레이리스트 생성
→ playlist_items에 곡 목록 저장
→ 최종 응답 반환
```

---

# API 설계

## 라디오 생성 API

```http
POST /api/radio
X-USER-ID: 사용자ID
Content-Type: application/json
```

## 요청 Body 예시

```json
{
  "title": "오늘 나를 위로하는 2세대 라디오",
  "mood": "지친",
  "situation": "프로젝트 마감 때문에 피곤함",
  "desiredMood": "위로",
  "story": "요즘 관통 프로젝트 때문에 체력적으로 힘든데 다시 힘내고 싶어요.",
  "era": "2세대",
  "genre": "댄스",
  "videoType": "라이브",
  "excludedKeywords": "remix",
  "saveAsPlaylist": true,
  "selectedSongs": [
    {
      "songId": "song-uuid-1",
      "title": "다시 만난 세계",
      "artist": "소녀시대",
      "youtubeUrl": "https://www.youtube.com/watch?v=...",
      "thumbnailUrl": "https://...",
      "generation": "2세대",
      "genre": "댄스",
      "mood": "위로"
    },
    {
      "songId": "song-uuid-2",
      "title": "하루하루",
      "artist": "BIGBANG",
      "youtubeUrl": "https://www.youtube.com/watch?v=...",
      "thumbnailUrl": "https://...",
      "generation": "2세대",
      "genre": "발라드",
      "mood": "감성"
    }
  ]
}
```

---

# 응답 Body 예시

```json
{
  "success": true,
  "message": "라디오가 생성되고 플레이리스트로 저장되었습니다.",
  "data": {
    "radioSessionId": "radio-session-uuid",
    "playlistId": "playlist-uuid",
    "title": "오늘 나를 위로하는 2세대 라디오",
    "story": "요즘 관통 프로젝트 때문에 체력적으로 힘든데 다시 힘내고 싶어요.",
    "djComment": "오늘은 지친 마음을 위로해줄 2세대 K-POP 라디오를 준비했어요.",
    "songs": [
      {
        "songId": "song-uuid-1",
        "title": "다시 만난 세계",
        "artist": "소녀시대",
        "youtubeUrl": "https://www.youtube.com/watch?v=...",
        "thumbnailUrl": "https://...",
        "source": "selected"
      },
      {
        "songId": "song-uuid-3",
        "title": "추천곡 제목",
        "artist": "추천 아티스트",
        "youtubeUrl": "https://www.youtube.com/watch?v=...",
        "thumbnailUrl": "https://...",
        "source": "recommended"
      }
    ]
  }
}
```

---

# 중요한 설계 기준

## 1. selectedSongs 우선

사용자가 직접 선택한 곡 리스트가 있으면 그 곡들을 최우선으로 라디오 곡 목록에 포함해주세요.

```text
selectedSongs
→ 추천곡보다 우선순위 높음
```

## 2. 추천곡은 부족할 때만 추가

selectedSongs가 충분하지 않으면 mood / desiredMood / era / genre 기반 추천곡을 추가해주세요.

예시:

```text
목표 곡 수: 10곡
사용자 선택 곡: 4곡
추천곡 추가: 6곡
```

## 3. 중복 곡 제거

selectedSongs와 추천곡이 중복되면 중복 제거해주세요.

기준:

* songId
* 또는 youtubeUrl
* 또는 title + artist

## 4. saveAsPlaylist 옵션

요청값에 아래 필드를 추가해주세요.

```java
private Boolean saveAsPlaylist;
```

* true이면 생성된 라디오 곡 목록을 사용자 플레이리스트로 저장
* false이면 라디오 세션만 생성하고 플레이리스트 저장은 하지 않음
* null이면 기본값 true로 처리

## 5. playlist title

요청에 title이 있으면 그 제목으로 플레이리스트를 생성하고, 없으면 자동 생성해주세요.

자동 제목 예시:

```text
지친 날을 위한 2세대 K-POP 라디오
위로가 필요한 밤의 RevibeK 라디오
```

---

# 필요한 DB 구조 점검

현재 playlist API가 이미 구현되어 있으므로 아래 테이블이 있는지 확인해주세요.

* playlists
* playlist_items
* radio_sessions
* radio_recommendations
* songs

없거나 부족한 컬럼이 있으면 schema 전체 수정본을 제공해주세요.

## radio_sessions에 필요한 컬럼

* id
* user_id
* title
* mood
* situation
* desired_mood
* story
* era
* genre
* video_type
* playlist_id
* dj_comment
* created_at

## radio_recommendations에 필요한 컬럼

* id
* radio_session_id
* song_id
* title
* artist
* youtube_url
* thumbnail_url
* source

    * selected
    * recommended
    * fallback
* sort_order
* created_at

## playlists에 필요한 컬럼

* id
* user_id
* title
* description
* created_at
* updated_at

## playlist_items에 필요한 컬럼

* id
* playlist_id
* song_id
* title
* artist
* youtube_url
* thumbnail_url
* sort_order
* created_at

---

# 필요한 DTO 전체 코드

아래 DTO를 현재 프로젝트 구조에 맞춰 생성 또는 수정해주세요.

```java
// RevibeK/src/main/java/com/ssafy/revibek/radio/dto/RadioCreateRequestDto.java
전체 코드
```

필수 필드:

```java
private String title;
private String mood;
private String situation;
private String desiredMood;
private String story;
private String era;
private String genre;
private String videoType;
private String excludedKeywords;
private Boolean saveAsPlaylist;
private List<RadioSelectedSongDto> selectedSongs;
```

---

```java
// RevibeK/src/main/java/com/ssafy/revibek/radio/dto/RadioSelectedSongDto.java
전체 코드
```

필수 필드:

```java
private String songId;
private String title;
private String artist;
private String youtubeUrl;
private String thumbnailUrl;
private String generation;
private String genre;
private String mood;
```

---

```java
// RevibeK/src/main/java/com/ssafy/revibek/radio/dto/RadioCreateResponseDto.java
전체 코드
```

필수 필드:

```java
private String radioSessionId;
private String playlistId;
private String title;
private String story;
private String djComment;
private List<RadioSongResponseDto> songs;
```

---

```java
// RevibeK/src/main/java/com/ssafy/revibek/radio/dto/RadioSongResponseDto.java
전체 코드
```

필수 필드:

```java
private String songId;
private String title;
private String artist;
private String youtubeUrl;
private String thumbnailUrl;
private String source;
private Integer sortOrder;
```

---

# 필요한 Service 로직

RadioService를 아래 흐름으로 수정해주세요.

```text
1. userId 확인
2. RadioCreateRequestDto 수신
3. selectedSongs 검증
4. user_preferences 조회
5. selectedSongs를 RadioSongResponseDto로 변환
6. 부족한 곡 수 계산
7. 부족한 만큼 추천곡 조회
8. selectedSongs + recommendedSongs 병합
9. 중복 제거
10. sortOrder 부여
11. DJ 멘트 생성
12. radio_sessions insert
13. radio_recommendations batch insert
14. saveAsPlaylist가 true이면 playlists insert
15. playlist_items batch insert
16. radio_sessions에 playlist_id 업데이트
17. 최종 응답 반환
```

---

# 추천곡 fallback 순서

추천곡이 부족할 경우 아래 순서로 조회해주세요.

```text
1. desiredMood + era + genre
2. desiredMood + era
3. desiredMood + genre
4. desiredMood
5. mood + era + genre
6. mood + era
7. mood + genre
8. mood
9. era + genre
10. era
11. genre
12. user preference
13. score fallback
```

---

# 필요한 Mapper 전체 코드

아래 파일들을 전체 코드로 작성해주세요.

```java
// RevibeK/src/main/java/com/ssafy/revibek/radio/mapper/RadioMapper.java
전체 코드
```

```xml
<!-- RevibeK/src/main/resources/mapper/radio/RadioMapper.xml -->
        전체 코드
```

```java
// RevibeK/src/main/java/com/ssafy/revibek/song/mapper/SongMapper.java
전체 코드
```

```xml
<!-- RevibeK/src/main/resources/mapper/song/SongMapper.xml -->
        전체 코드
```

플레이리스트 저장을 위해 기존 playlist mapper 수정이 필요하면 아래도 전체 코드로 작성해주세요.

```java
// RevibeK/src/main/java/com/ssafy/revibek/playlist/mapper/PlaylistMapper.java
전체 코드
```

```xml
<!-- RevibeK/src/main/resources/mapper/playlist/PlaylistMapper.xml -->
        전체 코드
```

---

# 필요한 Controller 전체 코드

아래 파일을 전체 코드로 작성해주세요.

```java
// RevibeK/src/main/java/com/ssafy/revibek/radio/controller/RadioController.java
전체 코드
```

요구사항:

* `POST /api/radio`
* `X-USER-ID` 헤더를 받을 수 있게 처리
* 기존 방식의 userId 처리 방식이 있으면 기존 방식 우선
* 요청 Body는 RadioCreateRequestDto 사용
* 응답은 ApiResponseDto 또는 기존 공통 응답 구조 사용

---

# PlaylistService 연동

현재 playlist API가 이미 구현되어 있다면 RadioService에서 직접 playlist mapper를 호출하기보다, 가능하면 PlaylistService를 재사용해주세요.

단, 기존 PlaylistService가 라디오 생성용 batch insert를 지원하지 않으면 아래 메서드를 추가해주세요.

```java
createPlaylistFromRadio(String userId, String title, String description, List<RadioSongResponseDto> songs)
```

필요하면 전체 코드로 작성해주세요.

```java
// RevibeK/src/main/java/com/ssafy/revibek/playlist/service/PlaylistService.java
전체 코드
```

---

# DB schema 전체 코드

필요하면 아래 파일을 전체 코드로 수정해주세요.

```sql
-- RevibeK/src/main/resources/sql/kpop_radio_schema.sql
전체 코드
```

주의:

* 기존 playlist API 커밋과 충돌하지 않게 작성
* 기존 테이블 삭제 금지
* 필요한 컬럼은 ALTER 또는 CREATE TABLE 기준으로 안전하게 제안
* 전체 초기화용 schema라면 전체 CREATE TABLE 코드 제공
* 운영 DB 수정용이면 ALTER TABLE 코드도 별도로 제공

---

# 테스트 예시 작성

아래 테스트 예시를 작성해주세요.

## 1. selectedSongs만으로 라디오 생성

```http
POST /api/radio
X-USER-ID: 사용자ID
Content-Type: application/json
```

## 2. selectedSongs + 추천곡 자동 보충

```http
POST /api/radio
X-USER-ID: 사용자ID
Content-Type: application/json
```

## 3. saveAsPlaylist false

```http
POST /api/radio
X-USER-ID: 사용자ID
Content-Type: application/json
```

## 4. 플레이리스트 저장 확인

```http
GET /api/playlists
X-USER-ID: 사용자ID
```

## 5. 라디오 세션 저장 확인

가능한 API가 있으면 제안해주세요.

---

# 최종 답변 순서

반드시 아래 순서로 답변해주세요.

1. 현재 구조에서 가능한지 판단
2. 구현 우선순위
3. 수정/생성 파일 목록
4. DB schema 수정안 전체 코드
5. RadioCreateRequestDto 전체 코드
6. RadioSelectedSongDto 전체 코드
7. RadioCreateResponseDto 전체 코드
8. RadioSongResponseDto 전체 코드
9. RadioController 전체 코드
10. RadioService 전체 코드
11. RadioMapper / RadioMapper.xml 전체 코드
12. SongMapper / SongMapper.xml 전체 코드
13. PlaylistService 연동 코드
14. 필요한 PlaylistMapper 수정 코드
15. API 테스트 예시
16. 구현 후 확인 체크리스트

---

# 최종 답변 규칙

다시 강조합니다.

* 코드 일부만 주지 마세요.
* 수정 또는 생성되는 파일은 반드시 전체 코드로 주세요.
* `...`, `생략`, `기존 코드 유지`, `나머지는 동일` 같은 표현을 쓰지 마세요.
* 실제 프로젝트 패키지명과 파일명을 기준으로 작성해주세요.
* 기존 playlist API와 충돌하지 않게 작성해주세요.
* selectedSongs는 추천곡보다 우선순위가 높아야 합니다.
* saveAsPlaylist가 true이면 라디오 결과가 사용자 플레이리스트로 저장되어야 합니다.
* saveAsPlaylist가 false이면 플레이리스트 저장 없이 라디오 세션만 생성되어야 합니다.
* 중복 곡은 반드시 제거해주세요.
* 프론트엔드에서 바로 사용할 수 있는 요청/응답 JSON 예시를 포함해주세요.
