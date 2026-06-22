<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from "vue"
import { useRoute, useRouter } from "vue-router"
import { useRadioStore } from "@/stores/radio"
import { useUiStore } from "@/stores/ui"
import playlistApi from "@/api/playlistApi"
import AppShell from "@/components/common/AppShell.vue"
import BaseButton from "@/components/common/BaseButton.vue"
import NeonWaveform from "@/components/common/NeonWaveform.vue"
import SongCard from "@/components/song/SongCard.vue"
import LoadingOverlay from "@/components/common/LoadingOverlay.vue"

const route = useRoute()
const router = useRouter()
const radio = useRadioStore()
const ui = useUiStore()

const SESSION_KEY = "revibek.lastRadioResult"

const loading = ref(true)
const data = ref(null)
const speaking = ref(false)
const audioEl = ref(null)
const autoplayBlocked = ref(false)

// id of the recommended song whose video is currently expanded in its card
const playingSongId = ref(null)

// batch "add all recommended songs" state
const addingAll = ref(false)
const batchMessage = ref("")

const playlistId = computed(() => route.params.id)
const songs = computed(
  () => data.value?.recommendedSongs || data.value?.songs || []
)

function songKey(song) {
  return song?.songId || song?.id
}
function isPlaying(song) {
  return playingSongId.value != null && playingSongId.value === songKey(song)
}
function togglePlay(song) {
  const key = songKey(song)
  playingSongId.value = playingSongId.value === key ? null : key
}

async function addAllToPlaylist() {
  if (!playlistId.value || !songs.value.length) return
  addingAll.value = true
  batchMessage.value = ""
  try {
    const songIds = songs.value
      .map((s) => s.songId || s.id)
      .filter(Boolean)
    const result = await playlistApi.addItemsBatch(playlistId.value, songIds)
    batchMessage.value =
      result.added > 0
        ? `추천곡 ${result.added}곡을 담았어요.`
        : "추천곡이 이미 모두 플레이리스트에 담겨 있어요."
    ui.success(batchMessage.value)
  } catch {
    batchMessage.value = "추천곡 담기에 실패했어요. 다시 시도해주세요."
    ui.error(batchMessage.value)
  } finally {
    addingAll.value = false
  }
}
const tts = computed(() => data.value?.tts || null)
const hasAudioTts = computed(() => !!tts.value?.audioUrl)
const hasBrowserTts = computed(
  () =>
    typeof window !== "undefined" &&
    "speechSynthesis" in window &&
    (tts.value?.mode === "BROWSER_TTS" || !!data.value?.djMent)
)
const canPlayDjMent = computed(() => hasAudioTts.value || hasBrowserTts.value)

function matchesCurrentPlaylist(result) {
  return result && result.playlistId === playlistId.value
}

async function load() {
  loading.value = true
  try {
    // 1) freshly generated result in the Pinia store
    if (matchesCurrentPlaylist(radio.current)) {
      data.value = radio.current
      return
    }

    // 2) sessionStorage fallback (survives refresh)
    try {
      const saved = sessionStorage.getItem(SESSION_KEY)
      if (saved) {
        const parsed = JSON.parse(saved)
        if (matchesCurrentPlaylist(parsed)) {
          data.value = parsed
          radio.current = parsed
          return
        }
      }
    } catch {
      // ignore malformed storage
    }

    // 3) fetch playlist detail by id
    data.value = await radio.fetchByPlaylist(playlistId.value)
  } catch {
    ui.error("플레이리스트를 불러오지 못했어요.")
  } finally {
    loading.value = false
  }
}

function stopDjMent() {
  if (typeof window !== "undefined" && "speechSynthesis" in window) {
    window.speechSynthesis.cancel()
  }
  if (audioEl.value) {
    audioEl.value.pause()
    audioEl.value.currentTime = 0
  }
  speaking.value = false
}

// auto = true when triggered by the post-load autoplay attempt.
function playDjMent(auto = false) {
  if (speaking.value) return
  autoplayBlocked.value = false

  // 1) Prefer a real audio file when provided
  if (hasAudioTts.value) {
    if (!audioEl.value) audioEl.value = new Audio(tts.value.audioUrl)
    audioEl.value.onended = () => (speaking.value = false)
    audioEl.value.onerror = () => (speaking.value = false)
    speaking.value = true
    audioEl.value.play().catch(() => {
      speaking.value = false
      if (auto) autoplayBlocked.value = true
    })
    return
  }

  // 2) Fallback to the Web Speech API
  if (hasBrowserTts.value) {
    const text = tts.value?.text || data.value?.djMent || ""
    const utter = new SpeechSynthesisUtterance(text)
    utter.lang = "ko-KR"
    utter.rate = 0.98
    utter.onend = () => (speaking.value = false)
    utter.onerror = () => (speaking.value = false)
    speaking.value = true
    window.speechSynthesis.cancel()
    window.speechSynthesis.speak(utter)
    // Some browsers block speech until a user gesture; detect that for autoplay.
    if (auto) {
      setTimeout(() => {
        if (!window.speechSynthesis.speaking && !window.speechSynthesis.pending) {
          speaking.value = false
          autoplayBlocked.value = true
        }
      }, 400)
    }
    return
  }

  if (!auto) ui.notify("이 브라우저는 음성 재생을 지원하지 않아요.", "info")
}

// keep a single backward-compatible toggle for the waveform button
function toggleDjMent() {
  if (speaking.value) stopDjMent()
  else playDjMent(false)
}

// Try to autoplay the DJ ment shortly after the result loads.
watch(
  data,
  (val) => {
    if (val && canPlayDjMent.value) {
      setTimeout(() => playDjMent(true), 500)
    }
  },
  { immediate: false }
)

function goDetail() {
  router.push({ name: "playlist-detail", params: { id: playlistId.value } })
}

function makeAnother() {
  router.push({ name: "radio-story" })
}

onMounted(load)
onBeforeUnmount(stopDjMent)
</script>

<template>
  <AppShell>
    <LoadingOverlay v-if="loading" message="플레이리스트를 정리하는 중..." />

    <div v-else-if="data" class="pr-wrap">
      <!-- 완료 헤더 -->
      <header class="pr-hero">
        <div class="pr-hero-bg" aria-hidden="true"></div>
        <div class="pr-hero-inner">
          <span class="saved-badge">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.5">
              <path d="M20 6 9 17l-5-5" />
            </svg>
            플레이리스트 저장 완료
          </span>

          <h1 class="pr-title text-balance">
            AI DJ가 당신의 사연으로 플레이리스트를 만들었어요
          </h1>
          <p class="pr-sub text-pretty">
            방금 작성한 사연을 바탕으로 오늘의 감정에 어울리는 곡들을 골랐습니다.
          </p>

          <div class="pr-meta">
            <span v-if="data.mood" class="badge badge-mood">{{ data.mood }}</span>
            <span v-if="data.era" class="badge">{{ data.era }}</span>
            <span v-if="data.genre" class="badge">{{ data.genre }}</span>
            <span v-if="data.videoType" class="badge badge-neon">{{ data.videoType }}</span>
          </div>

          <NeonWaveform :active="speaking" :bars="36" class="pr-wave" />

          <!-- AI DJ 멘트 + TTS -->
          <div class="dj-box">
            <div class="dj-head">
              <span class="dj-tag">AI DJ MENT</span>
              <div v-if="canPlayDjMent" class="dj-controls">
                <BaseButton
                  size="sm"
                  variant="outline"
                  :disabled="speaking"
                  @click="playDjMent(false)"
                >
                  DJ 멘트 재생
                </BaseButton>
                <BaseButton
                  size="sm"
                  variant="ghost"
                  :disabled="!speaking"
                  @click="stopDjMent"
                >
                  정지
                </BaseButton>
              </div>
            </div>
            <p class="dj-text text-pretty">{{ data.djMent }}</p>
            <p v-if="autoplayBlocked" class="dj-blocked">
              브라우저 정책으로 자동재생이 차단되었습니다. DJ 멘트 재생 버튼을 눌러주세요.
            </p>
          </div>

          <!-- 액션 버튼 -->
          <div class="pr-actions">
            <BaseButton size="lg" @click="goDetail">플레이리스트 상세보기</BaseButton>
            <BaseButton variant="outline" size="lg" @click="makeAnother">
              새 사연으로 다시 만들기
            </BaseButton>
          </div>
        </div>
      </header>

      <!-- 추천곡 (YouTube 영상 목록) -->
      <section class="pr-songs">
        <div class="songs-head">
          <div>
            <h2 class="section-title">추천곡 {{ songs.length }}</h2>
            <p class="text-muted songs-hint">
              재생 버튼을 누르면 카드 안에서 바로 영상이 펼쳐져요.
            </p>
          </div>
          <div class="batch-box">
            <BaseButton
              :loading="addingAll"
              :disabled="!playlistId || !songs.length"
              @click="addAllToPlaylist"
            >
              추천곡 전체 담기
            </BaseButton>
            <p v-if="batchMessage" class="batch-msg">{{ batchMessage }}</p>
          </div>
        </div>
        <div class="song-grid">
          <SongCard
            v-for="(s, i) in songs"
            :key="s.songId || s.id || i"
            :song="s"
            :show-save="false"
            :playing="isPlaying(s)"
            @play="togglePlay"
          />
        </div>
      </section>
    </div>

    <!-- 데이터 없음 -->
    <div v-else class="pr-empty">
      <h1 class="empty-title">결과를 찾을 수 없어요</h1>
      <p class="text-muted empty-text">
        플레이리스트 정보가 만료되었거나 존재하지 않습니다.
      </p>
      <BaseButton size="lg" @click="makeAnother">새 사연으로 만들기</BaseButton>
    </div>
  </AppShell>
</template>

<style scoped>
.pr-wrap {
  max-width: 880px;
  margin: 0 auto;
  padding: 32px 0 80px;
  display: flex;
  flex-direction: column;
  gap: 36px;
}

/* hero */
.pr-hero {
  position: relative;
  border-radius: var(--radius-lg);
  border: 1px solid var(--card-border-strong);
  overflow: hidden;
  background: var(--card-bg);
}
.pr-hero-bg {
  position: absolute;
  inset: 0;
  background: var(--grad-hero);
  opacity: 0.92;
}
.pr-hero-inner {
  position: relative;
  padding: 38px 32px;
}
.saved-badge {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 13px;
  font-weight: 700;
  color: var(--neon-cyan);
  border: 1px solid var(--neon-cyan);
  background: rgba(43, 223, 219, 0.1);
  border-radius: var(--radius-pill);
  padding: 6px 14px;
  box-shadow: 0 0 18px rgba(43, 223, 219, 0.3);
}
.pr-title {
  font-size: clamp(22px, 4.5vw, 34px);
  font-weight: 800;
  line-height: 1.25;
  margin-top: 18px;
}
.pr-sub {
  color: var(--text-secondary);
  margin-top: 12px;
  max-width: 540px;
  line-height: 1.6;
}
.pr-meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 18px;
}
.badge-mood {
  background: var(--grad-neon);
  color: #ffffff;
  border-color: transparent;
}
.pr-wave {
  margin: 26px 0;
}

/* DJ ment */
.dj-box {
  background: rgba(10, 10, 24, 0.5);
  border: 1px solid var(--card-border);
  border-radius: var(--radius-md);
  padding: 18px;
}
.dj-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}
.dj-tag {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.1em;
  color: var(--neon-cyan);
}
.dj-text {
  color: var(--text-secondary);
  line-height: 1.65;
}
.dj-controls {
  display: flex;
  gap: 8px;
}
.dj-blocked {
  margin-top: 12px;
  font-size: 13px;
  color: var(--neon-magenta);
  background: rgba(255, 60, 172, 0.08);
  border: 1px solid rgba(255, 60, 172, 0.35);
  border-radius: var(--radius-sm);
  padding: 10px 12px;
  line-height: 1.5;
}

.batch-box {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
}
.batch-msg {
  font-size: 13px;
  color: var(--neon-cyan);
  font-weight: 600;
}

/* actions */
.pr-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
  flex-wrap: wrap;
}

/* songs */
.songs-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 18px;
}
.section-title {
  font-size: 20px;
  font-weight: 700;
}
.songs-hint {
  font-size: 13px;
}
.song-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 14px;
}
@media (min-width: 720px) {
  .song-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

/* empty */
.pr-empty {
  max-width: 480px;
  margin: 80px auto;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
}
.empty-title {
  font-size: 24px;
  font-weight: 800;
}
.empty-text {
  line-height: 1.6;
}
</style>
