<script setup>
import { computed } from "vue"
import { getSongYoutubeId } from "@/utils/youtube"
import YoutubePlayer from "@/components/radio/YoutubePlayer.vue"

const props = defineProps({
  song: { type: Object, required: true },
  index: { type: Number, default: 0 },
  removable: { type: Boolean, default: false },
  // true when this row's video is expanded/playing
  playing: { type: Boolean, default: false },
})
defineEmits(["remove", "play"])

const youtubeId = computed(() => getSongYoutubeId(props.song))
const hasVideo = computed(() => !!youtubeId.value)
</script>

<template>
  <div class="track-cell" :class="{ 'is-playing': playing }">
    <div class="track-row">
      <span class="track-index">{{ String(index + 1).padStart(2, "0") }}</span>
      <div class="track-info">
        <p class="track-title">{{ song.title }}</p>
        <p class="track-artist text-muted">{{ song.artist }}</p>
      </div>
      <span v-if="song.generation || song.era" class="badge">{{ song.generation || song.era }}</span>
      <span v-if="song.genre" class="badge">{{ song.genre }}</span>
      <button
        v-if="hasVideo"
        type="button"
        class="track-yt"
        :class="{ 'is-playing': playing }"
        :aria-label="playing ? '영상 닫기' : '여기서 재생'"
        @click="$emit('play', song)"
      >
        <svg v-if="!playing" viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M8 5v14l11-7z" /></svg>
        <svg v-else viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6 6 18M6 6l12 12" /></svg>
      </button>
      <button
        v-if="removable"
        class="track-remove"
        aria-label="곡 삭제"
        @click="$emit('remove', song)"
      >
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6 6 18M6 6l12 12" /></svg>
      </button>
    </div>

    <!-- 행 아래에서 펼쳐지는 YouTube 영상 -->
    <div v-if="playing && hasVideo" class="track-video">
      <YoutubePlayer :source="youtubeId" :title="song.title" autoplay />
    </div>
  </div>
</template>

<style scoped>
.track-cell {
  border-radius: var(--radius-sm);
  border: 1px solid transparent;
  transition: all 0.15s ease;
}
.track-cell:hover {
  background: rgba(255, 255, 255, 0.03);
  border-color: var(--card-border);
}
.track-cell.is-playing {
  border-color: var(--neon-magenta);
  background: rgba(255, 60, 172, 0.08);
}
.track-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 14px;
}
.track-video {
  padding: 0 14px 14px;
  animation: video-reveal 0.25s ease;
}
@keyframes video-reveal {
  from {
    opacity: 0;
    transform: translateY(-6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
.track-index {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-muted);
  width: 24px;
}
.track-info {
  flex: 1;
  min-width: 0;
}
.track-title {
  font-size: 15px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.track-artist {
  font-size: 12px;
}
.track-yt,
.track-remove {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid var(--card-border);
  background: transparent;
  color: var(--text-secondary);
}
.track-yt:hover,
.track-yt.is-playing {
  color: var(--neon-magenta);
  border-color: var(--neon-magenta);
}
.track-yt.is-playing {
  background: rgba(255, 60, 172, 0.1);
}
.track-remove:hover {
  color: var(--danger);
  border-color: var(--danger);
}
</style>
