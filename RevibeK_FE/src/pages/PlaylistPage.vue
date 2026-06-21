<script setup>
import { ref, onMounted } from "vue"
import { usePlaylistStore } from "@/stores/playlist"
import AppShell from "@/components/common/AppShell.vue"
import BaseButton from "@/components/common/BaseButton.vue"
import PlaylistCard from "@/components/playlist/PlaylistCard.vue"
import LoadingOverlay from "@/components/common/LoadingOverlay.vue"

const playlist = usePlaylistStore()
const loading = ref(true)

onMounted(async () => {
  try {
    await playlist.fetchAll()
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <AppShell>
    <div class="pl-wrap">
      <header class="pl-head">
        <div>
          <p class="eyebrow">MY PLAYLISTS</p>
          <h1 class="title">저장한 플레이리스트</h1>
        </div>
        <BaseButton :to="{ name: 'radio-story' }">새 라디오 만들기</BaseButton>
      </header>

      <LoadingOverlay v-if="loading" inline message="불러오는 중..." />

      <div v-else-if="playlist.playlists.length" class="pl-grid">
        <PlaylistCard
          v-for="p in playlist.playlists"
          :key="p.playlistId"
          :playlist="p"
        />
      </div>

      <div v-else class="empty">
        <p>아직 저장한 플레이리스트가 없어요.</p>
        <BaseButton :to="{ name: 'radio-story' }">라디오 만들러 가기</BaseButton>
      </div>
    </div>
  </AppShell>
</template>

<style scoped>
.pl-wrap {
  max-width: 1000px;
  margin: 0 auto;
  padding: 32px 0 80px;
}
.pl-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 28px;
  flex-wrap: wrap;
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
.pl-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 18px;
}
@media (min-width: 700px) {
  .pl-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}
@media (min-width: 1000px) {
  .pl-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}
.empty {
  text-align: center;
  padding: 80px 0;
  color: var(--text-secondary);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}
</style>
