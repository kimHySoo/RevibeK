<script setup>
import { ref, computed, onMounted } from "vue"
import { useRouter, useRoute } from "vue-router"
import AuthCard from "@/components/auth/AuthCard.vue"
import SignupStepper from "@/components/auth/SignupStepper.vue"
import BaseButton from "@/components/common/BaseButton.vue"
import ErrorNotice from "@/components/common/ErrorNotice.vue"
import authApi from "@/api/authApi"
import { useAuthStore } from "@/stores/auth"
import { useRadioStore } from "@/stores/radio"
import { useUiStore } from "@/stores/ui"

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const radio = useRadioStore()
const ui = useUiStore()

const hasPending = ref(false)
const redirectTarget = computed(() => route.query.redirect || "")
const loginLink = computed(() => ({
  path: "/login",
  query: redirectTarget.value ? { redirect: redirectTarget.value } : {},
}))

onMounted(() => {
  hasPending.value = !!radio.loadPending()
})

const steps = ["이메일 인증", "코드 확인", "정보 입력"]
const current = ref(0)
const error = ref("")
const busy = ref(false)

const email = ref("")
const code = ref("")
const nickname = ref("")
const password = ref("")
const devCode = ref("")

async function sendCode() {
  error.value = ""
  if (!email.value) {
    error.value = "이메일을 입력해주세요."
    return
  }
  busy.value = true
  try {
    const res = await authApi.sendEmailCode(email.value)
    devCode.value = res.devCode || ""
    ui.success("인증 코드를 보냈어요." + (devCode.value ? ` (데모 코드: ${devCode.value})` : ""))
    current.value = 1
  } catch (e) {
    error.value = "인증 코드 발송에 실패했어요."
  } finally {
    busy.value = false
  }
}

async function verifyCode() {
  error.value = ""
  busy.value = true
  try {
    const res = await authApi.verifyEmailCode(email.value, code.value)
    if (res.verified) {
      current.value = 2
    } else {
      error.value = "인증 코드가 올바르지 않아요."
    }
  } catch (e) {
    error.value = "인증에 실패했어요."
  } finally {
    busy.value = false
  }
}

async function signup() {
  error.value = ""
  if (!nickname.value || !password.value) {
    error.value = "닉네임과 비밀번호를 입력해주세요."
    return
  }
  busy.value = true
  try {
    await auth.signup({
      email: email.value,
      password: password.value,
      nickname: nickname.value,
    })
    ui.success("회원가입이 완료되었어요! 로그인하고 이어서 만들어요.")

    // After signup, route to login (carrying any pending-story redirect)
    if (radio.loadPending()) {
      router.push({ path: "/login", query: { redirect: "/radio/generating" } })
    } else {
      router.push("/login")
    }
  } catch (e) {
    error.value = "회원가입에 실패했어요."
  } finally {
    busy.value = false
  }
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
          <p class="pending-strong">사연이 저장되어 있어요.</p>
          <p class="text-muted pending-text">
            회원가입 후 로그인하면 바로 AI DJ가 선곡을 시작합니다.
          </p>
        </div>
      </div>

      <AuthCard title="회원가입" subtitle="감정 기반 AI 커버 라디오를 시작해보세요.">
        <SignupStepper :steps="steps" :current="current" />
        <ErrorNotice v-if="error" :message="error" />

        <!-- Step 1: email -->
        <form v-if="current === 0" class="auth-form" @submit.prevent="sendCode">
          <div>
            <label class="field-label" for="su-email">이메일</label>
            <input
              id="su-email"
              v-model="email"
              type="email"
              class="field"
              placeholder="you@revibek.app"
              required
            />
          </div>
          <BaseButton type="submit" block size="lg" :loading="busy">
            인증 코드 받기
          </BaseButton>
        </form>

        <!-- Step 2: verify code -->
        <form v-else-if="current === 1" class="auth-form" @submit.prevent="verifyCode">
          <div>
            <label class="field-label" for="su-code">인증 코드</label>
            <input
              id="su-code"
              v-model="code"
              type="text"
              inputmode="numeric"
              class="field"
              placeholder="6자리 코드"
              required
            />
            <p v-if="devCode" class="hint text-muted">데모 코드: {{ devCode }}</p>
          </div>
          <BaseButton type="submit" block size="lg" :loading="busy">
            코드 확인
          </BaseButton>
          <button type="button" class="text-link" @click="current = 0">
            이메일 다시 입력
          </button>
        </form>

        <!-- Step 3: profile -->
        <form v-else class="auth-form" @submit.prevent="signup">
          <div>
            <label class="field-label" for="su-nick">닉네임</label>
            <input
              id="su-nick"
              v-model="nickname"
              type="text"
              class="field"
              placeholder="라디오에서 불릴 이름"
              required
            />
          </div>
          <div>
            <label class="field-label" for="su-pw">비밀번호</label>
            <input
              id="su-pw"
              v-model="password"
              type="password"
              class="field"
              placeholder="비밀번호"
              autocomplete="new-password"
              required
            />
          </div>
          <BaseButton type="submit" block size="lg" :loading="busy">
            {{ hasPending ? "가입하고 라디오 이어서 만들기" : "가입 완료" }}
          </BaseButton>
        </form>

        <p class="auth-foot text-muted">
          이미 계정이 있으신가요?
          <router-link :to="loginLink" class="link">로그인하고 이어서 만들기</router-link>
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
  margin-top: 8px;
}
.hint {
  font-size: 12px;
  margin-top: 6px;
}
.text-link {
  background: transparent;
  border: none;
  color: var(--text-muted);
  font-size: 13px;
  text-decoration: underline;
}
.auth-foot {
  text-align: center;
  font-size: 14px;
  margin-top: 8px;
}
.link {
  color: var(--neon-magenta);
  font-weight: 600;
}
</style>
