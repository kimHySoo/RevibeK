# RevibeK 프론트엔드(Vue/HTML/JavaScript/CSS) 생성을 위한 백엔드 구조/API/JSON 분석 요청

현재 프로젝트는 `RevibeK` Maven Wrapper 기반 Spring Boot 백엔드 프로젝트입니다.

이번 작업의 목적은 **백엔드 Controller, DTO, Service, Mapper, DB schema를 분석한 뒤, Vue 기반 프론트엔드를 만들기 위한 API/JSON 구조와 화면 설계 자료를 정리하는 것**입니다.

프론트엔드는 **Vue + HTML + JavaScript + CSS** 기준으로 작업할 예정입니다.

이번 작업에서는 백엔드 코드를 수정하지 마세요.
분석만 수행하고, 프론트엔드 구현에 필요한 구조를 정리해주세요.

---

## 1. 작업 목표

RevibeK는 감정·상황 기반 K-POP AI 커버 라디오 큐레이션 서비스입니다.

프론트엔드에서 구현할 핵심 흐름은 아래와 같습니다.

```text
회원가입 / 로그인
→ 감정, 상황, 세대, 장르 입력
→ AI 커버 라디오 생성
→ AI DJ 멘트 확인
→ 추천 곡/영상 목록 확인
→ 좋아요
→ 플레이리스트 저장
→ 내 플레이리스트 조회
```

백엔드 구조를 분석해서 Vue 프론트엔드에서 필요한 다음 내용을 정리해주세요.

* API 경로
* 요청 JSON
* 응답 JSON
* Vue 화면 구조
* Vue 컴포넌트 구조
* JavaScript 상태 관리 구조
* CSS 디자인 방향
* mock data 구조
* axios API 호출 구조

---

## 2. 반드시 먼저 분석할 파일 구조

프로젝트 전체 파일 구조를 먼저 분석해주세요.

특히 아래 항목을 확인해주세요.

* 루트 구조
* `src/main/java/com/ssafy/revibek` 패키지 구조
* Controller 목록
* Service 목록
* DTO 목록
* Mapper interface 목록
* MyBatis XML mapper 위치
* `src/main/resources/sql/kpop_radio_schema.sql`
* `application.properties`
* `docs`
* `FE` 폴더 존재 여부
* 실제 프론트엔드 소스 존재 여부

분석 시 `bin` 폴더나 빌드 산출물은 실제 소스 기준으로 보지 말고, Maven 표준 소스 루트인 `src/main/java`, `src/main/resources`를 기준으로 판단해주세요.

---

## 3. 주요 도메인별 구조 분석

아래 도메인별로 Controller, Service, DTO, Mapper, DB 테이블 연결을 정리해주세요.

* auth
* user
* radio
* playlist
* like
* song
* usersong
* youtube
* analysis
* explore
* qdrant
* ai
* tts

각 도메인별로 아래 형식으로 정리해주세요.

```text
도메인명:
역할:
Controller:
Service:
DTO:
Mapper:
XML Mapper:
관련 DB 테이블:
프론트엔드에서 필요한 기능:
```

---

## 4. 실제 API 목록 정리

Controller 코드를 기준으로 실제 API 목록을 정리해주세요.

추측하지 말고 실제 코드에 존재하는 API만 작성해주세요.

아래 형식으로 작성해주세요.

```text
METHOD /api/...
인증 필요 여부:
Request Body:
Response Body:
관련 DTO:
관련 Service:
프론트엔드 화면:
주의사항:
```

반드시 아래 API 영역은 확인해주세요.

* 회원가입
* 이메일 인증
* 로그인
* 토큰 재발급
* 로그아웃
* 내 정보 조회
* 라디오 생성
* 라디오 상세 조회
* 좋아요 추가
* 좋아요 삭제
* 좋아요 상태 조회
* 플레이리스트 생성
* 플레이리스트 목록 조회
* 플레이리스트 상세 조회
* 플레이리스트에 곡 추가
* 곡 검색/조회
* YouTube/FastAPI/Qdrant fallback 관련 API

---

## 5. 인증/JWT 흐름 정리

Vue 프론트엔드에서 로그인 후 API를 호출할 수 있도록 인증 흐름을 정리해주세요.

아래 내용을 포함해주세요.

* 회원가입 요청 JSON
* 이메일 인증 요청 JSON
* 로그인 요청 JSON
* 로그인 응답 JSON
* accessToken 저장 방식 제안
* refreshToken 저장 방식 제안
* Authorization header 형식
* `/api/users/me` 호출 방식
* 인증 실패 시 프론트 처리 방식
* axios interceptor 적용 방식

예시:

```text
Authorization: Bearer ACCESS_TOKEN
```

---

## 6. Vue 프론트엔드 화면 구성 제안

Vue로 만들 화면을 제안해주세요.

최소 화면은 아래 기준으로 구성해주세요.

1. Landing Page
2. Login Page
3. Signup Page
4. Radio Create Page
5. Radio Result Page
6. Playlist Page
7. Playlist Detail Page
8. My Page

각 화면별로 아래 형식으로 정리해주세요.

```text
화면 이름:
route:
사용 목적:
필요 API:
필요 상태값:
주요 컴포넌트:
사용자 액션:
성공 시 이동:
실패 시 처리:
```

---

## 7. Vue 컴포넌트 구조 제안

Vue 파일 구조를 제안해주세요.

예시 구조:

```text
FE/
  src/
    main.js
    App.vue
    router/
      index.js
    api/
      axios.js
      authApi.js
      radioApi.js
      playlistApi.js
      likeApi.js
      songApi.js
    pages/
      LandingPage.vue
      LoginPage.vue
      SignupPage.vue
      RadioCreatePage.vue
      RadioResultPage.vue
      PlaylistPage.vue
      PlaylistDetailPage.vue
      MyPage.vue
    components/
      common/
      radio/
      playlist/
      song/
    store/
      authStore.js
      radioStore.js
      playlistStore.js
    assets/
      styles/
        global.css
```

현재 프로젝트 구조에 맞게 실제로 적용 가능한 구조로 정리해주세요.

---

## 8. Vue에서 사용할 API 모듈 설계

JavaScript 기준으로 axios API 모듈 구조를 제안해주세요.

아래 파일별 역할과 함수명을 작성해주세요.

```text
api/axios.js
api/authApi.js
api/radioApi.js
api/playlistApi.js
api/likeApi.js
api/songApi.js
```

예시:

```javascript
export async function login(payload) {
  const response = await api.post('/api/auth/login', payload)
  return response.data
}
```

실제 백엔드 API 경로와 DTO를 기준으로 작성해주세요.

---

## 9. API별 요청/응답 JSON 예시 작성

Vue 프론트엔드에서 바로 mock data로 사용할 수 있도록 API별 요청/응답 JSON 예시를 작성해주세요.

반드시 포함할 항목:

* 회원가입
* 이메일 인증
* 로그인
* 내 정보 조회
* 라디오 생성
* 라디오 상세 조회
* 좋아요 추가
* 좋아요 삭제
* 좋아요 상태 조회
* 플레이리스트 생성
* 플레이리스트 목록 조회
* 플레이리스트 상세 조회
* 플레이리스트에 곡 추가
* 라디오 결과를 플레이리스트로 저장하는 흐름

라디오 → 플레이리스트 자동 저장이 백엔드에 구현되어 있다면 해당 API 기준으로 작성하고, 아직 미구현이면 프론트에서 라디오 추천 결과를 받아 플레이리스트 생성 후 곡을 추가하는 수동 흐름으로 작성해주세요.

---

## 10. JavaScript 상태 관리 구조 제안

Vue에서 사용할 상태 관리 구조를 정리해주세요.

Pinia를 쓰지 않는 기본 JavaScript 방식과, Pinia를 쓰는 방식 둘 다 간단히 제안해주세요.

최소 상태:

```text
authState
- accessToken
- refreshToken
- user
- isAuthenticated

radioState
- mood
- story
- era
- genre
- radioSessionId
- djMent
- recommendedSongs
- playlistId

playlistState
- playlists
- selectedPlaylist

uiState
- loading
- error
- toast
```

---

## 11. CSS 디자인 방향 제안

RevibeK의 콘셉트에 맞는 CSS 디자인 방향을 제안해주세요.

서비스 콘셉트:

```text
감정·상황 기반 K-POP AI 커버 라디오 큐레이션 서비스
```

디자인 키워드:

* K-POP
* AI cover
* radio
* playlist
* nostalgia
* neon
* dark mode
* gradient
* card UI
* music player style

아래 항목을 제안해주세요.

* 전체 색감
* 배경 스타일
* 버튼 스타일
* 카드 스타일
* 라디오 생성 화면 UI
* 추천 결과 카드 UI
* 플레이리스트 UI
* 모바일 반응형 방향

---

## 12. v0 또는 Vue UI 생성용 최종 프롬프트 작성

분석 결과를 바탕으로 v0 또는 Vue UI 생성 도구에 그대로 넣을 수 있는 최종 프롬프트를 작성해주세요.

프롬프트에는 아래 내용을 포함해주세요.

* 서비스명: RevibeK
* 서비스 콘셉트
* Vue 기반 화면 구성
* HTML/CSS/JavaScript 기준
* 디자인 톤
* 페이지 구성
* 컴포넌트 구조
* mock data
* API 연동 전제
* 로그인 후 사용자 흐름
* 라디오 생성 → 추천 결과 → 플레이리스트 저장 흐름
* 반응형 디자인
* K-POP AI 커버 라디오 감성

---

## 13. 결과 저장

결과는 반드시 아래 파일에 저장해주세요.

```text
docs/answer/answer11.md
```

반드시 UTF-8 인코딩으로 저장해주세요.

---

## 14. answer11.md 형식

아래 형식으로 정리해주세요.

```text
# RevibeK Vue 프론트엔드 설계를 위한 백엔드 구조/API/JSON 분석 결과

## 1. 전체 결론

## 2. 백엔드 파일 구조 분석

## 3. 주요 도메인 구조

## 4. Controller/API 목록

## 5. 인증/JWT 흐름

## 6. Radio API 분석

## 7. Playlist API 분석

## 8. Like API 분석

## 9. User/Auth API 분석

## 10. Vue 프론트엔드 핵심 사용자 흐름

## 11. Vue 화면 구성 제안

## 12. Vue 컴포넌트 구조

## 13. axios API 모듈 구조

## 14. API별 요청/응답 JSON 예시

## 15. JavaScript 상태 관리 구조

## 16. CSS 디자인 방향

## 17. mock data 구조

## 18. v0 또는 Vue UI 생성용 최종 프롬프트

## 19. 구현 시 주의사항

## 20. 최종 판단
```

---

## 15. 최종 지시

코드를 수정하지 마세요.

백엔드 Controller, DTO, Service, Mapper, DB schema를 분석해서 Vue/HTML/JavaScript/CSS 프론트엔드에서 필요한 API/JSON 구조를 뽑아주세요.

결과는 반드시 `docs/answer/answer11.md` 파일로 UTF-8 인코딩으로 저장해주세요.
