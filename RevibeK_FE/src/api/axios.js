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

export function getAccessToken() {
  return localStorage.getItem(TOKEN_KEYS.access)
}
export function getRefreshToken() {
  return localStorage.getItem(TOKEN_KEYS.refresh)
}
export function setTokens({ accessToken, refreshToken }) {
  if (accessToken) localStorage.setItem(TOKEN_KEYS.access, accessToken)
  if (refreshToken) localStorage.setItem(TOKEN_KEYS.refresh, refreshToken)
}
export function clearTokens() {
  localStorage.removeItem(TOKEN_KEYS.access)
  localStorage.removeItem(TOKEN_KEYS.refresh)
  localStorage.removeItem(TOKEN_KEYS.user)
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

export const USE_MOCK = String(import.meta.env.VITE_USE_MOCK) !== "false"

export default api
