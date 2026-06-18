<script setup>
import { ref, reactive, onMounted, computed } from "vue"
import { useRouter } from "vue-router"
import { useRadioStore } from "@/stores/radio"
import { useUiStore } from "@/stores/ui"
import AppShell from "@/components/common/AppShell.vue"
import BaseButton from "@/components/common/BaseButton.vue"
import NeonWaveform from "@/components/common/NeonWaveform.vue"

const router = useRouter()
const radio = useRadioStore()
const ui = useUiStore()

const failed = ref(false)
const input = ref(null)

const steps = [
  "사연의 감정 신호를 읽는 중",
  "2·3세대 K-POP 감성과 매칭 중",
  "AI 커버 후보를 고르는 중",
  "DJ 멘트를 작성하는 중",
  "플레이리스트로 정리하는 중",
]

const gen = reactive(radio.generating)
const currentStep = computed(() => gen.step)

async function run() {
  const pending = radio.loadPending()
  if (!pending) {
    // nothing to generate, go back to the story input
    router.replace({ name: "radio-story" })
    return
  }
  input.value = pending
  failed.value = false
  try {
    const result = await radio.generate(pending)
    ui.success("라디오가 완성됐어요!")
    try {
      sessionStorage.setItem("revibek.lastRadioResult", JSON.stringify(result))
    } catch {
      // ignore storage write failures (e.g. private mode)
    }
    if (result.playlistId) {
      router.replace({ name: "playlist-result", params: { id: result.playlistId } })
    } else {
      const radioId = result.radioSessionId || result.id
      if (radioId) {
        router.replace({ name: "radio-result", params: { id: radioId } })
      } else {
        // no usable id returned — keep the user on the story page
        failed.value = true
      }
    }
  } catch {
    failed.value = true
  }
}

function retry() {
  run()
}

onMounted(run)
</script>

<template>
  <AppShell :hide-footer="true">
    <div class="gen-wrap">
      <div class="gen-card">
        <NeonWaveform :active="!failed" :bars="28" class="gen-wave" />

        <template v-if="!failed">
          <p class="eyebrow">TUNING<span class="dots">...</span></p>
          <h1 class="gen-title text-balance">
            AI DJ가 당신의 사연을 선곡하는 중...
          </h1>

          <!-- 입력 요약 카드 -->
          <div v-if="input" class="summary-card">
            <div v-if="input.mood" class="summary-line">
              <span class="summary-key">오늘의 감정</span>
              <span class="summary-value">{{ input.mood }}</span>
            </div>
            <div v-if="input.situation" class="summary-line">
              <span class="summary-key">현재 상황</span>
              <span class="summary-value">{{ input.situation }}</span>
            </div>
            <div v-if="input.desiredMood" class="summary-line">
              <span class="summary-key">듣고 싶은 분위기</span>
              <span class="summary-value">{{ input.desiredMood }}</span>
            </div>
            <div v-if="input.era" class="summary-line">
              <span class="summary-key">세대</span>
              <span class="summary-value">{{ input.era }}</span>
            </div>
            <div v-if="input.genre" class="summary-line">
              <span class="summary-key">장르</span>
              <span class="summary-value">{{ input.genre }}</span>
            </div>
          </div>

          <ul class="step-list">
            <li
              v-for="(s, i) in steps"
              :key="s"
              class="step-item"
              :class="{
                'is-done': currentStep > i + 1,
                'is-active': currentStep === i + 1,
              }"
            >
              <span class="step-dot" aria-hidden="true"></span>
              <span class="step-text">{{ s }}</span>
            </li>
          </ul>

          <!-- 결과 스포일러 카드 -->
          <div class="spoiler-grid">
            <div class="spoiler-card">
              <span class="spoiler-key">오늘의 라디오 분위기</span>
              <p class="spoiler-val">"{{ gen.previewMoodText || "감성을 해석하는 중..." }}"</p>
            </div>
            <div class="spoiler-card">
              <span class="spoiler-key">예상 선곡 흐름</span>
              <p class="spoiler-val">{{ gen.previewFlowText || "흐름을 구성하는 중..." }}</p>
            </div>
            <div class="spoiler-card">
              <span class="spoiler-key">AI DJ 멘트 예고</span>
              <p class="spoiler-val">"{{ gen.previewDjMentText || "멘트를 쓰는 중..." }}"</p>
            </div>
          </div>

          <p class="count-hint text-muted">약 5곡의 AI 커버 추천곡을 준비하고 있어요.</p>
        </template>

        <template v-else>
          <p class="eyebrow err">GENERATION FAILED</p>
          <h1 class="gen-title">라디오 생성에 실패했어요</h1>
          <p class="err-msg">{{ gen.error || "잠시 후 다시 시도해 주세요." }}</p>
          <div class="err-actions">
            <BaseButton size="lg" @click="retry">다시 생성하기</BaseButton>
            <BaseButton variant="outline" size="lg" @click="router.push({ name: 'radio-story' })">
              사연 수정하기
            </BaseButton>
          </div>
        </template>
      </div>
    </div>
  </AppShell>
</template>

<style scoped>
.gen-wrap {
  display: flex;
  justify-content: center;
  padding: 48px 0 80px;
}
.gen-card {
  width: 100%;
  max-width: 640px;
  background: var(--card-bg);
  border: 1px solid var(--card-border);
  border-radius: var(--radius-lg);
  padding: 40px 32px;
  text-align: center;
}
.gen-wave {
  margin: 0 auto 28px;
}
.eyebrow {
  color: var(--neon-cyan);
  letter-spacing: 0.18em;
  font-size: 12px;
  font-weight: 700;
}
.eyebrow.err {
  color: var(--danger);
}
.gen-title {
  font-size: clamp(22px, 4vw, 30px);
  font-weight: 800;
  margin-top: 10px;
  line-height: 1.25;
}
.dots {
  display: inline-block;
  animation: tuning 1.4s steps(4, end) infinite;
  width: 1.2em;
  text-align: left;
  overflow: hidden;
  vertical-align: bottom;
}
@keyframes tuning {
  0% { clip-path: inset(0 100% 0 0); }
  100% { clip-path: inset(0 0 0 0); }
}
.summary-card {
  margin: 22px auto 0;
  max-width: 420px;
  text-align: left;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--card-border);
  border-radius: var(--radius-md);
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.summary-line {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  font-size: 14px;
}
.summary-key {
  color: var(--text-muted);
  flex-shrink: 0;
}
.summary-value {
  color: var(--text-primary);
  font-weight: 600;
  text-align: right;
}
.spoiler-grid {
  margin-top: 28px;
  display: grid;
  gap: 12px;
  text-align: left;
}
.spoiler-card {
  background: rgba(120, 75, 160, 0.12);
  border: 1px solid var(--card-border);
  border-radius: var(--radius-md);
  padding: 16px 18px;
}
.spoiler-key {
  font-size: 12px;
  font-weight: 700;
  color: var(--neon-magenta);
  letter-spacing: 0.04em;
}
.spoiler-val {
  margin-top: 6px;
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.55;
}
.count-hint {
  margin-top: 22px;
  font-size: 13px;
}
.step-list {
  list-style: none;
  margin: 32px auto 0;
  max-width: 360px;
  text-align: left;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.step-item {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--text-muted);
  transition: color 0.3s ease;
}
.step-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 2px solid var(--card-border-strong);
  flex-shrink: 0;
  transition: all 0.3s ease;
}
.step-item.is-active {
  color: var(--text-primary);
}
.step-item.is-active .step-dot {
  border-color: var(--neon-cyan);
  box-shadow: 0 0 12px var(--neon-cyan);
  animation: pulse 1s ease-in-out infinite;
}
.step-item.is-done {
  color: var(--text-secondary);
}
.step-item.is-done .step-dot {
  background: var(--neon-magenta);
  border-color: var(--neon-magenta);
}
.err-msg {
  color: var(--text-secondary);
  margin-top: 12px;
}
.err-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-top: 28px;
  flex-wrap: wrap;
}
@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.25); }
}
</style>
