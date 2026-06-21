<script setup>
import { ref } from "vue"
import { useAuthStore } from "@/stores/auth"
import { useUiStore } from "@/stores/ui"
import followApi from "@/api/followApi"

const props = defineProps({
  userId: { type: String, required: true },
  followed: { type: Boolean, default: false },
  // hide button when the story belongs to the current user
  isSelf: { type: Boolean, default: false },
})
const emit = defineEmits(["update"])

const auth = useAuthStore()
const ui = useUiStore()

const localFollowed = ref(props.followed)
const busy = ref(false)

async function toggle() {
  if (!auth.isAuthenticated) {
    ui.notify("로그인이 필요합니다.", "info")
    return
  }
  if (busy.value) return
  busy.value = true
  const prev = localFollowed.value
  // optimistic
  localFollowed.value = !prev
  try {
    if (prev) await followApi.unfollow(props.userId)
    else await followApi.follow(props.userId)
    emit("update", localFollowed.value)
  } catch (e) {
    localFollowed.value = prev // revert
    ui.error("요청 처리 중 오류가 발생했습니다.")
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <span v-if="isSelf" class="self-tag">내 사연</span>
  <button
    v-else
    type="button"
    class="follow-btn"
    :class="{ 'is-following': localFollowed }"
    :disabled="busy"
    @click.stop="toggle"
  >
    {{ localFollowed ? "팔로잉" : "팔로우" }}
  </button>
</template>

<style scoped>
.follow-btn {
  padding: 6px 14px;
  border-radius: var(--radius-pill);
  border: 1px solid var(--neon-cyan);
  background: transparent;
  color: var(--neon-cyan);
  font-size: 13px;
  font-weight: 600;
  transition: all 0.15s ease;
  flex-shrink: 0;
}
.follow-btn:hover:not(:disabled) {
  background: rgba(43, 223, 219, 0.12);
}
.follow-btn.is-following {
  border-color: var(--card-border);
  color: var(--text-muted);
}
.follow-btn:disabled {
  opacity: 0.6;
}
.self-tag {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
  padding: 6px 10px;
  flex-shrink: 0;
}
</style>
