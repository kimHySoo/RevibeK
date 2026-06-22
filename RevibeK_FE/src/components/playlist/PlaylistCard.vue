<script setup>
import { computed } from "vue"

const props = defineProps({
  playlist: { type: Object, required: true },
})

const count = computed(() => props.playlist.items?.length || 0)
</script>

<template>
  <RouterLink
    :to="{ name: 'playlist-detail', params: { id: playlist.playlistId } }"
    class="pl-card"
  >
    <div class="pl-cover" :style="{ background: playlist.coverGradient }">
      <span class="pl-count">{{ count }}곡</span>
    </div>
    <div class="pl-body">
      <p class="pl-title">{{ playlist.title }}</p>
      <span v-if="playlist.moodTag" class="badge">{{ playlist.moodTag }}</span>
    </div>
  </RouterLink>
</template>

<style scoped>
.pl-card {
  display: block;
  border-radius: var(--radius-md);
  border: 1px solid var(--card-border);
  overflow: hidden;
  background: var(--card-bg);
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
}
.pl-card:hover {
  transform: translateY(-4px);
  border-color: rgba(43, 223, 219, 0.55);
  box-shadow: 0 18px 40px rgba(0, 0, 0, 0.35), 0 0 24px rgba(43, 223, 219, 0.18);
}
.pl-cover {
  aspect-ratio: 1.4 / 1;
  position: relative;
  display: flex;
  align-items: flex-end;
  padding: 12px;
}
.pl-count {
  font-size: 12px;
  font-weight: 700;
  color: #0a0a18;
  background: rgba(255, 255, 255, 0.85);
  padding: 3px 10px;
  border-radius: var(--radius-pill);
}
.pl-body {
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.pl-title {
  font-size: 15px;
  font-weight: 700;
  line-height: 1.35;
}
</style>
