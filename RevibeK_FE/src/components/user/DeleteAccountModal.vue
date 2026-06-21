<script setup>
import { ref } from "vue"
import { useAuthStore } from "@/stores/auth"
import { useUiStore } from "@/stores/ui"
import userApi from "@/api/userApi"
import BaseModal from "@/components/common/BaseModal.vue"
import BaseButton from "@/components/common/BaseButton.vue"

defineProps({
  modelValue: { type: Boolean, default: false },
})
const emit = defineEmits(["update:modelValue", "deleted"])

const auth = useAuthStore()
const ui = useUiStore()
const deleting = ref(false)

function close() {
  emit("update:modelValue", false)
}

async function confirm() {
  deleting.value = true
  try {
    await userApi.deleteMe()
    // clear access/refresh tokens + user state
    auth.clearSession()
    ui.success("회원 탈퇴가 완료되었습니다.")
    close()
    emit("deleted")
  } catch {
    ui.error("회원 탈퇴 처리에 실패했습니다.")
  } finally {
    deleting.value = false
  }
}
</script>

<template>
  <BaseModal :model-value="modelValue" title="회원 탈퇴" @update:model-value="close">
    <p class="warn text-secondary">
      정말 회원 탈퇴하시겠습니까?<br />
      탈퇴 후 저장곡, 좋아요, 플레이리스트, 리뷰, 계획, 챌린지 정보에 접근할 수 없습니다.
    </p>
    <template #footer>
      <BaseButton variant="ghost" @click="close">취소</BaseButton>
      <BaseButton variant="danger" :loading="deleting" @click="confirm">탈퇴하기</BaseButton>
    </template>
  </BaseModal>
</template>

<style scoped>
.warn {
  line-height: 1.6;
  font-size: 14px;
}
</style>
