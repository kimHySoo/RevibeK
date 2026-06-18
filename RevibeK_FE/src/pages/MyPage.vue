<script setup>
import { ref, computed, watch, onMounted } from "vue"
import { useRoute, useRouter } from "vue-router"
import { useAuthStore } from "@/stores/auth"
import { useRadioStore } from "@/stores/radio"
import { useUiStore } from "@/stores/ui"
import likeApi from "@/api/likeApi"
import publicRadioApi from "@/api/publicRadioApi"
import AppShell from "@/components/common/AppShell.vue"
import BaseButton from "@/components/common/BaseButton.vue"
import TrackRow from "@/components/song/TrackRow.vue"
import LoadingOverlay from "@/components/common/LoadingOverlay.vue"
import UserEditModal from "@/components/user/UserEditModal.vue"
import DeleteAccountModal from "@/components/user/DeleteAccountModal.vue"
import MyReviewsPanel from "@/components/review/MyReviewsPanel.vue"
import FollowPanel from "@/components/social/FollowPanel.vue"
import PlanPanel from "@/components/plan/PlanPanel.vue"

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const radio = useRadioStore()
const ui = useUiStore()

// Map between the ?tab= query value and the internal tab key.
const QUERY_TO_TAB = {
  likes: "liked",
  radio: "history",
  reviews: "reviews",
  follow: "follow",
  plan: "plan",
}
const TAB_TO_QUERY = {
  liked: "likes",
  history: "radio",
  reviews: "reviews",
  follow: "follow",
  plan: "plan",
}
function tabFromQuery(q) {
  return QUERY_TO_TAB[q] || "liked"
}
function queryFromTab(t) {
  return TAB_TO_QUERY[t] || "likes"
}

// profile edit / account deletion modals
const editModal = ref(false)
const deleteModal = ref(false)

function onProfileUpdated() {
  // auth store is already updated by the modal; nothing else needed
}

async function onAccountDeleted() {
  await router.push({ name: "landing" })
}

const tab = ref(tabFromQuery(route.query.tab))
const loading = ref(true)
const likedSongs = ref([])

// id of the liked song whose video is expanded in its row
const playingSongId = ref(null)
function songKey(song) {
  return song?.songId || song?.id
}
function isPlaying(song) {
  return playingSongId.value != null && playingSongId.value === songKey(song)
}
function togglePlay(song) {
  const key = songKey(song)
  playingSongId.value = playingSongId.value === key ? null : key
}

// Click a tab: update state + keep the URL query in sync.
function selectTab(next) {
  tab.value = next
  const q = queryFromTab(next)
  if (route.query.tab !== q) {
    router.replace({ query: { ...route.query, tab: q } })
  }
}

// React to external query changes (e.g. header links to /me?tab=radio).
watch(
  () => route.query.tab,
  (q) => {
    tab.value = tabFromQuery(q)
  }
)

const user = computed(() => auth.user || {})
const pref = computed(() => user.value.preference || {})
const history = computed(() => radio.history)

const joinedLabel = computed(() => {
  if (!user.value.joinedAt) return ""
  return new Date(user.value.joinedAt).toLocaleDateString("ko-KR")
})

async function load() {
  loading.value = true
  try {
    const [likes] = await Promise.all([
      likeApi.likedSongs(),
      radio.fetchHistory(),
      auth.user ? Promise.resolve() : auth.fetchMe(),
    ])
    likedSongs.value = likes
  } finally {
    loading.value = false
  }
}

function openHistory(item) {
  router.push({ name: "radio-result", params: { id: item.radioSessionId } })
}

const togglingId = ref(null)

function publishedLabel(iso) {
  if (!iso) return ""
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return ""
  return d.toLocaleDateString("ko-KR")
}

async function togglePublic(item) {
  if (!auth.isAuthenticated) {
    ui.notify("로그인이 필요합니다.", "info")
    return
  }
  if (togglingId.value) return
  togglingId.value = item.radioSessionId
  const next = !item.isPublic
  try {
    const res = await publicRadioApi.togglePublic(item.radioSessionId, next)
    // mutate the reactive store item so the card updates immediately
    item.isPublic = res?.isPublic ?? next
    item.publishedAt = res?.publishedAt ?? (next ? new Date().toISOString() : null)
    ui.success(next ? "리바이브닝에 공개했어요." : "비공개로 전환했어요.")
  } catch {
    ui.error("요청 처리 중 오류가 발생했습니다.")
  } finally {
    togglingId.value = null
  }
}

async function logout() {
  await auth.logout()
  ui.success("로그아웃 되었어요.")
  router.push({ name: "landing" })
}

onMounted(load)
</script>

<template>
  <AppShell>
    <div class="me-wrap">
      <section class="profile card">
        <div class="avatar" aria-hidden="true">
          {{ (user.nickname || "U").charAt(0) }}
        </div>
        <div class="profile-info">
          <h1 class="nickname">{{ user.nickname || "사용자" }}</h1>
          <p class="email text-muted">{{ user.email }}</p>
          <p v-if="joinedLabel" class="joined text-muted">{{ joinedLabel }} 가입</p>
          <div class="pref-tags">
            <span v-if="pref.favoriteMood" class="badge badge-neon">{{ pref.favoriteMood }}</span>
            <span v-if="pref.favoriteEra" class="badge">{{ pref.favoriteEra }}</span>
            <span v-if="pref.favoriteGenre" class="badge">{{ pref.favoriteGenre }}</span>
          </div>
        </div>
        <div class="profile-actions">
          <BaseButton variant="outline" @click="editModal = true">회원정보 수정</BaseButton>
          <BaseButton variant="ghost" @click="logout">로그아웃</BaseButton>
        </div>
      </section>

      <div class="tabs">
        <button class="tab" :class="{ 'is-active': tab === 'liked' }" @click="selectTab('liked')">
          좋아요한 곡 {{ likedSongs.length }}
        </button>
        <button class="tab" :class="{ 'is-active': tab === 'history' }" @click="selectTab('history')">
          라디오 기록 {{ history.length }}
        </button>
        <button class="tab" :class="{ 'is-active': tab === 'reviews' }" @click="selectTab('reviews')">
          내 리뷰
        </button>
        <button class="tab" :class="{ 'is-active': tab === 'follow' }" @click="selectTab('follow')">
          팔로우
        </button>
        <button class="tab" :class="{ 'is-active': tab === 'plan' }" @click="selectTab('plan')">
          청취 계획
        </button>
      </div>

      <LoadingOverlay v-if="loading" inline message="불러오는 중..." />

      <template v-else>
        <section v-if="tab === 'liked'" class="tab-panel">
          <div v-if="likedSongs.length" class="track-items">
            <TrackRow
              v-for="(s, i) in likedSongs"
              :key="s.songId"
              :song="s"
              :index="i"
              :playing="isPlaying(s)"
              @play="togglePlay"
            />
          </div>
          <p v-else class="empty text-muted">아직 좋아요한 곡이 없어요.</p>
        </section>

        <section v-else-if="tab === 'history'" class="tab-panel">
          <div v-if="history.length" class="history-list">
            <article v-for="h in history" :key="h.radioSessionId" class="history-item card">
              <div
                class="history-body"
                role="button"
                tabindex="0"
                @click="openHistory(h)"
                @keydown.enter="openHistory(h)"
              >
                <div class="history-tags">
                  <span class="badge badge-neon">{{ h.mood }}</span>
                  <span class="badge">{{ h.era }}</span>
                  <span class="badge">{{ h.genre }}</span>
                </div>
                <p class="history-situation">{{ h.situation }}</p>
                <p class="history-ment text-muted">{{ h.djMent }}</p>
              </div>

              <div class="history-foot">
                <div class="history-status">
                  <span class="status-dot" :class="{ 'is-public': h.isPublic }" aria-hidden="true"></span>
                  <span class="status-text">{{ h.isPublic ? "공개" : "비공개" }}</span>
                  <span v-if="h.isPublic && publishedLabel(h.publishedAt)" class="status-date text-muted">
                    · {{ publishedLabel(h.publishedAt) }} 공개
                  </span>
                </div>
                <button
                  type="button"
                  class="toggle-public"
                  :class="{ 'is-public': h.isPublic }"
                  :disabled="togglingId === h.radioSessionId"
                  @click="togglePublic(h)"
                >
                  {{ h.isPublic ? "비공개로 전환" : "리바이브닝에 공개" }}
                </button>
              </div>
            </article>
          </div>
          <p v-else class="empty text-muted">아직 만든 라디오가 없어요.</p>
        </section>

        <section v-else-if="tab === 'reviews'" class="tab-panel">
          <MyReviewsPanel />
        </section>

        <section v-else-if="tab === 'follow'" class="tab-panel">
          <FollowPanel />
        </section>

        <section v-else-if="tab === 'plan'" class="tab-panel">
          <PlanPanel />
        </section>
      </template>

      <section class="account card">
        <div class="account-info">
          <h2 class="account-title">계정 관리</h2>
          <p class="account-desc text-muted">
            탈퇴하면 저장곡, 좋아요, 플레이리스트, 리뷰, 계획, 챌린지 정보에 접근할 수 없습니다.
          </p>
        </div>
        <BaseButton variant="danger" @click="deleteModal = true">회원 탈퇴</BaseButton>
      </section>
    </div>

    <UserEditModal v-model="editModal" @updated="onProfileUpdated" />
    <DeleteAccountModal v-model="deleteModal" @deleted="onAccountDeleted" />
  </AppShell>
</template>

<style scoped>
.me-wrap {
  max-width: 820px;
  margin: 0 auto;
  padding: 32px 0 80px;
}
.profile {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 24px;
  flex-wrap: wrap;
}
.avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: var(--grad-neon);
  color: #0a0a18;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30px;
  font-weight: 800;
  flex-shrink: 0;
}
.profile-info {
  flex: 1;
  min-width: 200px;
}
.nickname {
  font-size: 22px;
  font-weight: 800;
}
.email {
  font-size: 14px;
  margin-top: 2px;
}
.joined {
  font-size: 12px;
  margin-top: 2px;
}
.pref-tags {
  display: flex;
  gap: 6px;
  margin-top: 12px;
  flex-wrap: wrap;
}
.profile-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.account {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 22px 24px;
  margin-top: 28px;
  border-color: rgba(255, 92, 122, 0.3);
  flex-wrap: wrap;
}
.account-title {
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 4px;
}
.account-desc {
  font-size: 13px;
  line-height: 1.5;
  max-width: 520px;
}
.tabs {
  display: flex;
  gap: 14px;
  margin: 28px 0 20px;
  border-bottom: 1px solid var(--card-border);
  overflow-x: auto;
}
.tab {
  background: transparent;
  border: none;
  color: var(--text-muted);
  font-size: 15px;
  font-weight: 600;
  padding: 12px 4px;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  white-space: nowrap;
  flex-shrink: 0;
}
.tab.is-active {
  color: var(--text-primary);
  border-bottom-color: var(--neon-magenta);
}
.track-items {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.history-list {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}
@media (min-width: 640px) {
  .history-list {
    grid-template-columns: 1fr 1fr;
  }
}
.history-item {
  text-align: left;
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.history-body {
  cursor: pointer;
  transition: opacity 0.15s ease;
}
.history-body:hover {
  opacity: 0.85;
}
.history-tags {
  display: flex;
  gap: 6px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}
.history-situation {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 6px;
}
.history-ment {
  font-size: 13px;
  line-height: 1.5;
}
.history-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding-top: 12px;
  border-top: 1px solid var(--card-border);
  flex-wrap: wrap;
}
.history-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}
.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--text-muted);
}
.status-dot.is-public {
  background: var(--neon-cyan);
  box-shadow: 0 0 8px var(--neon-cyan);
}
.status-text {
  font-weight: 600;
}
.status-date {
  font-size: 12px;
}
.toggle-public {
  padding: 7px 14px;
  border-radius: var(--radius-pill);
  border: 1px solid var(--neon-cyan);
  background: transparent;
  color: var(--neon-cyan);
  font-size: 13px;
  font-weight: 600;
  transition: all 0.15s ease;
}
.toggle-public:hover:not(:disabled) {
  background: rgba(43, 223, 219, 0.12);
}
.toggle-public.is-public {
  border-color: var(--card-border);
  color: var(--text-muted);
}
.toggle-public:disabled {
  opacity: 0.6;
}
.empty {
  text-align: center;
  padding: 60px 0;
}
</style>
