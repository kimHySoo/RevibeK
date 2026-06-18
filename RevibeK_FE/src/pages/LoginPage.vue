<script setup>
import { ref, computed, onMounted } from "vue"
import { useRouter, useRoute } from "vue-router"
import AuthCard from "@/components/auth/AuthCard.vue"
import BaseButton from "@/components/common/BaseButton.vue"
import ErrorNotice from "@/components/common/ErrorNotice.vue"
import { useAuthStore } from "@/stores/auth"
import { useRadioStore } from "@/stores/radio"
import { useUiStore } from "@/stores/ui"

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const radio = useRadioStore()
const ui = useUiStore()

const email = ref("")
const password = ref("")
const error = ref("")

const hasPending = ref(false)

onMounted(() => {
  hasPending.value = !!radio.loadPending()
})

const redirectTarget = computed(() => route.query.redirect || "")
const signupLink = computed(() => ({
  path: "/signup",
  query: hasPending.value
    ? { redirect: "/radio/generating" }
    : redirectTarget.value
      ? { redirect: redirectTarget.value }
      : {},
}))

async function submit() {
  error.value = ""
  try {
    await auth.login({ email: email.value, password: password.value })
    ui.success("로그인되었어요.")

    // With a saved story, head straight to generating per flow
    if (radio.loadPending()) {
      router.push("/radio/generating")
    } else if (redirectTarget.value) {
      router.push(redirectTarget.value)
    } else {
      router.push("/main")
    }
  } catch (e) {
    error.value = e?.message || "로그인에 실패했어요. 정보를 확인해주세요."
  }
}

const backendOrigin = (import.meta.env.VITE_API_BASE_URL || "/api").replace(/\/api\/?$/, "")

function loginWithGoogle() {
  window.location.href = `${backendOrigin}/oauth2/authorization/google`
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-glow" aria-hidden="true"></div>
    <div class="container-narrow auth-wrap">
      <router-link to="/" class="back-link">
        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
          <path d="M15 18l-6-6 6-6" />
        </svg>
        메인으로 돌아가기
      </router-link>

      <div v-if="hasPending" class="pending-banner card">
        <span class="pending-icon" aria-hidden="true"></span>
        <div>
          <p class="pending-strong">저장된 라디오 사연이 있어요.</p>
          <p class="text-muted pending-text">
            로그인하면 AI DJ가 바로 선곡을 시작합니다.
          </p>
        </div>
      </div>

      <AuthCard title="로그인" subtitle="감정으로 시작하는 AI 커버 라디오에 다시 오신 걸 환영해요.">
        <ErrorNotice v-if="error" :message="error" />
        <form class="auth-form" @submit.prevent="submit">
          <div>
            <label class="field-label" for="email">이메일</label>
            <input
              id="email"
              v-model="email"
              type="email"
              class="field"
              placeholder="you@revibek.app"
              autocomplete="email"
              required
            />
          </div>
          <div>
            <label class="field-label" for="password">비밀번호</label>
            <input
              id="password"
              v-model="password"
              type="password"
              class="field"
              placeholder="비밀번호"
              autocomplete="current-password"
              required
            />
          </div>
          <BaseButton type="submit" block size="lg" :loading="auth.loading">
            {{ hasPending ? "로그인하고 라디오 만들기" : "로그인" }}
          </BaseButton>
        </form>

        <div class="divider"><span>또는</span></div>

        <button type="button" class="gsi-material-button" @click="loginWithGoogle">
          <div class="gsi-material-button-state"></div>
          <div class="gsi-material-button-content-wrapper">
            <div class="gsi-material-button-icon">
              <svg version="1.1" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" style="display: block;">
                <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"></path>
                <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"></path>
                <path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"></path>
                <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"></path>
                <path fill="none" d="M0 0h48v48H0z"></path>
              </svg>
            </div>
            <span class="gsi-material-button-contents">Sign in with Google</span>
          </div>
        </button>

        <p class="auth-foot text-muted">
          아직 계정이 없나요?
          <router-link :to="signupLink" class="link">
            {{ hasPending ? "회원가입하고 이어서 만들기" : "회원가입" }}
          </router-link>
        </p>
      </AuthCard>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  position: relative;
  background: var(--bg-base);
  overflow: hidden;
}
.auth-glow {
  position: absolute;
  inset: 0;
  background: var(--grad-hero);
}
.auth-wrap {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 18px;
}
.back-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  align-self: flex-start;
  width: 100%;
  max-width: 440px;
  color: var(--text-muted);
  font-size: 14px;
  font-weight: 500;
  transition: color 0.18s ease;
}
.back-link:hover {
  color: var(--neon-cyan);
}
.pending-banner {
  display: flex;
  gap: 14px;
  align-items: center;
  padding: 16px 18px;
  max-width: 440px;
  width: 100%;
  border-color: var(--neon-cyan);
}
.pending-icon {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: var(--neon-cyan);
  box-shadow: var(--glow-cyan);
  flex-shrink: 0;
  animation: pulse-soft 1.4s ease-in-out infinite;
}
.pending-strong {
  font-weight: 600;
  font-size: 14px;
}
.pending-text {
  font-size: 13px;
}
.auth-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 4px;
}
.divider {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 6px 0 2px;
  color: var(--text-muted);
  font-size: 12px;
}
.divider::before,
.divider::after {
  content: "";
  flex: 1;
  height: 1px;
  background: var(--card-border);
}

/* Google "Sign in with Google" button — Google Identity branding guidelines */
.gsi-material-button {
  -moz-user-select: none;
  -webkit-user-select: none;
  user-select: none;
  -webkit-appearance: none;
  background-color: #fff;
  background-image: none;
  border: 1px solid #747775;
  -webkit-border-radius: 20px;
  border-radius: 20px;
  -webkit-box-sizing: border-box;
  box-sizing: border-box;
  color: #1f1f1f;
  cursor: pointer;
  font-family: "Roboto", arial, sans-serif;
  font-size: 14px;
  height: 40px;
  letter-spacing: 0.25px;
  outline: none;
  overflow: hidden;
  padding: 0 12px;
  position: relative;
  text-align: center;
  -webkit-transition: background-color .218s, border-color .218s, box-shadow .218s;
  transition: background-color .218s, border-color .218s, box-shadow .218s;
  vertical-align: middle;
  white-space: nowrap;
  width: 100%;
}
.gsi-material-button .gsi-material-button-icon {
  height: 20px;
  margin-right: 12px;
  min-width: 20px;
  width: 20px;
}
.gsi-material-button .gsi-material-button-content-wrapper {
  -webkit-align-items: center;
  align-items: center;
  display: flex;
  -webkit-flex-direction: row;
  flex-direction: row;
  -webkit-flex-wrap: nowrap;
  flex-wrap: nowrap;
  height: 100%;
  justify-content: center;
  position: relative;
  width: 100%;
}
.gsi-material-button .gsi-material-button-contents {
  -webkit-flex-grow: 1;
  flex-grow: 1;
  font-family: "Roboto", arial, sans-serif;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: top;
}
.gsi-material-button .gsi-material-button-state {
  -webkit-transition: opacity .218s;
  transition: opacity .218s;
  bottom: 0;
  left: 0;
  opacity: 0;
  position: absolute;
  right: 0;
  top: 0;
}
.gsi-material-button:disabled {
  cursor: default;
  background-color: #ffffff61;
  border-color: #1f1f1f1f;
}
.gsi-material-button:not(:disabled):active .gsi-material-button-state,
.gsi-material-button:not(:disabled):focus .gsi-material-button-state {
  background-color: #303030;
  opacity: 12%;
}
.gsi-material-button:not(:disabled):hover {
  -webkit-box-shadow: 0 1px 2px 0 rgba(60, 64, 67, .3), 0 1px 3px 1px rgba(60, 64, 67, .15);
  box-shadow: 0 1px 2px 0 rgba(60, 64, 67, .3), 0 1px 3px 1px rgba(60, 64, 67, .15);
}
.gsi-material-button:not(:disabled):hover .gsi-material-button-state {
  background-color: #303030;
  opacity: 8%;
}
.auth-foot {
  text-align: center;
  font-size: 14px;
  margin-top: 4px;
}
.link {
  color: var(--neon-magenta);
  font-weight: 600;
}
</style>
