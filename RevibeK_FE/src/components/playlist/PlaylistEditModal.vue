<script setup>
import { ref, watch } from "vue"
import { useAuthStore } from "@/stores/auth"
import { usePlaylistStore } from "@/stores/playlist"
import { useUiStore } from "@/stores/ui"
import BaseModal from "@/components/common/BaseModal.vue"
import BaseButton from "@/components/common/BaseButton.vue"

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  playlist: { type: Object, default: null },
})
const emit = defineEmits(["update:modelValue", "updated"])

const auth = useAuthStore()
const playlistStore = usePlaylistStore()
const ui = useUiStore()

const form = ref({ name: "", moodTag: "", isPublic: false })
const saving = ref(false)

// hydrate the form whenever the modal opens
watch(
  () => [props.modelValue, props.playlist],
  ([open]) => {
    if (open && props.playlist) {
      form.value = {
        name: props.playlist.title || props.playlist.name || "",
        moodTag: props.playlist.moodTag || "",
        isPublic: !!props.playlist.isPublic,
      }
    }
  },
  { immediate: true }
)

function close() {
  emit("update:modelValue", false)
}

async function save() {
  if (!auth.isAuthenticated) {
    ui.notify("로그인이 필요합니다.", "info")
    return
  }
  if (!form.value.name.trim()) {
    ui.notify("플레이리스트 이름을 입력해주세요.", "info")
    return
  }
  saving.value = true
  try {
    const updated = await playlistStore.update(props.playlist.playlistId, {
      name: form.value.name.trim(),
      moodTag: form.value.moodTag.trim(),
      isPublic: form.value.isPublic,
    })
    ui.success("플레이리스트가 수정되었습니다.")
    emit("updated", updated)
    close()
  } catch {
    ui.error("플레이리스트 수정에 실패했습니다.")
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <BaseModal
    :model-value="modelValue"
    title="플레이리스트 수정"
    @update:model-value="close"
  >
    <div class="form">
      <label class="field">
        <span class="field-label">플레이리스트 이름</span>
        <input
          v-model="form.name"
          type="text"
          class="field-input"
          placeholder="예: 퇴근길 감성 플레이리스트"
        />
      </label>
      <label class="field">
        <span class="field-label">분위기 태그</span>
        <input
          v-model="form.moodTag"
          type="text"
          class="field-input"
          placeholder="예: 위로"
        />
      </label>
      <label class="toggle-field">
        <input v-model="form.isPublic" type="checkbox" />
        <span>공개 플레이리스트로 설정</span>
      </label>
    </div>

    <template #footer>
      <BaseButton variant="ghost" @click="close">취소</BaseButton>
      <BaseButton :loading="saving" @click="save">저장</BaseButton>
    </template>
  </BaseModal>
</template>

<style scoped>
.form {
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
  font-size: 14px;
  padding: 10px 12px;
  font-family: inherit;
}
.field-input:focus {
  outline: none;
  border-color: var(--neon-cyan);
}
.toggle-field {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--text-secondary);
  cursor: pointer;
}
.toggle-field input {
  width: 16px;
  height: 16px;
  accent-color: var(--neon-cyan);
}
</style>
