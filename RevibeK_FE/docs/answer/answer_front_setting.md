# RevibeK 프론트엔드 세팅/수정 결과 보고서

Vue 3 + Vite + Vue Router + Pinia + Axios 구조를 유지하며, 백엔드/DB/SQL/Spring 코드는 일절 수정하지 않고 프론트엔드 코드만 안정화했습니다. 실제 API 모드 기준으로 수정했으며, mock 전용 구조로 되돌리지 않았습니다. 기존 네온 라디오 콘셉트와 다크 UI도 그대로 유지했습니다.

수정 후 `vite build` 성공 (exit code 0).

---

## 1. LandingPage 라우팅 문제

### 점검 결과
이미 정상 상태였습니다. `src/router/index.js`에서 `/`는 `LandingPage.vue`를 렌더링하며, `meta.requiresAuth`가 없어 로그인 여부와 무관하게 접근 가능합니다.

```js
{
  path: "/",
  name: "landing",
  component: () => import("@/pages/LandingPage.vue"),
}
```

- `/radio/story`는 별도 라우트로 유지되어 사연 입력 페이지에 그대로 접근 가능합니다.
- `LandingPage.vue`의 주요 CTA(`내 사연으로 AI 커버 라디오 만들기`, `나의 라디오 만들기`)는 `router.push("/radio/story")`로 이동하도록 이미 연결되어 있습니다.

### 변경 사항
변경 없음 (요구된 동작이 이미 충족됨).

---

## 2. MyPage 탭 쿼리 반영 문제

### 문제
`MyPage.vue`가 `route.query.tab`을 읽지 않아 항상 좋아요 탭으로 시작했습니다. 내부 탭 키는 `liked` / `history`이고, URL 쿼리값은 `likes` / `radio`로 서로 달라 매핑이 필요했습니다.

### 변경 사항 (`src/pages/MyPage.vue`)
- `useRoute`, `watch` 추가.
- 쿼리값과 내부 탭 키를 매핑하는 헬퍼 추가.

```js
function tabFromQuery(q) {
  return q === "radio" ? "history" : "liked"
}
function queryFromTab(t) {
  return t === "history" ? "radio" : "likes"
}

const tab = ref(tabFromQuery(route.query.tab))
```

- 초기 탭을 `route.query.tab` 기준으로 결정: `tab=radio` → 라디오 기록 탭, `tab=likes` 또는 없음 → 좋아요 탭.
- 외부 쿼리 변경(헤더의 `/me?tab=radio` 등)에 반응하도록 `watch` 추가.

```js
watch(
  () => route.query.tab,
  (q) => { tab.value = tabFromQuery(q) }
)
```

- 탭 클릭 시 URL 쿼리도 동기화하는 `selectTab` 추가, 템플릿의 탭 버튼을 `@click="selectTab('liked')"` / `@click="selectTab('history')"`로 연결.

```js
function selectTab(next) {
  tab.value = next
  const q = queryFromTab(next)
  if (route.query.tab !== q) {
    router.replace({ query: { ...route.query, tab: q } })
  }
}
```

---

## 3. YouTube 내부 재생 방식 통일

### 방향
공용 iframe 1개를 목록 위에 두는 방식 대신, 각 곡의 재생 버튼을 누르면 해당 곡 바로 아래에서 iframe이 펼쳐지는 구조로 통일했습니다. 한 번에 하나의 영상만 펼쳐지며, 펼쳐진 곡을 다시 누르면 닫힙니다. 외부 YouTube 이동(`window.open`)은 제거했습니다.

### 공통 컴포넌트
`SongCard.vue`, `TrackRow.vue`는 이미 `playing` prop과 내부 `YoutubePlayer` 펼침 구조를 갖추고 있어 그대로 재사용했습니다. iframe `title`은 곡 제목 기반(`:title="song.title"`)이며, videoId 파싱은 `@/utils/youtube`의 `getSongYoutubeId`를 재사용합니다.

### 페이지별 상태
- `RadioResultPage.vue`, `PlaylistResultPage.vue`, `PlaylistDetailPage.vue`: 이전 작업에서 이미 카드/행 내부 펼침 + `playingSongId` 토글 방식으로 구현되어 있어 유지.
- `SongPage.vue` (변경): `playingSongId` 토글 상태와 `isPlaying`/`togglePlay`를 추가하고 `SongCard`에 `:playing="isPlaying(s)" @play="togglePlay"` 연결.
- `MyPage.vue` 좋아요 목록 (변경): 기존 `playSong`의 `window.open(youtubeUrl, "_blank")`를 제거하고, `TrackRow`에 `:playing="isPlaying(s)" @play="togglePlay"`를 연결하여 내부 재생으로 전환.

### 토글 패턴 (SongPage / MyPage 공통)
```js
const playingSongId = ref(null)
function songKey(song) { return song?.songId || song?.id }
function isPlaying(song) {
  return playingSongId.value != null && playingSongId.value === songKey(song)
}
function togglePlay(song) {
  const key = songKey(song)
  playingSongId.value = playingSongId.value === key ? null : key
}
```

---

## 4. POST /radio payload 안정화

### 문제
radio store의 `generate(story)`가 `title`, `saveAsPlaylist`, `playlistTitle`, `selectedSongs` 등 UI 전용 필드까지 포함한 story 객체를 그대로 `radioApi.create`에 넘겨, 실제 API 모드에서 `api.post("/radio", payload)`로 불필요한 필드가 전송됐습니다.

### 변경 사항 (`src/api/radioApi.js`)
백엔드 `RadioRequest` DTO에 맞는 필드만 명시적으로 구성하는 `toRadioRequest`를 추가하고, 실제 API 전송 시 이를 사용하도록 변경했습니다. mock 분기는 기존대로 전체 story를 `buildMockRadio`에 전달하여 mock 동작(saveAsPlaylist 등)을 유지합니다.

```js
function toRadioRequest(payload = {}) {
  return {
    mood: payload.mood ?? "",
    situation: payload.situation ?? "",
    desiredMood: payload.desiredMood ?? "",
    story: payload.story ?? "",
    era: payload.era ?? "",
    genre: payload.genre ?? "",
    videoType: payload.videoType ?? "",
    preferredArtist: payload.preferredArtist ?? "",
    excludedKeywords: payload.excludedKeywords ?? "",
  }
}

// create() 실제 분기
const { data } = await api.post("/radio", toRadioRequest(payload))
```

전송 필드: `mood`, `situation`, `desiredMood`, `story`, `era`, `genre`, `videoType`, `preferredArtist`, `excludedKeywords`.

---

## 수정한 파일 목록

| 파일 | 변경 내용 |
| --- | --- |
| `src/api/radioApi.js` | `toRadioRequest` 추가, 실제 `POST /radio` payload를 DTO 필드로 한정 |
| `src/pages/MyPage.vue` | `route.query.tab` 초기 반영 + `watch` + 탭 클릭 시 URL 동기화, 좋아요 목록 내부 재생 전환 |
| `src/pages/SongPage.vue` | `SongCard` 내부 재생(펼침) 토글 연결 |

## 변경하지 않은 항목
- `src/router/index.js` (이미 `/` → LandingPage 정상)
- `RadioResultPage.vue`, `PlaylistResultPage.vue`, `PlaylistDetailPage.vue` (내부 재생 이미 구현됨)
- 백엔드 / DB / SQL / Spring 코드
- API 경로 및 기존 디자인 토큰/콘셉트

## 빌드 결과
`vite build` 성공 (exit code 0).
