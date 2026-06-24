<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from "vue"
import { useRoute, useRouter } from "vue-router"
import { useRadioStore } from "@/stores/radio"
import { usePlaylistStore } from "@/stores/playlist"
import { useUiStore } from "@/stores/ui"
import playlistApi from "@/api/playlistApi"
import AppShell from "@/components/common/AppShell.vue"
import BaseButton from "@/components/common/BaseButton.vue"
import NeonWaveform from "@/components/common/NeonWaveform.vue"
import TrackRow from "@/components/song/TrackRow.vue"
import LoadingOverlay from "@/components/common/LoadingOverlay.vue"

const route = useRoute()
const router = useRouter()
const radio = useRadioStore()
const playlist = usePlaylistStore()
const ui = useUiStore()

const loading = ref(true)
const data = ref(null)
const speaking = ref(false)
const savingPlaylist = ref(false)
const autoplayBlocked = ref(false)
const audioEl = ref(null)

// id of the song whose video is currently expanded in its row
const playingSongId = ref(null)

const addingAll = ref(false)
const batchMessage = ref("")

// 생성 응답은 recommendedSongs, 조회 응답은 songs 필드를 사용하므로 둘 다 안전하게 처리한다.
const songs = computed(() => data.value?.songs ?? data.value?.recommendedSongs ?? [])

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
const tts = computed(() => data.value?.tts ?? null)
const hasAudioTts = computed(() => !!tts.value?.audioUrl)
const hasBrowserTts = computed(
  () =>
    typeof window !== "undefined" &&
    "speechSynthesis" in window &&
    (tts.value?.mode === "BROWSER_TTS" || !!data.value?.djMent)
)
const canPlayDjMent = computed(() => hasAudioTts.value || hasBrowserTts.value)

async function addAllToPlaylist() {
  if (!data.value?.playlistId || !songs.value.length) return
  addingAll.value = true
  batchMessage.value = ""
  try {
    const songIds = songs.value.map((s) => s.songId || s.id).filter(Boolean)
    const result = await playlistApi.addItemsBatch(
      data.value.playlistId,
      songIds
    )
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

async function load() {
  loading.value = true
  try {
    data.value = await radio.fetchRadio(route.params.id)
  } catch {
    ui.error("라디오를 불러오지 못했어요.")
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

function playBrowserSpeech(auto = false) {
  if (!hasBrowserTts.value) {
    speaking.value = false
    if (!auto) ui.notify("이 브라우저는 음성 재생을 지원하지 않아요.", "info")
    return
  }

  const text = tts.value?.text || data.value?.djMent || ""
  const utter = new SpeechSynthesisUtterance(text)
  utter.lang = "ko-KR"
  utter.rate = 0.98
  utter.onend = () => (speaking.value = false)
  utter.onerror = () => (speaking.value = false)
  speaking.value = true
  window.speechSynthesis.cancel()
  window.speechSynthesis.speak(utter)
  if (auto) {
    setTimeout(() => {
      if (!window.speechSynthesis.speaking && !window.speechSynthesis.pending) {
        speaking.value = false
        autoplayBlocked.value = true
      }
    }, 400)
  }
}

function playDjMent(auto = false) {
  if (speaking.value) return
  autoplayBlocked.value = false

  if (hasAudioTts.value) {
    if (!audioEl.value) audioEl.value = new Audio(tts.value.audioUrl)
    audioEl.value.onended = () => (speaking.value = false)
    // Google 오디오 재생/디코딩이 실패하면 브라우저 TTS로 자동 fallback한다.
    audioEl.value.onerror = () => {
      speaking.value = false
      playBrowserSpeech(auto)
    }
    speaking.value = true
    audioEl.value.play().catch(() => {
      speaking.value = false
      if (auto) {
        autoplayBlocked.value = true
      } else {
        playBrowserSpeech(false)
      }
    })
    return
  }

  playBrowserSpeech(auto)
}

watch(
  data,
  (val) => {
    if (val && canPlayDjMent.value) {
      setTimeout(() => playDjMent(true), 500)
    }
  },
  { immediate: false }
)

async function saveAsPlaylist() {
  savingPlaylist.value = true
  try {
    const created = await playlist.createFromRadio({
      title: `${data.value.mood} ${data.value.genre} 라디오`,
      moodTag: data.value.desiredMood || data.value.mood,
      songs: songs.value,
    })
    ui.success("플레이리스트로 저장했어요!")
    router.push({ name: "playlist-detail", params: { id: created.playlistId } })
  } catch {
    ui.error("플레이리스트 저장에 실패했어요.")
  } finally {
    savingPlaylist.value = false
  }
}

onMounted(load)
onBeforeUnmount(stopDjMent)
</script>

<template>
  <AppShell>
    <LoadingOverlay v-if="loading" message="라디오를 불러오는 중..." />

    <div v-else-if="data" class="result-wrap">
      <section class="player-card">
        <div class="player-bg" aria-hidden="true"></div>
        <div class="player-inner">
          <div class="player-meta">
            <span class="badge">{{ data.generation }}</span>
            <span class="badge">{{ data.genre }}</span>
            <span class="badge badge-mood">{{ data.mood }}</span>
          </div>
          <h1 class="player-title text-balance">
            {{ data.mood }} · {{ data.genre }} 라디오
          </h1>
          <p v-if="data.situation" class="player-sub text-pretty">{{ data.situation }}</p>

          <NeonWaveform :active="speaking" :bars="36" class="player-wave" />

          <div class="dj-box">
            <div class="dj-head">
              <span class="dj-tag">DJ MENT</span>
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

          <div class="player-actions">
            <BaseButton
              v-if="data.playlistId"
              size="lg"
              :loading="addingAll"
              :disabled="!songs.length"
              @click="addAllToPlaylist"
            >
              추천곡 전체 담기
            </BaseButton>
            <BaseButton
              variant="outline"
              size="lg"
              :loading="savingPlaylist"
              @click="saveAsPlaylist"
            >
              플레이리스트로 저장
            </BaseButton>
          </div>
          <p v-if="batchMessage" class="batch-msg">{{ batchMessage }}</p>
        </div>
      </section>

      <section class="tracklist">
        <div class="tracklist-head">
          <h2 class="section-title">수록곡 {{ songs.length }}</h2>
          <RouterLink :to="{ name: 'radio-story' }" class="link-cyan">새 라디오 만들기</RouterLink>
        </div>
        <p class="text-muted tracklist-hint">
          재생 버튼을 누르면 해당 곡 아래에서 영상이 펼쳐져요.
        </p>
        <div class="track-items">
          <TrackRow
            v-for="(s, i) in songs"
            :key="s.id || s.songId || i"
            :song="s"
            :index="i"
            :playing="isPlaying(s)"
            @play="togglePlay"
          />
        </div>
      </section>
    </div>
  </AppShell>
</template>

<style scoped>
.result-wrap {
  max-width: 860px;
  margin: 0 auto;
  padding: 32px 0 80px;
  display: flex;
  flex-direction: column;
  gap: 32px;
}
.player-card {
  position: relative;
  border-radius: var(--radius-lg);
  border: 1px solid var(--card-border-strong);
  overflow: hidden;
  background: var(--card-bg);
}
.player-bg {
  position: absolute;
  inset: 0;
  background: var(--grad-hero);
  opacity: 0.9;
}
.player-inner {
  position: relative;
  padding: 36px 32px;
}
.player-meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}
.badge-mood {
  background: var(--grad-neon);
  color: #0a0a18;
  border-color: transparent;
}
.player-title {
  font-size: clamp(24px, 5vw, 36px);
  font-weight: 800;
  line-height: 1.2;
}
.player-sub {
  color: var(--text-secondary);
  margin-top: 10px;
}
.player-wave {
  margin: 28px 0;
}
.dj-box {
  background: rgba(10, 10, 24, 0.45);
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
.player-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
  flex-wrap: wrap;
}
.batch-msg {
  margin-top: 12px;
  font-size: 13px;
  font-weight: 600;
  color: var(--neon-cyan);
}
.tracklist-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.tracklist-hint {
  font-size: 13px;
  margin-bottom: 16px;
}
.section-title {
  font-size: 20px;
  font-weight: 700;
}
.link-cyan {
  color: var(--neon-cyan);
  font-size: 14px;
  font-weight: 600;
}
.track-items {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
</style>
