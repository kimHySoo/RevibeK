<script setup>
import { ref, onMounted } from "vue"
import publicRadioApi from "@/api/publicRadioApi"
import AppShell from "@/components/common/AppShell.vue"
import PublicRadioCard from "@/components/radio/PublicRadioCard.vue"
import PublicRadioDetailModal from "@/components/radio/PublicRadioDetailModal.vue"

const sort = ref("latest")
const loading = ref(true)
const error = ref(false)
const stories = ref([])

const detailOpen = ref(false)
const detailId = ref("")

async function load(nextSort = sort.value) {
  loading.value = true
  error.value = false
  try {
    const list = await publicRadioApi.getFeed(nextSort)
    stories.value = Array.isArray(list) ? list : []
  } catch {
    // popular fallback → latest
    if (nextSort === "popular") {
      try {
        stories.value = await publicRadioApi.getFeed("latest")
        sort.value = "latest"
        loading.value = false
        return
      } catch {
        error.value = true
      }
    } else {
      error.value = true
    }
  } finally {
    loading.value = false
  }
}

function changeSort(next) {
  if (sort.value === next) return
  sort.value = next
  load(next)
}

function openDetail(story) {
  detailId.value = story.radioSessionId
  detailOpen.value = true
}

// keep feed cards in sync when like/follow toggled in card or modal
function onLikeUpdate({ id, liked, likeCount }) {
  const s = stories.value.find((x) => x.radioSessionId === id)
  if (s) {
    s.liked = liked
    s.likeCount = likeCount
  }
  publicRadioApi._applyLocal(id, { liked, likeCount })
}
function onFollowUpdate({ id, followed }) {
  const target = stories.value.find((x) => x.radioSessionId === id)
  const userId = target?.userId
  stories.value.forEach((s) => {
    if (s.userId === userId) s.followed = followed
  })
}

onMounted(() => load())
</script>

<template>
  <AppShell>
    <div class="container revibening">
      <header class="rv-head">
        <div>
          <h1 class="rv-title text-balance">리바이브닝</h1>
          <p class="text-muted">다른 사람들이 공개한 라디오 사연을 둘러보세요.</p>
        </div>
        <div class="sort-tabs" role="tablist" aria-label="정렬">
          <button
            class="sort-tab"
            :class="{ 'is-active': sort === 'latest' }"
            role="tab"
            :aria-selected="sort === 'latest'"
            @click="changeSort('latest')"
          >
            최신순
          </button>
          <button
            class="sort-tab"
            :class="{ 'is-active': sort === 'popular' }"
            role="tab"
            :aria-selected="sort === 'popular'"
            @click="changeSort('popular')"
          >
            인기순
          </button>
        </div>
      </header>

      <p v-if="loading" class="state text-muted">불러오는 중입니다...</p>

      <p v-else-if="error" class="state text-muted">
        데이터를 불러오지 못했습니다.<br />잠시 후 다시 시도해주세요.
      </p>

      <p v-else-if="!stories.length" class="state text-muted">
        아직 공개된 라디오 사연이 없습니다.<br />
        마이페이지에서 내 라디오를 공개해보세요.
      </p>

      <div v-else class="rv-grid">
        <PublicRadioCard
          v-for="s in stories"
          :key="s.radioSessionId"
          :story="s"
          @open="openDetail"
          @like-update="onLikeUpdate"
          @follow-update="onFollowUpdate"
        />
      </div>
    </div>

    <PublicRadioDetailModal
      v-model="detailOpen"
      :radio-session-id="detailId"
      @like-update="onLikeUpdate"
      @follow-update="onFollowUpdate"
    />
  </AppShell>
</template>

<style scoped>
.revibening {
  padding-top: 32px;
  padding-bottom: 64px;
}
.rv-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 28px;
}
.rv-title {
  font-size: 30px;
  font-weight: 800;
  margin-bottom: 4px;
}
.sort-tabs {
  display: flex;
  gap: 4px;
  background: rgba(255, 255, 255, 0.04);
  border-radius: var(--radius-pill);
  padding: 4px;
}
.sort-tab {
  padding: 8px 18px;
  border-radius: var(--radius-pill);
  border: none;
  background: transparent;
  color: var(--text-muted);
  font-size: 14px;
  font-weight: 600;
  transition: all 0.15s ease;
}
.sort-tab.is-active {
  background: var(--grad-neon);
  color: #0a0a18;
}
.state {
  text-align: center;
  padding: 64px 0;
  line-height: 1.7;
}
.rv-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 18px;
}
@media (max-width: 760px) {
  .rv-grid {
    grid-template-columns: 1fr;
  }
}
</style>
