<script setup>
import { onMounted } from "vue"
import { useRouter } from "vue-router"
import { useAuthStore } from "@/stores/auth"
import { useRadioStore } from "@/stores/radio"
import { useUiStore } from "@/stores/ui"

const router = useRouter()
const auth = useAuthStore()
const radio = useRadioStore()
const ui = useUiStore()

onMounted(async () => {
  const params = new URLSearchParams(window.location.hash.replace(/^#/, ""))
  const accessToken = params.get("accessToken")
  const refreshToken = params.get("refreshToken")

  if (!accessToken || !refreshToken) {
    router.replace("/login?error=google-auth-failed")
    return
  }

  try {
    auth.persist({ accessToken, refreshToken, user: null })
    await auth.fetchMe()
    ui.success("Google 계정으로 로그인되었어요.")
    router.replace(radio.loadPending() ? "/radio/generating" : "/main")
  } catch (e) {
    auth.clearSession()
    router.replace("/login?error=google-auth-failed")
  }
})
</script>

<template>
  <div class="callback-page">
    <p class="text-muted">로그인 처리 중입니다…</p>
  </div>
</template>

<style scoped>
.callback-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
