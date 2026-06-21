<script setup>
import { ref, onMounted } from "vue"
import { useUiStore } from "@/stores/ui"
import reviewApi from "@/api/reviewApi"
import BaseButton from "@/components/common/BaseButton.vue"
import BaseModal from "@/components/common/BaseModal.vue"
import StarRating from "./StarRating.vue"

const ui = useUiStore()

const loading = ref(true)
const reviews = ref([])

const editingId = ref(null)
const editForm = ref({ rating: 5, content: "" })
const deleteTarget = ref(null)

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
    reviews.value = await reviewApi.mine()
  } catch {
    ui.error("요청 처리 중 오류가 발생했습니다.")
  } finally {
    loading.value = false
  }
}

function startEdit(r) {
  editingId.value = r.id
  editForm.value = { rating: r.rating, content: r.content }
}

async function submitEdit(r) {
  if (!editForm.value.content.trim()) {
    ui.notify("감상평을 입력해주세요.", "info")
    return
  }
  try {
    await reviewApi.update(r.id, {
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
  <div class="my-reviews">
    <p v-if="loading" class="empty text-muted">불러오는 중...</p>

    <ul v-else-if="reviews.length" class="review-list">
      <li v-for="r in reviews" :key="r.id" class="review-item card">
        <template v-if="editingId === r.id">
          <div class="form-row">
            <span class="form-label">별점</span>
            <StarRating v-model="editForm.rating" :size="20" />
          </div>
          <textarea v-model="editForm.content" class="review-textarea" rows="3"></textarea>
          <div class="form-actions">
            <BaseButton size="sm" variant="ghost" @click="editingId = null">취소</BaseButton>
            <BaseButton size="sm" @click="submitEdit(r)">저장</BaseButton>
          </div>
        </template>

        <template v-else>
          <div class="review-item-top">
            <StarRating :model-value="r.rating" readonly :size="15" />
            <span class="review-date text-muted">{{ formatDate(r.createdAt) }}</span>
          </div>
          <p class="review-song text-muted">곡 ID · {{ r.songId }}</p>
          <p class="review-content">{{ r.content }}</p>
          <div class="review-owner-actions">
            <button class="mini-btn" @click="startEdit(r)">수정</button>
            <button class="mini-btn danger" @click="deleteTarget = r">삭제</button>
          </div>
        </template>
      </li>
    </ul>

    <p v-else class="empty text-muted">아직 작성한 리뷰가 없어요.</p>

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
.review-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  list-style: none;
  padding: 0;
  margin: 0;
}
.review-item {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.review-item-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.review-date {
  font-size: 12px;
}
.review-song {
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
.empty {
  text-align: center;
  padding: 48px 0;
}
</style>
