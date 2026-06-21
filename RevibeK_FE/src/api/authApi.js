import api, { USE_MOCK, getRefreshToken } from "./axios"
import { buildMockAuth, mockUser } from "@/mocks/auth"

const delay = (ms = 600) => new Promise((r) => setTimeout(r, ms))

export const authApi = {
  async sendEmailCode(email) {
    if (USE_MOCK) {
      await delay()
      return { sent: true, devCode: "123456" }
    }
    const { data } = await api.post("/auth/email/send", { email })
    return data
  },

  async verifyEmailCode(email, code) {
    if (USE_MOCK) {
      await delay()
      return { verified: code === "123456" }
    }
    // 백엔드는 성공 시 plain text("이메일 인증 완료")를 반환하고 실패 시 4xx/5xx를 던진다.
    // axios가 여기까지 예외 없이 도달했다는 것 자체가 인증 성공을 의미하므로 verified로 정규화한다.
    await api.post("/auth/email/verify", { email, code })
    return { verified: true }
  },

  async signup({ email, password, nickname }) {
    if (USE_MOCK) {
      await delay()
      return buildMockAuth(email, nickname)
    }
    const { data } = await api.post("/auth/signup", {
      email,
      password,
      nickname,
    })
    return data
  },

  async login({ email, password }) {
    if (USE_MOCK) {
      await delay()
      if (!email || !password) throw new Error("이메일과 비밀번호를 입력해주세요.")
      return buildMockAuth(email)
    }
    const { data } = await api.post("/auth/login", { email, password })
    return data
  },

  async refresh(refreshToken) {
    const { data } = await api.post("/auth/refresh", { refreshToken })
    return data
  },

  async logout() {
    if (USE_MOCK) {
      await delay(200)
      return { ok: true }
    }
    const { data } = await api.post("/auth/logout", { refreshToken: getRefreshToken() })
    return data
  },

  async me() {
    if (USE_MOCK) {
      await delay(200)
      return mockUser
    }
    const { data } = await api.get("/users/me")
    return data
  },
}

export default authApi
