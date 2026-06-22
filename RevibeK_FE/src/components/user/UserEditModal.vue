<script setup>
import { ref, watch } from "vue"
import { useAuthStore } from "@/stores/auth"
import { useUiStore } from "@/stores/ui"
import userApi from "@/api/userApi"
import BaseModal from "@/components/common/BaseModal.vue"
import BaseButton from "@/components/common/BaseButton.vue"

const props = defineProps({
  modelValue: { type: Boolean, default: false },
})
const emit = defineEmits(["update:modelValue", "updated"])

const auth = useAuthStore()
const ui = useUiStore()

const loading = ref(false)
const saving = ref(false)
const form = ref({ nickname: "", email: "", password: "" })

// load current profile whenever the modal opens
watch(
  () => props.modelValue,
  async (open) => {
    if (!open) return
    loading.value = true
    try {
      const me = await userApi.me()
      form.value = {
        nickname: me.nickname || "",
        email: me.email || "",
        password: "",
      }
    } catch {
      ui.error("요청 처리 중 오류가 발생했습니다.")
    } finally {
      loading.value = false
    }
  }
)

function close() {
  emit("update:modelValue", false)
}

async function save() {
  saving.value = true
  try {
    const payload = {
      nickname: form.value.nickname,
      email: form.value.email,
    }
    // only send password when the user typed a new one
    if (form.value.password.trim()) {
      payload.password = form.value.password.trim()
    }
    await userApi.updateMe(payload)
    // re-fetch the latest profile and sync the auth store
    const fresh = await userApi.me()
    auth.setUser(fresh)
    ui.success("회원정보가 수정되었습니다.")
    emit("updated", fresh)
    close()
  } catch {
    ui.error("회원정보 수정에 실패했습니다.")
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <BaseModal :model-value="modelValue" title="회원정보 수정" @update:model-value="close">
    <div v-if="loading" class="edit-loading text-muted">불러오는 중...</div>
    <form v-else class="edit-form" @submit.prevent="save">
      <label class="field">
        <span class="field-label">닉네임</span>
        <input v-model="form.nickname" type="text" class="field-input" placeholder="닉네임" />
      </label>
      <label class="field">
        <span class="field-label">이메일</span>
        <input v-model="form.email" type="email" class="field-input" placeholder="이메일" />
      </label>
      <label class="field">
        <span class="field-label">새 비밀번호</span>
        <input
          v-model="form.password"
          type="password"
          class="field-input"
          placeholder="변경할 때만 입력하세요"
          autocomplete="new-password"
        />
        <span class="field-hint">비워두면 비밀번호는 변경되지 않습니다.</span>
      </label>
    </form>
    <template #footer>
      <BaseButton variant="ghost" @click="close">취소</BaseButton>
      <BaseButton :loading="saving" :disabled="loading" @click="save">저장</BaseButton>
    </template>
  </BaseModal>
</template>

<style scoped>
.edit-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
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
  font-size: 15px;
  padding: 11px 13px;
}
.field-input:focus {
  outline: none;
  border-color: var(--neon-cyan);
}
.field-hint {
  font-size: 12px;
  color: var(--text-muted);
}
.edit-loading {
  padding: 24px 0;
  text-align: center;
}
</style>
