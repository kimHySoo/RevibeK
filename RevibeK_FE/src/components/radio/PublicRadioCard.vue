<script setup>
import { computed } from "vue"
import { useAuthStore } from "@/stores/auth"
import RadioLikeButton from "./RadioLikeButton.vue"
import FollowButton from "@/components/social/FollowButton.vue"

const props = defineProps({
  story: { type: Object, required: true },
})
const emit = defineEmits(["open", "like-update", "follow-update"])

const auth = useAuthStore()

const isSelf = computed(
  () => auth.user?.id != null && auth.user.id === props.story.userId
)
const songs = computed(() => props.story.recommendedSongs || [])

function publishedLabel(iso) {
  if (!iso) return ""
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return ""
  return d.toLocaleDateString("ko-KR", {
    year: "numeric",
    month: "long",
    day: "numeric",
  })
}
</script>

<template>
  <article class="public-card card">
    <header class="pc-head">
      <div class="pc-tags">
        <span v-if="story.mood" class="badge badge-neon">{{ story.mood }}</span>
        <span v-if="story.situation" class="badge">{{ story.situation }}</span>
        <span v-if="story.generation" class="badge">{{ story.generation }}</span>
      </div>
      <span v-if="publishedLabel(story.publishedAt)" class="pc-date text-muted">
        {{ publishedLabel(story.publishedAt) }}
      </span>
    </header>

    <h3 class="pc-title">{{ story.title }}</h3>

    <div class="pc-author">
      <span class="pc-nick text-secondary">{{ story.userNickname }}</span>
      <FollowButton
        :user-id="story.userId"
        :followed="story.followed"
        :is-self="isSelf"
        @update="$emit('follow-update', { id: story.radioSessionId, followed: $event })"
      />
    </div>

    <p class="pc-story text-secondary">{{ story.story }}</p>

    <div v-if="story.djComment" class="pc-dj">
      <span class="pc-dj-label">DJ 멘트</span>
      <p class="text-muted">{{ story.djComment }}</p>
    </div>

    <div v-if="songs.length" class="pc-songs">
      <span class="pc-songs-label text-muted">추천곡</span>
      <ul class="pc-song-list">
        <li v-for="(s, i) in songs.slice(0, 3)" :key="s.songId || i">
          {{ s.artist }} - {{ s.title }}
        </li>
      </ul>
    </div>

    <footer class="pc-foot">
      <RadioLikeButton
        :radio-session-id="story.radioSessionId"
        :liked="story.liked"
        :like-count="story.likeCount"
        @update="$emit('like-update', { id: story.radioSessionId, ...$event })"
      />
      <button type="button" class="pc-detail" @click="$emit('open', story)">
        자세히 보기
      </button>
    </footer>
  </article>
</template>

<style scoped>
.public-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 20px;
}
.pc-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}
.pc-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.pc-date {
  font-size: 12px;
  flex-shrink: 0;
}
.pc-title {
  font-size: 18px;
  font-weight: 700;
}
.pc-author {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.pc-nick {
  font-size: 14px;
  font-weight: 600;
}
.pc-story {
  font-size: 14px;
  line-height: 1.6;
}
.pc-dj {
  border-left: 2px solid var(--neon-cyan);
  padding-left: 12px;
}
.pc-dj-label {
  font-size: 12px;
  font-weight: 700;
  color: var(--neon-cyan);
}
.pc-dj p {
  font-size: 13px;
  line-height: 1.5;
  margin-top: 2px;
}
.pc-songs-label {
  font-size: 12px;
  font-weight: 700;
}
.pc-song-list {
  list-style: none;
  margin: 6px 0 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13px;
  color: var(--text-secondary);
}
.pc-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 4px;
}
.pc-detail {
  padding: 7px 16px;
  border-radius: var(--radius-pill);
  border: 1px solid var(--card-border);
  background: transparent;
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 600;
  transition: all 0.15s ease;
}
.pc-detail:hover {
  border-color: var(--neon-cyan);
  color: var(--neon-cyan);
}
</style>
