import { defineStore } from "pinia"
import authApi from "@/api/authApi"
import {
  TOKEN_KEYS,
  setTokens,
  clearTokens,
  getAccessToken,
} from "@/api/axios"

export const useAuthStore = defineStore("auth", {
  state: () => ({
    user: null,
    accessToken: null,
    refreshToken: null,
    initialized: false,
    loading: false,
  }),
  getters: {
    isAuthenticated: (s) => !!s.accessToken,
  },
  actions: {
    restoreSession() {
      this.accessToken = getAccessToken()
      this.refreshToken = sessionStorage.getItem(TOKEN_KEYS.refresh)
      const u = sessionStorage.getItem(TOKEN_KEYS.user)
      this.user = u ? JSON.parse(u) : null
      this.initialized = true
    },

    persist({ accessToken, refreshToken, user }) {
      this.accessToken = accessToken
      this.refreshToken = refreshToken
      this.user = user
      setTokens({ accessToken, refreshToken })
      if (user) sessionStorage.setItem(TOKEN_KEYS.user, JSON.stringify(user))
    },

    async login(credentials) {
      this.loading = true
      try {
        const res = await authApi.login(credentials)
        this.persist(res)
        return res
      } finally {
        this.loading = false
      }
    },

    async signup(payload) {
      this.loading = true
      try {
        const res = await authApi.signup(payload)
        // BE returns plain string "회원가입 완료" — do NOT call persist(res)
        return res
      } finally {
        this.loading = false
      }
    },

    async fetchMe() {
      const user = await authApi.me()
      this.user = user
      sessionStorage.setItem(TOKEN_KEYS.user, JSON.stringify(user))
      return user
    },

    async logout() {
      try {
        await authApi.logout()
      } catch (e) {
        // ignore network errors on logout
      }
      this.clearSession()
    },

    // Reset auth state + storage without hitting the logout endpoint
    // (used after account withdrawal).
    clearSession() {
      this.user = null
      this.accessToken = null
      this.refreshToken = null
      clearTokens()
    },

    // Merge updated profile fields into state + storage.
    setUser(user) {
      this.user = user
      if (user) sessionStorage.setItem(TOKEN_KEYS.user, JSON.stringify(user))
    },
  },
})
