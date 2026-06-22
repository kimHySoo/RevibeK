<script setup>
import { ref, onMounted } from "vue"
import { useUiStore } from "@/stores/ui"
import planApi from "@/api/planApi"
import { PLAN_TYPES } from "@/mocks/plans"
import BaseButton from "@/components/common/BaseButton.vue"
import BaseModal from "@/components/common/BaseModal.vue"

const ui = useUiStore()

const loading = ref(true)
const plans = ref([])
const planTypes = PLAN_TYPES

const showForm = ref(false)
const saving = ref(false)
const form = ref({ planType: PLAN_TYPES[0], title: "", description: "", targetDate: "" })

const deleteTarget = ref(null)

async function load() {
  loading.value = true
  try {
    plans.value = await planApi.list()
  } catch {
    ui.error("요청 처리 중 오류가 발생했습니다.")
  } finally {
    loading.value = false
  }
}

function openForm() {
  form.value = { planType: PLAN_TYPES[0], title: "", description: "", targetDate: "" }
  showForm.value = true
}

async function createPlan() {
  if (!form.value.title.trim()) {
    ui.notify("계획 제목을 입력해주세요.", "info")
    return
  }
  saving.value = true
  try {
    await planApi.create({ ...form.value, title: form.value.title.trim() })
    ui.success("청취 계획을 추가했어요.")
    showForm.value = false
    await load()
  } catch {
    ui.error("요청 처리 중 오류가 발생했습니다.")
  } finally {
    saving.value = false
  }
}

async function toggleComplete(plan) {
  try {
    await planApi.update(plan.id, { completed: !plan.completed })
    await load()
  } catch {
    ui.error("요청 처리 중 오류가 발생했습니다.")
  }
}

async function confirmDelete() {
  const target = deleteTarget.value
  if (!target) return
  try {
    await planApi.remove(target.id)
    ui.success("계획을 삭제했어요.")
    await load()
  } catch {
    ui.error("요청 처리 중 오류가 발생했습니다.")
  } finally {
    deleteTarget.value = null
  }
}

onMounted(load)
</script>

<template>
  <div class="plan-panel">
    <div class="panel-head">
      <h3 class="panel-title">청취 계획</h3>
      <BaseButton size="sm" variant="outline" @click="openForm">계획 추가</BaseButton>
    </div>

    <div v-if="showForm" class="plan-form card">
      <div class="form-grid">
        <label class="field">
          <span class="field-label">유형</span>
          <select v-model="form.planType" class="field-input">
            <option v-for="t in planTypes" :key="t" :value="t">{{ t }}</option>
          </select>
        </label>
        <label class="field">
          <span class="field-label">목표 날짜</span>
          <input v-model="form.targetDate" type="date" class="field-input" />
        </label>
      </div>
      <label class="field">
        <span class="field-label">제목</span>
        <input v-model="form.title" type="text" class="field-input" placeholder="예: 퇴근길 위로 라디오" />
      </label>
      <label class="field">
        <span class="field-label">설명</span>
        <textarea v-model="form.description" class="field-input" rows="2" placeholder="간단한 메모"></textarea>
      </label>
      <div class="form-actions">
        <BaseButton size="sm" variant="ghost" @click="showForm = false">취소</BaseButton>
        <BaseButton size="sm" :loading="saving" @click="createPlan">추가</BaseButton>
      </div>
    </div>

    <p v-if="loading" class="empty text-muted">불러오는 중...</p>

    <div v-else-if="plans.length" class="plan-list">
      <div v-for="p in plans" :key="p.id" class="plan-card card" :class="{ done: p.completed }">
        <div class="plan-top">
          <span class="badge badge-neon">{{ p.planType }}</span>
          <span v-if="p.completed" class="badge done-badge">완료</span>
        </div>
        <p class="plan-title-text">{{ p.title }}</p>
        <p v-if="p.description" class="plan-desc text-muted">{{ p.description }}</p>
        <p v-if="p.targetDate" class="plan-date text-muted">목표 {{ p.targetDate }}</p>
        <div class="plan-actions">
          <button class="mini-btn" @click="toggleComplete(p)">
            {{ p.completed ? "진행중으로" : "완료 표시" }}
          </button>
          <button class="mini-btn danger" @click="deleteTarget = p">삭제</button>
        </div>
      </div>
    </div>

    <p v-else class="empty text-muted">아직 등록한 청취 계획이 없어요.</p>

    <BaseModal
      :model-value="!!deleteTarget"
      title="계획 삭제"
      @update:model-value="deleteTarget = null"
    >
      <p class="text-secondary">이 청취 계획을 삭제하시겠습니까?</p>
      <template #footer>
        <BaseButton variant="ghost" @click="deleteTarget = null">취소</BaseButton>
        <BaseButton variant="danger" @click="confirmDelete">삭제</BaseButton>
      </template>
    </BaseModal>
  </div>
</template>

<style scoped>
.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.panel-title {
  font-size: 16px;
  font-weight: 700;
}
.plan-form {
  padding: 16px;
  margin-bottom: 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.field-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-muted);
}
.field-input {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--card-border);
  border-radius: var(--radius-sm);
  color: var(--text-primary);
  font-size: 14px;
  padding: 10px 12px;
  font-family: inherit;
}
.field-input:focus {
  outline: none;
  border-color: var(--neon-cyan);
}
.form-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}
.plan-list {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}
@media (min-width: 640px) {
  .plan-list {
    grid-template-columns: 1fr 1fr;
  }
}
.plan-card {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.plan-card.done {
  opacity: 0.7;
}
.plan-top {
  display: flex;
  gap: 6px;
}
.done-badge {
  border-color: var(--neon-cyan);
  color: var(--neon-cyan);
}
.plan-title-text {
  font-size: 15px;
  font-weight: 700;
}
.plan-desc {
  font-size: 13px;
  line-height: 1.5;
}
.plan-date {
  font-size: 12px;
}
.plan-actions {
  display: flex;
  gap: 12px;
  margin-top: 4px;
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
