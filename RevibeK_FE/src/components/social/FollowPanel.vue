<script setup>
import { ref, computed, onMounted } from "vue"
import { useUiStore } from "@/stores/ui"
import followApi from "@/api/followApi"
import BaseButton from "@/components/common/BaseButton.vue"

const ui = useUiStore()

const sub = ref("following") // following | followers
const loading = ref(true)
const following = ref([])
const followers = ref([])

const list = computed(() =>
  sub.value === "following" ? following.value : followers.value
)

async function load() {
  loading.value = true
  try {
    const [a, b] = await Promise.all([
      followApi.getFollowing(),
      followApi.getFollowers(),
    ])
    following.value = a
    followers.value = b
  } catch {
    ui.error("요청 처리 중 오류가 발생했습니다.")
  } finally {
    loading.value = false
  }
}

async function toggleFollow(user) {
  try {
    if (user.following) {
      await followApi.unfollow(user.userId)
      ui.success("언팔로우했어요.")
    } else {
      await followApi.follow(user.userId)
      ui.success("팔로우했어요.")
    }
    await load()
  } catch (e) {
    const msg = e?.response?.data?.message || "요청 처리 중 오류가 발생했습니다."
    ui.error(msg)
  }
}

onMounted(load)
</script>

<template>
  <div class="follow-panel">
    <div class="sub-tabs">
      <button
        class="sub-tab"
        :class="{ 'is-active': sub === 'following' }"
        @click="sub = 'following'"
      >
        팔로잉 {{ following.length }}
      </button>
      <button
        class="sub-tab"
        :class="{ 'is-active': sub === 'followers' }"
        @click="sub = 'followers'"
      >
        팔로워 {{ followers.length }}
      </button>
    </div>

    <p v-if="loading" class="empty text-muted">불러오는 중...</p>

    <div v-else-if="list.length" class="user-list">
      <div v-for="u in list" :key="u.userId" class="user-card card">
        <div class="user-avatar" aria-hidden="true">{{ (u.nickname || "U").charAt(0) }}</div>
        <div class="user-info">
          <p class="user-name">{{ u.nickname }}</p>
          <p v-if="u.favoriteGenre" class="user-meta text-muted">{{ u.favoriteGenre }}</p>
        </div>
        <BaseButton
          size="sm"
          :variant="u.following ? 'outline' : 'primary'"
          @click="toggleFollow(u)"
        >
          {{ u.following ? "언팔로우" : "팔로우" }}
        </BaseButton>
      </div>
    </div>

    <p v-else class="empty text-muted">
      {{ sub === "following" ? "아직 팔로잉한 사용자가 없어요." : "아직 팔로워가 없어요." }}
    </p>
  </div>
</template>

<style scoped>
.sub-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 18px;
}
.sub-tab {
  background: transparent;
  border: 1px solid var(--card-border);
  border-radius: var(--radius-pill);
  color: var(--text-muted);
  font-size: 13px;
  font-weight: 600;
  padding: 7px 14px;
  transition: all 0.15s ease;
}
.sub-tab.is-active {
  color: var(--text-primary);
  border-color: var(--neon-magenta);
  background: rgba(255, 60, 172, 0.08);
}
.user-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.user-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
}
.user-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: var(--grad-neon-soft);
  border: 1px solid var(--card-border);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: 18px;
  flex-shrink: 0;
}
.user-info {
  flex: 1;
  min-width: 0;
}
.user-name {
  font-size: 15px;
  font-weight: 700;
}
.user-meta {
  font-size: 12px;
  margin-top: 2px;
}
.empty {
  text-align: center;
  padding: 48px 0;
}
</style>
