<script setup>
import { ref, computed, onMounted } from "vue"
import { useAuthStore } from "@/stores/auth"
import { useUiStore } from "@/stores/ui"
import reviewApi from "@/api/reviewApi"
import BaseButton from "@/components/common/BaseButton.vue"
import BaseModal from "@/components/common/BaseModal.vue"
import StarRating from "./StarRating.vue"

const props = defineProps({
  songId: { type: String, required: true },
})

const auth = useAuthStore()
const ui = useUiStore()

const loading = ref(true)
const reviews = ref([])

// create form
const showForm = ref(false)
const form = ref({ rating: 5, content: "" })
const submitting = ref(false)

// inline edit
const editingId = ref(null)
const editForm = ref({ rating: 5, content: "" })

// delete confirm
const deleteTarget = ref(null)

const myId = computed(() => auth.user?.id || null)
const isLoggedIn = computed(() => auth.isAuthenticated)

function isOwner(review) {
  return myId.value && review.userId === myId.value
}

function formatDate(iso) {
  if (!iso) return ""
  return new Date(iso).toLocaleDateString("ko-KR", {
    year: "numeric",
    month: "long",
    day: "numeric",
  })
}

async function load() {
  loading.value = true
  try {
    reviews.value = await reviewApi.getBySong(props.songId)
  } catch {
    ui.error("요청 처리 중 오류가 발생했습니다.")
  } finally {
    loading.value = false
  }
}

function openForm() {
  if (!isLoggedIn.value) {
    ui.notify("로그인이 필요한 기능입니다.", "info")
    return
  }
  form.value = { rating: 5, content: "" }
  showForm.value = true
}

async function submitCreate() {
  if (!form.value.content.trim()) {
    ui.notify("감상평을 입력해주세요.", "info")
    return
  }
  submitting.value = true
  try {
    await reviewApi.create({
      songId: props.songId,
      content: form.value.content.trim(),
      rating: form.value.rating,
    })
    ui.success("리뷰가 등록되었습니다.")
    showForm.value = false
    await load()
  } catch {
    ui.error("리뷰 등록에 실패했습니다.")
  } finally {
    submitting.value = false
  }
}

function startEdit(review) {
  editingId.value = review.id
  editForm.value = { rating: review.rating, content: review.content }
}

function cancelEdit() {
  editingId.value = null
}

async function submitEdit(review) {
  if (!editForm.value.content.trim()) {
    ui.notify("감상평을 입력해주세요.", "info")
    return
  }
  try {
    await reviewApi.update(review.id, {
      content: editForm.value.content.trim(),
      rating: editForm.value.rating,
    })
    ui.success("리뷰가 수정되었습니다.")
    editingId.value = null
    await load()
  } catch {
    ui.error("본인의 리뷰만 수정할 수 있습니다.")
  }
}

function askDelete(review) {
  deleteTarget.value = review
}

async function confirmDelete() {
  const target = deleteTarget.value
  if (!target) return
  try {
    await reviewApi.remove(target.id)
    ui.success("리뷰가 삭제되었습니다.")
    await load()
  } catch {
    ui.error("본인의 리뷰만 삭제할 수 있습니다.")
  } finally {
    deleteTarget.value = null
  }
}

onMounted(load)
</script>

<template>
  <div class="review-section">
    <div class="review-head">
      <h4 class="review-title">리뷰 {{ reviews.length }}</h4>
      <BaseButton size="sm" variant="outline" @click="openForm">리뷰 작성</BaseButton>
    </div>

    <!-- create form -->
    <div v-if="showForm" class="review-form card">
      <div class="form-row">
        <span class="form-label">별점</span>
        <StarRating v-model="form.rating" :size="22" />
      </div>
      <textarea
        v-model="form.content"
        class="review-textarea"
        rows="3"
        placeholder="이 곡에 대한 감상평을 남겨주세요."
      ></textarea>
      <div class="form-actions">
        <BaseButton size="sm" variant="ghost" @click="showForm = false">취소</BaseButton>
        <BaseButton size="sm" :loading="submitting" @click="submitCreate">등록</BaseButton>
      </div>
    </div>

    <p v-if="loading" class="review-loading text-muted">리뷰를 불러오는 중...</p>

    <ul v-else-if="reviews.length" class="review-list">
      <li v-for="r in reviews" :key="r.id" class="review-item">
        <!-- inline edit mode -->
        <template v-if="editingId === r.id">
          <div class="form-row">
            <span class="form-label">별점</span>
            <StarRating v-model="editForm.rating" :size="20" />
          </div>
          <textarea v-model="editForm.content" class="review-textarea" rows="3"></textarea>
          <div class="form-actions">
            <BaseButton size="sm" variant="ghost" @click="cancelEdit">취소</BaseButton>
            <BaseButton size="sm" @click="submitEdit(r)">저장</BaseButton>
          </div>
        </template>

        <!-- display mode -->
        <template v-else>
          <div class="review-item-top">
            <div class="reviewer">
              <span class="reviewer-name">{{ r.userNickname }}</span>
              <StarRating :model-value="r.rating" readonly :size="14" />
            </div>
            <span class="review-date text-muted">{{ formatDate(r.createdAt) }}</span>
          </div>
          <p class="review-content">{{ r.content }}</p>
          <div v-if="isOwner(r)" class="review-owner-actions">
            <button class="mini-btn" @click="startEdit(r)">수정</button>
            <button class="mini-btn danger" @click="askDelete(r)">삭제</button>
          </div>
        </template>
      </li>
    </ul>

    <p v-else class="review-empty text-muted">
      아직 등록된 리뷰가 없습니다. 첫 감상평을 남겨보세요.
    </p>

    <BaseModal
      :model-value="!!deleteTarget"
      title="리뷰 삭제"
      @update:model-value="deleteTarget = null"
    >
      <p class="text-secondary">
        이 리뷰를 삭제하시겠습니까?<br />삭제한 리뷰는 복구할 수 없습니다.
      </p>
      <template #footer>
        <BaseButton variant="ghost" @click="deleteTarget = null">취소</BaseButton>
        <BaseButton variant="danger" @click="confirmDelete">삭제</BaseButton>
      </template>
    </BaseModal>
  </div>
</template>

<style scoped>
.review-section {
  border-top: 1px solid var(--card-border);
  padding-top: 16px;
}
.review-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}
.review-title {
  font-size: 14px;
  font-weight: 700;
}
.review-form {
  padding: 14px;
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.form-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.form-label {
  font-size: 13px;
  color: var(--text-muted);
  font-weight: 600;
}
.review-textarea {
  width: 100%;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--card-border);
  border-radius: var(--radius-sm);
  color: var(--text-primary);
  font-size: 14px;
  line-height: 1.5;
  padding: 10px 12px;
  resize: vertical;
  font-family: inherit;
}
.review-textarea:focus {
  outline: none;
  border-color: var(--neon-cyan);
}
.form-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}
.review-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  list-style: none;
  padding: 0;
  margin: 0;
}
.review-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--card-border);
}
.review-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}
.review-item-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.reviewer {
  display: flex;
  align-items: center;
  gap: 8px;
}
.reviewer-name {
  font-size: 14px;
  font-weight: 700;
}
.review-date {
  font-size: 12px;
}
.review-content {
  font-size: 14px;
  line-height: 1.55;
  color: var(--text-secondary);
}
.review-owner-actions {
  display: flex;
  gap: 12px;
}
.mini-btn {
  background: transparent;
  border: none;
  color: var(--text-muted);
  font-size: 13px;
  font-weight: 600;
  padding: 0;
}
.mini-btn:hover {
  color: var(--neon-cyan);
}
.mini-btn.danger:hover {
  color: var(--danger);
}
.review-loading,
.review-empty {
  font-size: 13px;
  padding: 16px 0;
  text-align: center;
}
</style>
