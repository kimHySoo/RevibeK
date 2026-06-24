<script setup>
import { ref, onMounted, computed } from "vue"
import { useRouter } from "vue-router"
import AppShell from "@/components/common/AppShell.vue"
import BaseButton from "@/components/common/BaseButton.vue"
import NeonWaveform from "@/components/common/NeonWaveform.vue"
import ChallengeSection from "@/components/challenge/ChallengeSection.vue"
import { useAuthStore } from "@/stores/auth"
import { useRadioStore } from "@/stores/radio"
import { useUiStore } from "@/stores/ui"

const router = useRouter()
const auth = useAuthStore()
const radio = useRadioStore()
const ui = useUiStore()

const pending = ref(null)
const lastRadioId = computed(
  () => radio.history?.[0]?.radioSessionId || radio.history?.[0]?.id || null
)

onMounted(async () => {
  pending.value = radio.loadPending()
  try {
    await radio.fetchHistory()
  } catch {
    /* ignore */
  }
})

function continueGenerate() {
  router.push("/radio/generating")
}

function discardPending() {
  radio.clearPending()
  pending.value = null
  ui.notify("저장된 라디오 사연을 삭제했어요.")
}

const menu = [
  {
    key: "create",
    title: "새 사연으로 라디오 만들기",
    desc: "지금의 감정과 사연을 들려주세요.",
    action: () => router.push("/radio/story"),
  },
  {
    key: "recent",
    title: "최근 라디오 결과 보기",
    desc: "방금 만든 라디오를 다시 들어요.",
    action: () =>
      lastRadioId.value
        ? router.push(`/radio/result/${lastRadioId.value}`)
        : router.push("/me?tab=radio"),
  },
  {
    key: "playlists",
    title: "내 플레이리스트",
    desc: "저장한 라디오 플레이리스트.",
    action: () => router.push("/playlists"),
  },
  {
    key: "likes",
    title: "좋아요한 곡",
    desc: "마음에 담아둔 커버 트랙.",
    action: () => router.push("/me?tab=likes"),
  },
  {
    key: "history",
    title: "내 라디오 히스토리",
    desc: "지금까지 만든 라디오 기록.",
    action: () => router.push("/me?tab=radio"),
  },
  {
    key: "me",
    title: "마이페이지",
    desc: "프로필과 취향을 관리해요.",
    action: () => router.push("/me"),
  },
]
</script>

<template>
  <AppShell hero>
    <section class="container main-top">
      <div class="welcome">
        <p class="text-muted hello-line">ON AIR · {{ auth.user?.nickname || "게스트" }}님</p>
        <h1 class="welcome-title text-balance">
          오늘은 어떤 <span class="text-gradient">감정</span>으로<br />
          라디오를 켜볼까요?
        </h1>
        <NeonWaveform :bars="32" />
      </div>
    </section>

    <!-- Continue generating card -->
    <section v-if="pending" class="container">
      <div class="pending-card card card-pad">
        <div class="pending-head">
          <span class="badge badge-neon">이어서 생성</span>
          <h2>이전에 보낸 사연이 있어요</h2>
        </div>
        <div class="pending-meta">
          <div class="meta-item">
            <span class="text-muted">감정</span>
            <strong>{{ pending.mood || "—" }}</strong>
          </div>
          <div class="meta-item">
            <span class="text-muted">세대</span>
            <strong>{{ pending.generation || "—" }}</strong>
          </div>
          <div class="meta-item">
            <span class="text-muted">장르</span>
            <strong>{{ pending.genre || "—" }}</strong>
          </div>
        </div>
        <p class="pending-q text-secondary">
          저장된 사연을 이어서 라디오로 생성하시겠어요?
        </p>
        <div class="pending-actions">
          <BaseButton @click="continueGenerate">이어서 생성하기</BaseButton>
          <BaseButton variant="danger" @click="discardPending">삭제하기</BaseButton>
        </div>
      </div>
    </section>

    <!-- Feature menu -->
    <section class="section container">
      <header class="section-head">
        <h2>무엇을 해볼까요?</h2>
        <p class="text-muted">RevibeK의 기능을 빠르게 시작해보세요.</p>
      </header>
      <div class="menu-grid">
        <button
          v-for="m in menu"
          :key="m.key"
          class="menu-card card card-pad"
          @click="m.action"
        >
          <span class="menu-dot" aria-hidden="true"></span>
          <strong class="menu-title">{{ m.title }}</strong>
          <span class="text-muted menu-desc">{{ m.desc }}</span>
          <span class="menu-arrow" aria-hidden="true">→</span>
        </button>
      </div>
    </section>

    <!-- K-POP 챌린지 -->
    <ChallengeSection />
  </AppShell>
</template>

<style scoped>
.main-top {
  padding-top: 48px;
}
.welcome {
  max-width: 640px;
}
.hello-line {
  font-size: 13px;
  letter-spacing: 0.08em;
  margin-bottom: 12px;
}
.welcome-title {
  font-size: clamp(28px, 5vw, 44px);
  font-weight: 800;
  margin-bottom: 24px;
}
.pending-card {
  border-color: var(--card-border-strong);
  box-shadow: var(--glow-purple);
}
.pending-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.pending-head h2 {
  font-size: 20px;
}
.pending-meta {
  display: flex;
  gap: 28px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.meta-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.meta-item span {
  font-size: 12px;
}
.meta-item strong {
  font-size: 17px;
}
.pending-q {
  margin-bottom: 18px;
}
.pending-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.section-head {
  margin-bottom: 24px;
}
.section-head h2 {
  font-size: 24px;
  margin-bottom: 6px;
}
.menu-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
.menu-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
  text-align: left;
  cursor: pointer;
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
}
.menu-card:hover {
  transform: translateY(-4px);
  border-color: rgba(43, 223, 219, 0.55);
  box-shadow: 0 18px 40px rgba(0, 0, 0, 0.35), 0 0 24px rgba(43, 223, 219, 0.18);
}
.menu-card:hover .menu-arrow {
  color: var(--neon-cyan);
  transform: translateX(4px);
}
.menu-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--grad-neon);
  box-shadow: var(--glow-magenta);
}
.menu-title {
  font-size: 17px;
}
.menu-desc {
  font-size: 13px;
  line-height: 1.5;
}
.menu-arrow {
  position: absolute;
  top: 22px;
  right: 22px;
  color: var(--text-muted);
  font-size: 18px;
  transition: color 0.18s ease, transform 0.18s ease;
}
@media (max-width: 860px) {
  .menu-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 520px) {
  .menu-grid {
    grid-template-columns: 1fr;
  }
}
</style>
