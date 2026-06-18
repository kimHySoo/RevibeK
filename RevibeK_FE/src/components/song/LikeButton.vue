<script setup>
import { ref, onMounted } from "vue"
import likeApi from "@/api/likeApi"
import { useUiStore } from "@/stores/ui"

const props = defineProps({
  songId: { type: String, required: true },
})
const ui = useUiStore()
const liked = ref(false)
const busy = ref(false)

onMounted(async () => {
  try {
    const res = await likeApi.status(props.songId)
    liked.value = !!res.liked
  } catch {
    /* ignore */
  }
})

async function toggle() {
  if (busy.value) return
  busy.value = true
  try {
    if (liked.value) {
      await likeApi.unlike(props.songId)
      liked.value = false
    } else {
      await likeApi.like(props.songId)
      liked.value = true
      ui.success("좋아요한 곡에 추가했어요.")
    }
  } catch {
    ui.error("좋아요 처리에 실패했어요.")
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <button
    class="like-btn"
    :class="{ 'is-liked': liked }"
    :aria-pressed="liked"
    :aria-label="liked ? '좋아요 취소' : '좋아요'"
    @click.stop="toggle"
  >
    <svg viewBox="0 0 24 24" width="20" height="20" :fill="liked ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2">
      <path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.6l-1-1a5.5 5.5 0 0 0-7.8 7.8l1 1L12 21l7.8-7.6 1-1a5.5 5.5 0 0 0 0-7.8z" />
    </svg>
  </button>
</template>

<style scoped>
.like-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: rgba(255, 255, 255, 0.03);
  color: #c9c9e8;
  transition: all 0.18s ease;
}
.like-btn:hover {
  color: #ff3cac;
  border-color: #ff3cac;
  box-shadow: 0 0 14px rgba(255, 60, 172, 0.25);
}
.like-btn:focus-visible {
  outline: 2px solid var(--neon-cyan);
  outline-offset: 3px;
}
.like-btn.is-liked {
  background: rgba(255, 60, 172, 0.16);
  color: #ff3cac;
  border-color: #ff3cac;
}
</style>
