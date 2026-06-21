<script setup>
import { ref, computed, onMounted } from "vue"
import songApi from "@/api/songApi"
import { usePlaylistStore } from "@/stores/playlist"
import { useUiStore } from "@/stores/ui"
import AppShell from "@/components/common/AppShell.vue"
import SongCard from "@/components/song/SongCard.vue"
import BaseModal from "@/components/common/BaseModal.vue"
import BaseButton from "@/components/common/BaseButton.vue"
import LoadingOverlay from "@/components/common/LoadingOverlay.vue"

const playlist = usePlaylistStore()
const ui = useUiStore()

const loading = ref(true)
const songs = ref([])
const keyword = ref("")
const activeGenre = ref("전체")
const genres = ["전체", "발라드", "댄스", "R&B", "힙합", "락", "어쿠스틱", "시티팝"]

// save-to-playlist modal
const saveModal = ref(false)
const saveTarget = ref(null)

// id of the song whose video is expanded in its card
const playingSongId = ref(null)
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

const filtered = computed(() => {
  let list = songs.value
  if (activeGenre.value !== "전체") {
    list = list.filter((s) => s.genre === activeGenre.value)
  }
  if (keyword.value.trim()) {
    const k = keyword.value.trim()
    list = list.filter(
      (s) => s.title.includes(k) || s.artist.includes(k)
    )
  }
  return list
})

async function load() {
  loading.value = true
  try {
    songs.value = await songApi.list()
    if (!playlist.playlists.length) await playlist.fetchAll()
  } finally {
    loading.value = false
  }
}

function openSave(song) {
  saveTarget.value = song
  saveModal.value = true
}

async function saveTo(pl) {
  try {
    await playlist.addItem(pl.playlistId, saveTarget.value)
    ui.success(`'${pl.title}'에 저장했어요.`)
  } catch {
    ui.error("저장에 실패했어요.")
  } finally {
    saveModal.value = false
  }
}

onMounted(load)
</script>

<template>
  <AppShell>
    <div class="song-wrap">
      <header class="song-head">
        <p class="eyebrow">SONG LIBRARY</p>
        <h1 class="title">노래 둘러보기</h1>
        <p class="subtitle text-secondary">감정에 어울리는 K-POP을 검색하고 좋아요를 눌러보세요.</p>
      </header>

      <div class="filters">
        <div class="search-box">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="7" /><path d="m21 21-4.3-4.3" />
          </svg>
          <input v-model="keyword" type="search" placeholder="곡 제목 또는 아티스트 검색" />
        </div>
        <div class="genre-row">
          <button
            v-for="g in genres"
            :key="g"
            class="chip"
            :class="{ 'is-active': activeGenre === g }"
            @click="activeGenre = g"
          >
            {{ g }}
          </button>
        </div>
      </div>

      <LoadingOverlay v-if="loading" inline message="노래를 불러오는 중..." />

      <div v-else-if="filtered.length" class="song-grid">
        <SongCard
          v-for="s in filtered"
          :key="songKey(s)"
          :song="s"
          :playing="isPlaying(s)"
          reviewable
          @play="togglePlay"
          @save="openSave"
        />
      </div>

      <p v-else class="empty text-muted">검색 결과가 없어요.</p>
    </div>

    <BaseModal v-model="saveModal" title="플레이리스트에 저장">
      <p class="save-target text-secondary">
        '{{ saveTarget?.title }}'을(를) 저장할 플레이리스트를 선택하세요.
      </p>
      <div class="save-list">
        <button
          v-for="p in playlist.playlists"
          :key="p.playlistId"
          class="save-item"
          @click="saveTo(p)"
        >
          <span class="save-dot" :style="{ background: p.coverGradient }"></span>
          <span>{{ p.title }}</span>
        </button>
        <p v-if="!playlist.playlists.length" class="text-muted">저장할 플레이리스트가 없어요.</p>
      </div>
      <template #footer>
        <BaseButton variant="ghost" @click="saveModal = false">닫기</BaseButton>
      </template>
    </BaseModal>
  </AppShell>
</template>

<style scoped>
.song-wrap {
  max-width: 920px;
  margin: 0 auto;
  padding: 32px 0 80px;
}
.song-head {
  margin-bottom: 24px;
}
.eyebrow {
  color: var(--neon-cyan);
  letter-spacing: 0.18em;
  font-size: 12px;
  font-weight: 700;
  margin-bottom: 8px;
}
.title {
  font-size: clamp(24px, 4vw, 32px);
  font-weight: 800;
}
.subtitle {
  margin-top: 10px;
}
.filters {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 28px;
}
.search-box {
  display: flex;
  align-items: center;
  gap: 10px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--card-border);
  border-radius: var(--radius-pill);
  padding: 11px 16px;
  color: var(--text-muted);
}
.search-box input {
  flex: 1;
  background: transparent;
  border: none;
  color: var(--text-primary);
  font-size: 15px;
}
.search-box input:focus {
  outline: none;
}
.genre-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.song-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 14px;
}
@media (min-width: 720px) {
  .song-grid {
    grid-template-columns: 1fr 1fr;
  }
}
.empty {
  text-align: center;
  padding: 60px 0;
}
.save-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 16px;
}
.save-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--card-border);
  background: transparent;
  color: var(--text-primary);
  font-size: 14px;
  text-align: left;
  transition: border-color 0.15s ease;
}
.save-item:hover {
  border-color: var(--neon-cyan);
}
.save-dot {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  flex-shrink: 0;
}
</style>
