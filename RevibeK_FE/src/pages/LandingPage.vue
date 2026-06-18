<script setup>
import { useRouter } from "vue-router"
import AppShell from "@/components/common/AppShell.vue"
import BaseButton from "@/components/common/BaseButton.vue"
import NeonWaveform from "@/components/common/NeonWaveform.vue"
import { mockSongs } from "@/mocks/songs"

const router = useRouter()
const previewSongs = mockSongs.slice(0, 3)

const features = [
  {
    title: "감정 신호 분석",
    desc: "지금의 감정과 상황을 입력하면 AI DJ가 결을 읽어요.",
  },
  {
    title: "2·3세대 K-POP 매칭",
    desc: "세대와 장르를 골라 추억과 취향을 함께 큐레이션해요.",
  },
  {
    title: "AI 커버 라디오",
    desc: "DJ 멘트와 함께 라디오처럼 흐르는 추천곡을 들려줘요.",
  },
]

function start() {
  router.push("/radio/story")
}
</script>

<template>
  <AppShell hero>
    <!-- Hero -->
    <section class="hero container">
      <div class="hero-text">
        <span class="eyebrow badge badge-neon">AI COVER RADIO · K-POP</span>
        <h1 class="hero-title text-balance">
          감정으로 시작하는<br />
          <span class="text-gradient">AI 커버 라디오</span>
        </h1>
        <p class="hero-sub text-secondary text-balance">
          오늘의 감정과 상황을 들려주세요. RevibeK의 AI DJ가 2·3세대 K-POP 감성을
          담은 AI 커버 추천곡과 멘트로 당신만의 라디오를 만들어 드려요.
        </p>
        <div class="hero-cta">
          <BaseButton size="lg" @click="start">내 사연으로 AI 커버 라디오 만들기</BaseButton>
          <BaseButton size="lg" variant="outline" @click="router.push('/login')">
            로그인
          </BaseButton>
        </div>
        <div class="hero-wave">
          <NeonWaveform :bars="36" />
          <span class="tuning text-muted">Now tuning · 92.6 MHz</span>
        </div>
      </div>

      <div class="hero-card card card-pad">
        <div class="dial">
          <span class="dial-label text-muted">REVIBEK FM</span>
          <span class="dial-freq text-gradient">92.6</span>
          <span class="dial-unit text-muted">MHz · AI DJ ON AIR</span>
        </div>
        <NeonWaveform :bars="24" />
        <p class="dial-ment text-secondary">
          "오늘 밤, 조용히 마음을 감싸 줄 노래들을 준비했어요."
        </p>
      </div>
    </section>

    <!-- Features -->
    <section class="section container">
      <div class="grid-3">
        <article v-for="f in features" :key="f.title" class="feature card card-pad">
          <span class="feature-dot" aria-hidden="true"></span>
          <h3>{{ f.title }}</h3>
          <p class="text-muted">{{ f.desc }}</p>
        </article>
      </div>
    </section>

    <!-- Preview songs -->
    <section class="section container">
      <header class="section-head">
        <h2>오늘의 추천곡 미리보기</h2>
        <p class="text-muted">감정 기반으로 큐레이션된 AI 커버 트랙의 예시예요.</p>
      </header>
      <div class="preview-grid">
        <div
          v-for="(s, i) in previewSongs"
          :key="s.songId"
          class="preview-card card card-pad"
          :style="{ animationDelay: `${i * 0.08}s` }"
        >
          <span class="preview-num text-gradient">{{ i + 1 }}</span>
          <div>
            <h3 class="preview-title">{{ s.title }}</h3>
            <p class="text-muted">{{ s.artist }} · {{ s.era }} · {{ s.genre }}</p>
          </div>
        </div>
      </div>
      <div class="text-center cta-bottom">
        <BaseButton size="lg" @click="start">나의 라디오 만들기</BaseButton>
      </div>
    </section>
  </AppShell>
</template>

<style scoped>
.hero {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 48px;
  align-items: center;
  padding-top: 72px;
  padding-bottom: 72px;
}
.eyebrow {
  margin-bottom: 20px;
}
.hero-title {
  font-size: clamp(36px, 6vw, 58px);
  font-weight: 800;
  margin-bottom: 20px;
}
.hero-sub {
  font-size: 17px;
  max-width: 520px;
  margin-bottom: 28px;
}
.hero-cta {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 32px;
}
.hero-wave {
  display: flex;
  align-items: center;
  gap: 16px;
  max-width: 460px;
}
.tuning {
  font-size: 12px;
  white-space: nowrap;
  letter-spacing: 0.04em;
}
.hero-card {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.dial {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}
.dial-label {
  font-size: 12px;
  letter-spacing: 0.2em;
}
.dial-freq {
  font-size: 56px;
  font-weight: 800;
  line-height: 1.1;
}
.dial-unit {
  font-size: 12px;
  letter-spacing: 0.08em;
}
.dial-ment {
  text-align: center;
  font-size: 14px;
  font-style: italic;
}
.grid-3 {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}
.feature-dot {
  display: block;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: var(--grad-neon);
  box-shadow: var(--glow-purple);
  margin-bottom: 14px;
}
.feature h3 {
  font-size: 18px;
  margin-bottom: 8px;
}
.section-head {
  text-align: center;
  margin-bottom: 32px;
}
.section-head h2 {
  font-size: 28px;
  margin-bottom: 8px;
}
.preview-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
.preview-card {
  display: flex;
  align-items: center;
  gap: 16px;
  animation: float-up 0.5s ease both;
}
.preview-num {
  font-size: 32px;
  font-weight: 800;
}
.preview-title {
  font-size: 16px;
  margin-bottom: 2px;
}
.cta-bottom {
  margin-top: 36px;
}
@media (max-width: 860px) {
  .hero {
    grid-template-columns: 1fr;
    padding-top: 40px;
  }
  .grid-3,
  .preview-grid {
    grid-template-columns: 1fr;
  }
}
</style>
