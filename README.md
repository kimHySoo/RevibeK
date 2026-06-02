# 🎵 RevibeK — K-POP AI 라디오 서비스

> AI로 되살린 K-POP 감성 + 개인형 위로 라디오 서비스


---
## 반드시 먼저 실행할것!!!!
```
git pull origin develop
```
## 🛠 기술 스택

| 구분 | 기술 |
|---|---|
| Backend | Spring Boot 3.x, MyBatis, Spring Security, JWT |
| Build | Maven |
| Database | MySQL 8.0 |
| Frontend | React (별도 레포) |
| External API | YouTube Data API v3, Claude API, Clova TTS |
| Tools | Swagger (springdoc-openapi) |

---

## 📁 패키지 구조

```
com.ssafy.revibek
├── config          # Security, Swagger, CORS 설정
├── interceptor     # JWT 인증 인터셉터
├── model
│   ├── dao         # MyBatis DB 접근 (Mapper)
│   ├── dto         # 요청/응답 데이터 객체
│   └── service     # 비즈니스 로직
└── controller      # API 엔드포인트 (추가 예정)
```

---

## 🌿 브랜치 전략

```
main        → 배포용. 직접 push 금지
develop     → 통합 브랜치. PR로만 머지
feature/*   → 기능별 작업 브랜치
hotfix/*    → 긴급 버그 수정
```

### 브랜치 담당

| 브랜치 | 담당 | 내용 |
|---|---|---|
| feature/song | 김형수 | 노래 CRUD · 검색 |
| feature/recommend | 김형수 | 추천 엔진 · 점수 계산 |
| feature/playlist | 김형수 | 플레이리스트 |
| feature/user | 김재원 | 유저 · 인증 · OAuth2 |
| feature/radio | 김재원 | AI 라디오 · TTS |
| feature/user-song | 김재원 | 보관함 · 저장 · 평가 |

---

## 🚀 시작하기

### 1. 레포지토리 clone

```bash
git clone https://github.com/팀명/RevibeK.git
cd RevibeK
```

### 2. 브랜치 확인

```bash
git branch -a
```

아래처럼 나오면 정상이에요.

```
* main
  remotes/origin/main
  remotes/origin/develop
```

### 3. develop 브랜치로 이동

```bash
git checkout develop
```

develop이 로컬에 없으면 아래처럼 해요.

```bash
git fetch origin
git checkout -b develop origin/develop
```

### 4. develop 최신 상태로 업데이트

```bash
git pull origin develop
```

### 5. 내 feature 브랜치 생성

담당 브랜치에 맞게 생성해요.

```bash
# 예시 — 유저 담당이면
git checkout -b feature/user

# 노래 담당이면
git checkout -b feature/song
```

### 6. 브랜치 확인

```bash
git branch
```

아래처럼 나오면 성공이에요.

```
  main
  develop
* feature/user
```

---

## 💻 Eclipse / IntelliJ 세팅

### 1. 프로젝트 열기

**Eclipse**
```
File → Import → Maven → Existing Maven Projects
→ clone한 폴더 선택 → pom.xml 인식 확인 → Finish
```

**IntelliJ**
```
File → Open → clone한 폴더 선택
→ pom.xml 자동 인식 후 Maven sync 완료되면 실행 가능
```

### 2. application-secret.properties 생성

`src/main/resources/` 아래에 `application-secret.properties` 파일을 직접 만들어요.
(팀장에게 내용 따로 받아서 입력)

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/kpop_radio
spring.datasource.username=root
spring.datasource.password=여기에_비밀번호_입력
youtube.api.key=여기에_키_입력
claude.api.key=여기에_키_입력
```

> ⚠️ 이 파일은 절대 GitHub에 올리면 안 돼요. `.gitignore`에 이미 등록되어 있어요.

### 3. MySQL DB 생성

sql폴더 확인

### 4. 서버 실행 후 Swagger 확인

```
http://localhost:8080/swagger-ui/index.html
```

---

## 📝 작업 흐름

### 매일 작업 시작 전 — develop 최신화

```bash
git checkout develop
git pull origin develop
git checkout feature/내브랜치
git merge develop
```

### 작업 후 커밋 & push

```bash
git add .
git commit -m "feat: 유저 로그인 API 구현"
git push origin feature/user
```

### PR 생성

GitHub에서 `feature/user → develop` 으로 PR 생성해요.
상대방 리뷰 후 머지해요. 리뷰 없이 혼자 머지하지 않아요.

---

## 📌 커밋 메시지 규칙

| 태그 | 설명 | 예시 |
|---|---|---|
| feat | 새 기능 추가 | feat: 노래 검색 API 구현 |
| fix | 버그 수정 | fix: 추천 점수 계산 오류 수정 |
| refactor | 리팩토링 | refactor: SongService 구조 개선 |
| docs | 문서 수정 | docs: Swagger 명세 추가 |
| chore | 설정 변경 | chore: Maven 의존성 추가 |
| test | 테스트 추가 | test: SongService 단위 테스트 |

---

## ⚠️ 협업 규칙

1. `main`, `develop` 직접 push 절대 금지 — PR로만 머지
2. PR 머지 전 상대방 코드 리뷰 필수
3. 매일 아침 develop pull 후 작업 시작
4. API 키, 비밀번호 절대 커밋 금지
5. 충돌 발생 시 혼자 해결하지 말고 팀원과 함께 확인

---

## 👥 팀원

| 이름 | 역할 | 담당 |
|---|---|---|
| 김형수 | 백엔드 | 노래 · 추천 · 플레이리스트 |
| 김재원 | 백엔드 | 유저 · 라디오 · 보관함 |
