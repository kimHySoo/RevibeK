<script setup>
import { ref, computed, onMounted } from "vue"
import { useRoute, useRouter } from "vue-router"
import { usePlaylistStore } from "@/stores/playlist"
import { useUiStore } from "@/stores/ui"
import AppShell from "@/components/common/AppShell.vue"
import BaseButton from "@/components/common/BaseButton.vue"
import BaseModal from "@/components/common/BaseModal.vue"
import TrackRow from "@/components/song/TrackRow.vue"
import PlaylistEditModal from "@/components/playlist/PlaylistEditModal.vue"

const route = useRoute()
const router = useRouter()
const playlist = usePlaylistStore()
const ui = useUiStore()

const loading = ref(true)
const error = ref("")
const confirmDelete = ref(false)
const editOpen = ref(false)

const pl = computed(() => playlist.current)
const items = computed(() => pl.value?.items || [])

// id of the track whose video is currently expanded in its row
const playingSongId = ref(null)

function songKey(song) {
  return song?.itemId || song?.songId || song?.id
}
function isPlaying(song) {
  return playingSongId.value != null && playingSongId.value === songKey(song)
}
function togglePlay(song) {
  const key = songKey(song)
  playingSongId.value = playingSongId.value === key ? null : key
}

const playlistId = computed(() => route.params.id)

async function load() {
  loading.value = true
  error.value = ""
  try {
    const result = await playlist.fetchOne(playlistId.value)
    if (!result) {
      error.value = "플레이리스트를 찾을 수 없습니다."
    }
  } catch {
    error.value = "플레이리스트를 불러오지 못했습니다."
  } finally {
    loading.value = false
  }
}

async function removeTrack(song) {
  try {
    await playlist.removeItem(pl.value.playlistId, song.itemId)
    if (isPlaying(song)) playingSongId.value = null
    ui.success("곡을 삭제했어요.")
  } catch {
    ui.error("곡 삭제에 실패했어요.")
  }
}

async function deletePlaylist() {
  try {
    await playlist.remove(pl.value.playlistId)
    ui.success("플레이리스트를 삭제했어요.")
    router.push({ name: "playlists" })
  } catch {
    ui.error("삭제에 실패했어요.")
  } finally {
    confirmDelete.value = false
  }
}

function goList() {
  router.push({ name: "playlists" })
}

function makeAnother() {
  router.push({ name: "radio-story" })
}

onMounted(load)
</script>

<template>
  <AppShell>
    <div class="detail-page">
      <!-- 로딩 상태 -->
      <section v-if="loading" class="state-card">
        <span class="state-spinner" aria-hidden="true"></span>
        <p>플레이리스트를 불러오는 중...</p>
      </section>

      <!-- 에러 상태 -->
      <section v-else-if="error" class="state-card state-error">
        <p class="state-title">{{ error }}</p>
        <p class="text-muted">잠시 후 다시 시도해주세요.</p>
        <div class="state-actions">
          <BaseButton @click="load">다시 시도</BaseButton>
          <BaseButton variant="outline" @click="goList">목록으로</BaseButton>
        </div>
      </section>

      <!-- 정상 상태 -->
      <div v-else-if="pl" class="detail-wrap">
        <section class="detail-hero">
          <div
            class="hero-cover"
            :style="{ background: pl.coverGradient || 'var(--grad-neon)' }"
          ></div>
          <div class="hero-info">
            <p class="eyebrow">PLAYLIST DETAIL</p>
            <h1 class="hero-title text-balance">{{ pl.title }}</h1>
            <div class="hero-meta">
              <span class="badge badge-mood">
                {{ pl.moodTag || "오늘의 감성 플레이리스트" }}
              </span>
              <span class="badge">{{ pl.isPublic ? "공개" : "비공개" }}</span>
              <span class="text-muted">{{ items.length }}곡</span>
            </div>
            <div class="hero-actions">
              <BaseButton v-if="items.length" @click="togglePlay(items[0])">
                재생
              </BaseButton>
              <BaseButton variant="outline" @click="editOpen = true">
                수정
              </BaseButton>
              <BaseButton variant="outline" @click="makeAnother">
                새 사연으로 다시 만들기
              </BaseButton>
              <BaseButton variant="danger" @click="confirmDelete = true">
                삭제
              </BaseButton>
            </div>
          </div>
        </section>

        <!-- 빈 플레이리스트 상태 -->
        <section v-if="!items.length" class="empty-card">
          <p class="empty-title">아직 플레이리스트에 담긴 곡이 없습니다.</p>
          <BaseButton variant="outline" @click="makeAnother">
            새 사연으로 라디오 만들기
          </BaseButton>
        </section>

        <!-- 곡 목록 (YouTube 영상 목록) -->
        <section v-if="items.length" class="track-items">
          <TrackRow
            v-for="(s, i) in items"
            :key="s.itemId || i"
            :song="s"
            :index="i"
            removable
            :playing="isPlaying(s)"
            @play="togglePlay"
            @remove="removeTrack"
          />
        </section>

        <div class="page-actions">
          <BaseButton variant="ghost" @click="goList">목록으로</BaseButton>
        </div>
      </div>
    </div>

    <PlaylistEditModal v-model="editOpen" :playlist="pl" />

    <BaseModal v-model="confirmDelete" title="플레이리스트 삭제">
      <p>정말 이 플레이리스트를 삭제할까요? 되돌릴 수 없어요.</p>
      <template #footer>
        <BaseButton variant="ghost" @click="confirmDelete = false">취소</BaseButton>
        <BaseButton variant="danger" @click="deletePlaylist">삭제</BaseButton>
      </template>
    </BaseModal>
  </AppShell>
</template>

<style scoped>
.detail-page {
  max-width: 880px;
  margin: 0 auto;
  padding: 32px 0 80px;
}

/* state cards */
.state-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  text-align: center;
  padding: 64px 32px;
  border: 1px solid var(--card-border);
  border-radius: var(--radius-lg);
  background: var(--card-bg);
}
.state-title {
  font-size: 18px;
  font-weight: 700;
}
.state-actions {
  display: flex;
  gap: 12px;
  margin-top: 8px;
  flex-wrap: wrap;
  justify-content: center;
}
.state-spinner {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  border: 3px solid var(--card-border-strong);
  border-top-color: var(--neon-cyan);
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* hero */
.detail-wrap {
  display: flex;
  flex-direction: column;
  gap: 28px;
}
.detail-hero {
  display: flex;
  gap: 24px;
  align-items: flex-end;
  flex-wrap: wrap;
}
.hero-cover {
  width: 180px;
  height: 180px;
  border-radius: var(--radius-md);
  flex-shrink: 0;
  box-shadow: var(--glow-magenta);
}
.hero-info {
  flex: 1;
  min-width: 240px;
}
.eyebrow {
  color: var(--neon-cyan);
  letter-spacing: 0.18em;
  font-size: 12px;
  font-weight: 700;
  margin-bottom: 8px;
}
.hero-title {
  font-size: clamp(26px, 5vw, 40px);
  font-weight: 800;
  line-height: 1.15;
}
.hero-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 14px;
  flex-wrap: wrap;
}
.badge-mood {
  background: var(--grad-neon);
  color: #ffffff;
  border-color: transparent;
}
.hero-actions {
  display: flex;
  gap: 12px;
  margin-top: 20px;
  flex-wrap: wrap;
}

/* tracks */
.track-items {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.empty-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  text-align: center;
  padding: 56px 32px;
  border: 1px dashed var(--card-border-strong);
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.02);
}
.empty-title {
  font-size: 16px;
  color: var(--text-secondary);
}
.page-actions {
  display: flex;
  justify-content: center;
}
</style>
