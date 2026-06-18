<script setup>
import { reactive, ref, onMounted, computed } from "vue"
import { useRouter, useRoute } from "vue-router"
import { useAuthStore } from "@/stores/auth"
import { useRadioStore, emptyRadioStory } from "@/stores/radio"
import { useUiStore } from "@/stores/ui"
import { getYoutubeId } from "@/utils/youtube"
import AppShell from "@/components/common/AppShell.vue"
import BaseButton from "@/components/common/BaseButton.vue"
import MoodSelector from "@/components/radio/MoodSelector.vue"
import EraChips from "@/components/radio/EraChips.vue"
import GenreChips from "@/components/radio/GenreChips.vue"
import PromptField from "@/components/radio/PromptField.vue"

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const radio = useRadioStore()
const ui = useUiStore()

const form = reactive(emptyRadioStory())
const submitting = ref(false)

// URL이 입력됐지만 유효한 YouTube ID를 추출할 수 없을 때 true
const youtubeUrlInvalid = computed(
  () => !!form.youtubeUrl && !getYoutubeId(form.youtubeUrl)
)

const videoTypes = ["원곡", "라이브", "커버", "리믹스", "무대", "AI cover"]

const canSubmit = computed(() => form.mood && (form.situation || form.story))

onMounted(() => {
  // Restore a pending story if the user came back from login/signup
  const pending = radio.loadPending()
  if (pending) Object.assign(form, pending)

  // Allow quick-start prefills from the main page
  if (route.query.mood) form.mood = String(route.query.mood)
  if (route.query.desiredMood) form.desiredMood = String(route.query.desiredMood)
})

async function onSubmit() {
  if (!canSubmit.value) {
    ui.toast("감정과 사연(또는 상황)을 입력해 주세요.", "error")
    return
  }

  const payload = { ...form }
  if (form.saveAsPlaylist && !payload.playlistTitle) {
    payload.playlistTitle = payload.title || `${form.mood} 라디오`
  }

  // Always preserve the story so it survives the auth flow
  radio.savePending(payload)

  if (!auth.isAuthenticated) {
    ui.toast("사연을 저장했어요. 로그인하면 바로 선곡을 시작해요.", "info")
    router.push({ name: "login", query: { redirect: "/radio/generating" } })
    return
  }

  submitting.value = true
  router.push({ name: "radio-generating" })
}
</script>

<template>
  <AppShell>
    <div class="story-wrap">
      <header class="story-head">
        <p class="eyebrow">RADIO STORY · 사연 신청</p>
        <h1 class="title text-balance">오늘 어떤 하루였나요?</h1>
        <p class="subtitle text-pretty">
          당신의 사연을 들려주면 AI DJ가 어울리는 K-POP 커버 라디오를 만들어드릴게요.
        </p>
      </header>

      <form class="story-card card" @submit.prevent="onSubmit">
        <span class="story-clip" aria-hidden="true"></span>

        <section class="field-block">
          <label class="field-label">라디오 제목 <span class="muted">(선택)</span></label>
          <input
            v-model="form.title"
            type="text"
            class="text-input"
            placeholder="예) 새벽 감성 시티팝 라디오"
            maxlength="40"
          />
        </section>

        <section class="field-block">
          <label class="field-label">오늘의 감정 <span class="req">*</span></label>
          <MoodSelector v-model="form.mood" />
        </section>

        <div class="field-grid">
          <section class="field-block">
            <label class="field-label">현재 상황</label>
            <input
              v-model="form.situation"
              type="text"
              class="text-input"
              placeholder="예) 퇴근길 지하철, 창밖에 비가 내릴 때"
              maxlength="60"
            />
          </section>
          <section class="field-block">
            <label class="field-label">듣고 싶은 분위기</label>
            <input
              v-model="form.desiredMood"
              type="text"
              class="text-input"
              placeholder="예) 위로받고 싶어요 / 힘이 나면 좋겠어요"
              maxlength="60"
            />
          </section>
        </div>

        <section class="field-block">
          <label class="field-label">사연 <span class="req">*</span></label>
          <PromptField v-model="form.story" :maxlength="300" />
        </section>

        <div class="field-grid">
          <section class="field-block">
            <label class="field-label">세대</label>
            <EraChips v-model="form.era" />
          </section>
          <section class="field-block">
            <label class="field-label">영상 타입</label>
            <div class="select-wrap">
              <select v-model="form.videoType" class="text-input">
                <option v-for="t in videoTypes" :key="t" :value="t">{{ t }}</option>
              </select>
            </div>
          </section>
        </div>

        <section class="field-block">
          <label class="field-label">장르</label>
          <GenreChips v-model="form.genre" />
        </section>

        <div class="field-grid">
          <section class="field-block">
            <label class="field-label">선호 아티스트 <span class="muted">(선택)</span></label>
            <input
              v-model="form.preferredArtist"
              type="text"
              class="text-input"
              placeholder="예) 아이유, 잔나비"
              maxlength="60"
            />
          </section>
          <section class="field-block">
            <label class="field-label">제외 키워드 <span class="muted">(선택)</span></label>
            <input
              v-model="form.excludedKeywords"
              type="text"
              class="text-input"
              placeholder="예) 시끄러운 곡 제외"
              maxlength="60"
            />
          </section>
        </div>

        <section class="field-block">
          <label class="field-label">
            기준 노래 YouTube URL <span class="muted">(선택)</span>
          </label>
          <input
            v-model="form.youtubeUrl"
            type="url"
            class="text-input"
            :class="{ 'input-error': youtubeUrlInvalid }"
            placeholder="예) https://www.youtube.com/watch?v=..."
          />
          <p v-if="youtubeUrlInvalid" class="field-error">
            올바른 YouTube URL을 입력해주세요.
          </p>
          <p v-else class="field-hint">
            이 곡과 비슷한 분위기로 선곡돼요. Qdrant 활성화 환경에서 동작합니다.
          </p>
        </section>

        <section class="field-block save-block">
          <label class="checkbox-row">
            <input v-model="form.saveAsPlaylist" type="checkbox" />
            <span>생성된 라디오를 플레이리스트로 저장하기</span>
          </label>
          <input
            v-if="form.saveAsPlaylist"
            v-model="form.playlistTitle"
            type="text"
            class="text-input"
            placeholder="플레이리스트 제목 (비우면 자동 생성)"
            maxlength="40"
          />
        </section>

        <div class="submit-row">
          <BaseButton type="submit" size="lg" block :loading="submitting" :disabled="!canSubmit">
            AI DJ에게 사연 보내기
          </BaseButton>
          <p v-if="!auth.isAuthenticated" class="login-hint">
            사연은 저장되니, 회원가입/로그인 후 바로 이어서 만들 수 있어요.
          </p>
        </div>
      </form>
    </div>
  </AppShell>
</template>

<style scoped>
.story-wrap {
  max-width: 760px;
  margin: 0 auto;
  padding: 32px 0 80px;
}
.story-head {
  margin-bottom: 28px;
  text-align: center;
}
.eyebrow {
  color: var(--neon-cyan);
  letter-spacing: 0.18em;
  font-size: 12px;
  font-weight: 700;
  margin-bottom: 10px;
}
.title {
  font-size: clamp(26px, 5vw, 38px);
  font-weight: 800;
  line-height: 1.2;
}
.subtitle {
  color: var(--text-secondary);
  margin-top: 12px;
  line-height: 1.6;
  max-width: 52ch;
  margin-inline: auto;
}
.story-card {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding: 36px 28px 30px;
}
.story-clip {
  position: absolute;
  top: -10px;
  left: 50%;
  transform: translateX(-50%);
  width: 64px;
  height: 20px;
  border-radius: var(--radius-pill);
  background: var(--grad-neon);
  box-shadow: var(--glow-magenta);
}
.field-block {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.field-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 24px;
}
@media (min-width: 640px) {
  .field-grid {
    grid-template-columns: 1fr 1fr;
  }
}
.field-label {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}
.req {
  color: var(--neon-magenta);
}
.muted {
  color: var(--text-muted);
  font-weight: 400;
  font-size: 13px;
}
.text-input {
  width: 100%;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--card-border);
  border-radius: var(--radius-md);
  padding: 13px 15px;
  color: var(--text-primary);
  font-size: 15px;
  font-family: inherit;
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
}
.text-input:focus {
  outline: none;
  border-color: var(--neon-cyan);
  box-shadow: 0 0 0 3px rgba(0, 224, 255, 0.12);
}
.select-wrap select {
  appearance: none;
  cursor: pointer;
}
.save-block {
  gap: 14px;
}
.checkbox-row {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
}
.checkbox-row input {
  width: 18px;
  height: 18px;
  accent-color: var(--neon-magenta);
}
.submit-row {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  margin-top: 4px;
}
.login-hint {
  font-size: 13px;
  color: var(--text-muted);
  text-align: center;
}
.field-hint {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: -4px;
}
.field-error {
  font-size: 12px;
  color: var(--danger, #ff4d6a);
  margin-top: -4px;
}
.text-input.input-error {
  border-color: var(--danger, #ff4d6a);
  box-shadow: 0 0 0 3px rgba(255, 77, 106, 0.12);
}
</style>
