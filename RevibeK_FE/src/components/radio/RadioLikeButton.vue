<script setup>
import { ref } from "vue"
import { useAuthStore } from "@/stores/auth"
import { useUiStore } from "@/stores/ui"
import radioLikeApi from "@/api/radioLikeApi"

// Radio STORY like button — uses /api/radio/{id}/likes,
// NOT the song like endpoints (/api/likes/**).
const props = defineProps({
  radioSessionId: { type: String, required: true },
  liked: { type: Boolean, default: false },
  likeCount: { type: Number, default: 0 },
})
const emit = defineEmits(["update"])

const auth = useAuthStore()
const ui = useUiStore()

const localLiked = ref(props.liked)
const localCount = ref(props.likeCount)
const busy = ref(false)

async function toggle() {
  if (!auth.isAuthenticated) {
    ui.notify("로그인이 필요합니다.", "info")
    return
  }
  if (busy.value) return
  busy.value = true
  const prevLiked = localLiked.value
  const prevCount = localCount.value
  // optimistic update
  localLiked.value = !prevLiked
  localCount.value = prevCount + (prevLiked ? -1 : 1)
  try {
    if (prevLiked) await radioLikeApi.unlike(props.radioSessionId)
    else await radioLikeApi.like(props.radioSessionId)
    emit("update", { liked: localLiked.value, likeCount: localCount.value })
  } catch (e) {
    localLiked.value = prevLiked // revert
    localCount.value = prevCount
    ui.error("요청 처리 중 오류가 발생했습니다.")
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <button
    type="button"
    class="radio-like"
    :class="{ 'is-liked': localLiked }"
    :disabled="busy"
    :aria-pressed="localLiked"
    aria-label="사연 좋아요"
    @click.stop="toggle"
  >
    <svg
      viewBox="0 0 24 24"
      width="16"
      height="16"
      :fill="localLiked ? 'currentColor' : 'none'"
      stroke="currentColor"
      stroke-width="2"
    >
      <path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1-1.1a5.5 5.5 0 0 0-7.8 7.8l1.1 1L12 21l7.7-7.6 1.1-1a5.5 5.5 0 0 0 0-7.8z" />
    </svg>
    <span class="count">{{ localCount }}</span>
  </button>
</template>

<style scoped>
.radio-like {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border-radius: var(--radius-pill);
  border: 1px solid var(--card-border);
  background: transparent;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 600;
  transition: all 0.15s ease;
}
.radio-like:hover:not(:disabled) {
  border-color: var(--neon-magenta);
  color: var(--neon-magenta);
}
.radio-like.is-liked {
  border-color: var(--neon-magenta);
  color: var(--neon-magenta);
  background: rgba(255, 60, 172, 0.1);
}
.radio-like:disabled {
  opacity: 0.6;
}
.count {
  font-variant-numeric: tabular-nums;
}
</style>
