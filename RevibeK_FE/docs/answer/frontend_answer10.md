# 프론트엔드 작업 보고서 (10차)

소셜/큐레이션 기능 확장: 리바이브닝 공개 피드, 라디오 공개/비공개 전환, 회원 기능,
리뷰, 팔로우, 청취 계획, K-POP 챌린지(템플릿형), 플레이리스트 수정.
백엔드/디자인/라우팅 구조는 변경하지 않았고 프론트엔드만 확장했습니다.
모든 API는 `USE_MOCK` 분기를 유지해 mock 데이터로도 동작합니다.

---

## 1. API 계층 (src/api)

| 파일 | 역할 | 실제 엔드포인트 |
| --- | --- | --- |
| `userApi.js` (확장) | 회원정보 수정 / 탈퇴 | `PUT /users/me`, `DELETE /users/me` |
| `reviewApi.js` | 곡 리뷰 CRUD + 내 리뷰 | `GET/POST/PUT/DELETE /songs/{songId}/reviews`, `GET /reviews/me` |
| `followApi.js` | 팔로우/언팔로우/목록 | `POST/DELETE /follows/{userId}`, `GET /follows/followings`, `GET /follows/followers` |
| `planApi.js` | 청취 계획 CRUD + 완료 | `GET/POST/DELETE /plans`, `PATCH /plans/{id}/complete` |
| `challengeApi.js` (재작성) | 챌린지 템플릿/참여/내 챌린지 | `GET /challenges/templates`, `POST /challenges`, `GET /challenges/me`, `PATCH /challenges/{id}/progress` |
| `publicRadioApi.js` | 공개 라디오 피드/상세/공개토글 | `GET /radio/public`, `GET /radio/public/{id}`, `PATCH /radio/{id}/visibility` |
| `radioLikeApi.js` | 라디오 사연 좋아요 | `POST/DELETE /radio/{id}/likes`, `GET /radio/{id}/likes` |
| `playlistApi.js` (확장) | 플레이리스트 수정 | `PUT /playlists/{id}` |

> axios `baseURL`은 `/api`이므로 위 경로 앞에 `/api`가 자동으로 붙습니다.
> 공개 피드 조회는 비로그인 접근을 위해 `_skipAuthRefresh`로 401 인터셉터 우회.

### Mock 데이터 (src/mocks)
- `reviews.js`, `social.js`, `plans.js`, `challenges.js`(템플릿 4종 + 내 챌린지), `publicRadio.js`
- `radio.js`: 라디오 기록에 `isPublic` / `publishedAt` 필드 추가

---

## 2. 리바이브닝 (공개 라디오 피드)

- 신규 페이지 `pages/RevibeningPage.vue`, 라우트 `/revibening` (name: `revibening`, 인증 불필요)
- `components/common/AppHeader.vue` 네비게이션에 "리바이브닝" 링크 추가
- 구성 컴포넌트
  - `components/radio/PublicRadioCard.vue` — 공개 사연 카드(작성자, 무드/세대/장르, 좋아요 수, 팔로우 버튼)
  - `components/radio/RadioLikeButton.vue` — 사연 좋아요(곡 좋아요와 분리)
  - `components/social/FollowButton.vue` — 카드/상세 공용 팔로우 토글
  - `components/radio/PublicRadioDetailModal.vue` — 상세(추천곡은 기존 `SongCard` 재사용, 카드 내부 영상/리뷰 그대로 활용)
- 정렬(인기순/최신순) 및 무드 필터, 빈 상태 처리 포함

---

## 3. 마이페이지 확장 (pages/MyPage.vue)

- 탭 5종으로 확장: 좋아요한 곡 / 라디오 기록 / 내 리뷰 / 팔로우 / 청취 계획
  - 탭 상태를 `?tab=` 쿼리와 양방향 동기화 (likes·radio·reviews·follow·plan)
- 라디오 기록 카드에 **공개/비공개 전환 토글** + 공개일자 표시
  - 클릭 시 `publicRadioApi.togglePublic` 호출 후 상태를 즉시 반영(반응성 확인 완료)
- 회원 기능
  - `components/user/UserEditModal.vue` — 닉네임/선호(세대·장르·무드) 수정
  - `components/user/DeleteAccountModal.vue` — 확인 입력 후 탈퇴, 세션 정리 후 랜딩 이동
  - `stores/auth.js`에 `clearSession()` / `setUser()` 액션 추가
- 패널 컴포넌트
  - `components/review/MyReviewsPanel.vue`
  - `components/social/FollowPanel.vue`
  - `components/plan/PlanPanel.vue`

---

## 4. 곡 리뷰 (카드 내부)

- `components/review/StarRating.vue` — 별점 표시/입력 공용
- `components/review/ReviewSection.vue` — 목록·작성·수정·삭제(본인 리뷰만)
- `components/song/SongCard.vue`에 `reviewable` prop + "리뷰" 토글 버튼 추가
- `pages/SongPage.vue` 카드에 `reviewable` 적용

---

## 5. K-POP 챌린지 (템플릿형)

- `components/challenge/ChallengeSection.vue` 재작성 — "어떤 챌린지 할래요?"
  - 템플릿 카드 그리드(예: "7일 동안 2세대 K-POP 듣기") + "이 챌린지 시작하기"
  - "내 챌린지" 영역에서 진행 상태 표시
- `pages/MainPage.vue` 하단에 `<ChallengeSection />` 실제 배치(임포트만 되어 있던 누락 수정)

---

## 6. 플레이리스트 수정

- `components/playlist/PlaylistEditModal.vue` — 이름/무드 태그/공개여부 수정
- `stores/playlist.js`에 `update()` 액션 추가
- `pages/PlaylistDetailPage.vue`에 "수정" 버튼 + 공개/비공개 배지 추가

---

## 7. 검증

- `npm run build` 성공 (exit 0). 신규 청크 `RevibeningPage`, `MyPage`, `SongCard` 등 정상 번들.
- 브라우저(agent-browser) 확인
  - `/revibening`: 피드 카드 렌더 + 상세 모달(추천곡 카드 재사용) 정상
  - 로그인(데모 계정) → `/main` 진입, 마이페이지 5탭/공개일자/토글 노출
  - 라디오 공개/비공개 토글 클릭 시 버튼 라벨이 즉시 전환됨(반응성 확인)
  - 홈 하단 챌린지 섹션 템플릿/내 챌린지 렌더 정상
- 디버깅용 `console.log("[v0] ...")`는 확인 후 모두 제거.

---

## 8. 신규/변경 파일 요약

신규: `api/reviewApi.js`, `api/followApi.js`, `api/planApi.js`, `api/publicRadioApi.js`,
`api/radioLikeApi.js`, `mocks/reviews.js`, `mocks/social.js`, `mocks/plans.js`,
`mocks/publicRadio.js`, `pages/RevibeningPage.vue`,
`components/radio/PublicRadioCard.vue`, `components/radio/RadioLikeButton.vue`,
`components/radio/PublicRadioDetailModal.vue`, `components/social/FollowButton.vue`,
`components/social/FollowPanel.vue`, `components/review/StarRating.vue`,
`components/review/ReviewSection.vue`, `components/review/MyReviewsPanel.vue`,
`components/plan/PlanPanel.vue`, `components/user/UserEditModal.vue`,
`components/user/DeleteAccountModal.vue`, `components/challenge/ChallengeSection.vue`,
`components/playlist/PlaylistEditModal.vue`

변경: `api/userApi.js`, `api/playlistApi.js`, `api/challengeApi.js`,
`stores/auth.js`, `stores/playlist.js`, `mocks/radio.js`, `mocks/challenges.js`,
`router/index.js`, `components/common/AppHeader.vue`, `components/common/BaseModal.vue`,
`components/song/SongCard.vue`, `components/radio/PublicRadioDetailModal.vue`,
`pages/MyPage.vue`, `pages/MainPage.vue`, `pages/SongPage.vue`,
`pages/PlaylistDetailPage.vue`

## 9. 리바이브닝 라디오 상세 모달 가로폭 확대

- 기존(440px)보다 약 1.5배 넓은 760px로 수정 (`BaseModal`에 opt-in `size` prop 추가, 상세 모달은 `size="lg"`).
- `width: min(92vw, 760px)` + `max-height: 85vh` + 세로 스크롤로 추천곡 카드 / YouTube 임베드 영역 가독성 개선 (추천곡은 넓은 화면에서 2열 그리드).
- YouTube 임베드는 `width: 100%` / `aspect-ratio: 16/9` 유지, 모바일(390px)에서 화면을 넘치지 않도록 반응형 유지.
- 헤더 상단 네비게이션에서 "메인" 항목 제거 (좌측 RevibeK 로고가 메인으로 연결).
