import axios from "axios"

export const TOKEN_KEYS = {
  access: "revibek.accessToken",
  refresh: "revibek.refreshToken",
  user: "revibek.user",
}

const baseURL = import.meta.env.VITE_API_BASE_URL || "/api"

const api = axios.create({
  baseURL,
  headers: { "Content-Type": "application/json" },
  timeout: 200000,
})

// sessionStorage를 쓰는 이유: 탭/브라우저를 닫으면 로그아웃되고, 같은 탭에서의
// 새로고침이나 SPA 내 라우팅 이동에는 로그인 상태가 유지되어야 하기 때문
// (docs/answer/frontend_refact.md 참고).
export function getAccessToken() {
  return sessionStorage.getItem(TOKEN_KEYS.access)
}
export function getRefreshToken() {
  return sessionStorage.getItem(TOKEN_KEYS.refresh)
}
export function setTokens({ accessToken, refreshToken }) {
  if (accessToken) sessionStorage.setItem(TOKEN_KEYS.access, accessToken)
  if (refreshToken) sessionStorage.setItem(TOKEN_KEYS.refresh, refreshToken)
}
export function clearTokens() {
  sessionStorage.removeItem(TOKEN_KEYS.access)
  sessionStorage.removeItem(TOKEN_KEYS.refresh)
  sessionStorage.removeItem(TOKEN_KEYS.user)
}

// Request interceptor: attach access token
api.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Response interceptor: refresh token on 401
let isRefreshing = false
let queue = []

function processQueue(error, token = null) {
  queue.forEach((p) => (error ? p.reject(error) : p.resolve(token)))
  queue = []
}

api.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config
    const status = error.response?.status

    if (status === 401 && !original._retry && !original._skipAuthRefresh) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          queue.push({ resolve, reject })
        })
          .then((token) => {
            original.headers.Authorization = `Bearer ${token}`
            return api(original)
          })
          .catch((err) => Promise.reject(err))
      }

      original._retry = true
      isRefreshing = true

      try {
        const refreshToken = getRefreshToken()
        if (!refreshToken) throw new Error("No refresh token")

        const { data } = await axios.post(
          `${baseURL}/auth/refresh`,
          { refreshToken },
          { headers: { "Content-Type": "application/json" } }
        )
        setTokens({
          accessToken: data.accessToken,
          refreshToken: data.refreshToken,
        })
        processQueue(null, data.accessToken)
        original.headers.Authorization = `Bearer ${data.accessToken}`
        return api(original)
      } catch (err) {
        processQueue(err, null)
        clearTokens()
        // redirect to login
        if (typeof window !== "undefined") {
          window.location.assign("/login")
        }
        return Promise.reject(err)
      } finally {
        isRefreshing = false
      }
    }

    return Promise.reject(error)
  }
)

// VITE_USE_MOCK이 명시적으로 "true"일 때만 Mock을 사용한다(opt-in).
// 이전에는 `!== "false"`로 비교해 환경변수가 비어 있거나 누락되면(예: `.env.development` 없이
// 그냥 `npm run build`) 기본값이 Mock으로 켜지는 문제가 있었다 — 실제 배포에서 Java 백엔드를
// 호출하지 않고 조용히 mocks/radio.js의 BROWSER_TTS 응답만 쓰게 될 위험이 있었다.
export const USE_MOCK = String(import.meta.env.VITE_USE_MOCK) === "true"

export default api
