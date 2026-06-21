<script setup>
import { ref, computed } from "vue"
import { getSongYoutubeId } from "@/utils/youtube"
import LikeButton from "./LikeButton.vue"
import YoutubePlayer from "@/components/radio/YoutubePlayer.vue"
import ReviewSection from "@/components/review/ReviewSection.vue"

const props = defineProps({
  song: { type: Object, required: true },
  showSave: { type: Boolean, default: true },
  // true when this card's video is expanded/playing
  playing: { type: Boolean, default: false },
  // enable the in-card review tab
  reviewable: { type: Boolean, default: false },
})
defineEmits(["save", "play"])

const youtubeId = computed(() => getSongYoutubeId(props.song))
const hasVideo = computed(() => !!youtubeId.value)
// /api/songs(SongDto)는 id로, 라디오 추천 응답(RecommendedSongResponseDto)은 songId로 내려와
// 두 형태를 모두 받아야 한다.
const resolvedSongId = computed(() => props.song.songId || props.song.id)

const showReviews = ref(false)
</script>

<template>
  <article class="song-card card" :class="{ 'is-playing': playing }">
    <div class="song-main">
      <div class="song-cover" aria-hidden="true">
        <span class="cover-eq"><i></i><i></i><i></i></span>
      </div>
      <div class="song-body">
        <div class="song-top">
          <div>
            <h3 class="song-title">{{ song.title }}</h3>
            <p class="song-artist text-secondary">{{ song.artist }}</p>
          </div>
          <span v-if="song.score != null" class="score-badge">
            {{ Math.round(song.score) }}
          </span>
        </div>

        <div class="song-tags">
          <span v-if="song.era" class="badge">{{ song.era }}</span>
          <span v-if="song.genre" class="badge badge-neon">{{ song.genre }}</span>
        </div>

        <p v-if="song.reason" class="song-reason text-muted">{{ song.reason }}</p>

        <div class="song-actions">
          <button
            v-if="hasVideo"
            type="button"
            class="yt-link"
            :class="{ 'is-playing': playing }"
            @click="$emit('play', song)"
          >
            <svg v-if="!playing" viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
              <path d="M23 7.5a3 3 0 0 0-2.1-2.1C19 5 12 5 12 5s-7 0-8.9.4A3 3 0 0 0 1 7.5 31 31 0 0 0 .6 12 31 31 0 0 0 1 16.5a3 3 0 0 0 2.1 2.1C5 19 12 19 12 19s7 0 8.9-.4a3 3 0 0 0 2.1-2.1 31 31 0 0 0 .4-4.5 31 31 0 0 0-.4-4.5zM10 15V9l5 3z" />
            </svg>
            <svg v-else viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M18 6 6 18M6 6l12 12" />
            </svg>
            {{ playing ? "닫기" : "재생" }}
          </button>
          <button
            v-if="showSave"
            class="save-link"
            @click="$emit('save', song)"
          >
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z" />
            </svg>
            저장
          </button>
          <button
            v-if="reviewable"
            class="review-link"
            :class="{ 'is-active': showReviews }"
            @click="showReviews = !showReviews"
          >
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8z" />
            </svg>
            리뷰
          </button>
          <span class="spacer" />
          <LikeButton :song-id="resolvedSongId" />
        </div>
      </div>
    </div>

    <!-- 카드 내부에서 펼쳐지는 YouTube 영상 -->
    <div v-if="playing && hasVideo" class="song-video">
      <YoutubePlayer :source="youtubeId" :title="song.title" autoplay />
    </div>

    <!-- 카드 내부에서 펼쳐지는 리뷰 영역 -->
    <ReviewSection v-if="reviewable && showReviews" :song-id="resolvedSongId" />
  </article>
</template>

<style scoped>
.song-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 16px;
  transition: border-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
}
.song-main {
  display: flex;
  gap: 16px;
}
.song-card:hover {
  border-color: rgba(43, 223, 219, 0.55);
  box-shadow: 0 18px 40px rgba(0, 0, 0, 0.35), 0 0 24px rgba(43, 223, 219, 0.18);
}
.song-card.is-playing {
  border-color: var(--neon-magenta);
  box-shadow: 0 0 24px rgba(255, 60, 172, 0.28);
}
.song-video {
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
.song-cover {
  width: 64px;
  height: 64px;
  border-radius: 14px;
  background: var(--grad-neon-soft);
  border: 1px solid var(--card-border);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.cover-eq {
  display: flex;
  align-items: flex-end;
  gap: 3px;
  height: 24px;
}
.cover-eq i {
  width: 4px;
  background: var(--neon-cyan);
  border-radius: 2px;
  animation: wave 1s ease-in-out infinite;
}
.cover-eq i:nth-child(1) { height: 60%; animation-delay: 0s; }
.cover-eq i:nth-child(2) { height: 100%; animation-delay: 0.2s; }
.cover-eq i:nth-child(3) { height: 45%; animation-delay: 0.4s; }
.song-body {
  flex: 1;
  min-width: 0;
}
.song-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}
.song-title {
  font-size: 16px;
  margin-bottom: 2px;
}
.song-artist {
  font-size: 13px;
}
.score-badge {
  font-size: 12px;
  font-weight: 700;
  color: var(--neon-cyan);
  border: 1px solid var(--neon-cyan);
  border-radius: var(--radius-pill);
  padding: 3px 9px;
  flex-shrink: 0;
}
.song-tags {
  display: flex;
  gap: 6px;
  margin: 10px 0;
  flex-wrap: wrap;
}
.song-reason {
  font-size: 13px;
  line-height: 1.5;
  margin-bottom: 12px;
}
.song-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}
.spacer {
  flex: 1;
}
.yt-link,
.save-link,
.review-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  padding: 7px 12px;
  border-radius: var(--radius-pill);
  border: 1px solid var(--card-border);
  background: transparent;
  color: var(--text-secondary);
  transition: all 0.15s ease;
}
.yt-link:hover {
  color: var(--neon-magenta);
  border-color: var(--neon-magenta);
}
.yt-link.is-playing {
  color: var(--neon-magenta);
  border-color: var(--neon-magenta);
  background: rgba(255, 60, 172, 0.1);
}
.save-link:hover {
  color: var(--neon-cyan);
  border-color: var(--neon-cyan);
}
.review-link:hover,
.review-link.is-active {
  color: var(--neon-cyan);
  border-color: var(--neon-cyan);
}
.review-link.is-active {
  background: rgba(43, 223, 219, 0.1);
}
</style>
