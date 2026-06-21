<script setup>
import { ref, watch, computed } from "vue"
import { useAuthStore } from "@/stores/auth"
import publicRadioApi from "@/api/publicRadioApi"
import BaseModal from "@/components/common/BaseModal.vue"
import SongCard from "@/components/song/SongCard.vue"
import RadioLikeButton from "./RadioLikeButton.vue"
import FollowButton from "@/components/social/FollowButton.vue"

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  radioSessionId: { type: String, default: "" },
})
const emit = defineEmits(["update:modelValue", "like-update", "follow-update"])

const auth = useAuthStore()

const loading = ref(false)
const error = ref(false)
const detail = ref(null)
const playingSongId = ref(null)

const isSelf = computed(
  () => auth.user?.id != null && detail.value && auth.user.id === detail.value.userId
)
const songs = computed(() => detail.value?.recommendedSongs || [])

function isPlaying(song) {
  const key = song?.songId || song?.id
  return playingSongId.value != null && playingSongId.value === key
}
function togglePlay(song) {
  const key = song?.songId || song?.id
  playingSongId.value = playingSongId.value === key ? null : key
}

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

async function load() {
  if (!props.radioSessionId) return
  loading.value = true
  error.value = false
  detail.value = null
  playingSongId.value = null
  try {
    detail.value = await publicRadioApi.getDetail(props.radioSessionId)
    if (!detail.value) error.value = true
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}

watch(
  () => [props.modelValue, props.radioSessionId],
  ([open]) => {
    if (open) load()
  }
)

function close() {
  emit("update:modelValue", false)
}
</script>

<template>
  <BaseModal
    :model-value="modelValue"
    :title="detail?.title || '라디오 사연'"
    size="lg"
    @update:model-value="close"
  >
    <p v-if="loading" class="state text-muted">불러오는 중입니다...</p>
    <p v-else-if="error" class="state text-muted">
      데이터를 불러오지 못했습니다.<br />잠시 후 다시 시도해주세요.
    </p>

    <div v-else-if="detail" class="detail">
      <div class="d-tags">
        <span v-if="detail.mood" class="badge badge-neon">{{ detail.mood }}</span>
        <span v-if="detail.situation" class="badge">{{ detail.situation }}</span>
        <span v-if="detail.generation" class="badge">{{ detail.generation }}</span>
      </div>

      <div class="d-author">
        <span class="d-nick">{{ detail.userNickname }}</span>
        <FollowButton
          :user-id="detail.userId"
          :followed="detail.followed"
          :is-self="isSelf"
          @update="$emit('follow-update', { id: detail.radioSessionId, followed: $event })"
        />
      </div>

      <p v-if="publishedLabel(detail.publishedAt)" class="d-date text-muted">
        {{ publishedLabel(detail.publishedAt) }}
      </p>

      <p class="d-story text-secondary">{{ detail.story }}</p>

      <div v-if="detail.djComment" class="d-dj">
        <span class="d-dj-label">DJ 멘트</span>
        <p class="text-muted">{{ detail.djComment }}</p>
      </div>

      <div v-if="songs.length" class="d-songs">
        <h4 class="d-songs-title">추천곡 {{ songs.length }}</h4>
        <div class="d-song-grid">
          <SongCard
            v-for="(s, i) in songs"
            :key="s.songId || i"
            :song="s"
            :show-save="false"
            :playing="isPlaying(s)"
            reviewable
            @play="togglePlay"
          />
        </div>
      </div>
    </div>

    <template #footer>
      <RadioLikeButton
        v-if="detail"
        :radio-session-id="detail.radioSessionId"
        :liked="detail.liked"
        :like-count="detail.likeCount"
        @update="$emit('like-update', { id: detail.radioSessionId, ...$event })"
      />
      <button type="button" class="d-close" @click="close">닫기</button>
    </template>
  </BaseModal>
</template>

<style scoped>
.state {
  text-align: center;
  padding: 40px 0;
  line-height: 1.6;
}
.detail {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.d-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.d-author {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.d-nick {
  font-size: 15px;
  font-weight: 700;
}
.d-date {
  font-size: 12px;
  margin-top: -6px;
}
.d-story {
  font-size: 14px;
  line-height: 1.7;
}
.d-dj {
  border-left: 2px solid var(--neon-cyan);
  padding-left: 12px;
}
.d-dj-label {
  font-size: 12px;
  font-weight: 700;
  color: var(--neon-cyan);
}
.d-dj p {
  font-size: 13px;
  line-height: 1.5;
  margin-top: 2px;
}
.d-songs-title {
  font-size: 15px;
  font-weight: 700;
  margin-bottom: 10px;
}
.d-song-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}
@media (min-width: 640px) {
  .d-song-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    align-items: start;
  }
}
.d-close {
  padding: 9px 18px;
  border-radius: var(--radius-pill);
  border: 1px solid var(--card-border);
  background: transparent;
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 600;
}
.d-close:hover {
  border-color: var(--neon-cyan);
  color: var(--neon-cyan);
}
</style>
