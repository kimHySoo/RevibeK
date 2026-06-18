import api, { USE_MOCK } from "./axios"
import { mockUser } from "@/mocks/auth"

const delay = (ms = 250) => new Promise((r) => setTimeout(r, ms))

// mutable copy used only in mock mode so edits "stick" within a session
let mockProfile = { ...mockUser }

export const userApi = {
  async me() {
    if (USE_MOCK) {
      await delay()
      return { ...mockProfile }
    }
    const { data } = await api.get("/users/me")
    return data
  },

  // PUT /api/users/me — nickname / email / (optional) password
  async updateMe(payload) {
    if (USE_MOCK) {
      await delay()
      const { password, ...rest } = payload || {}
      mockProfile = { ...mockProfile, ...rest }
      return { ...mockProfile }
    }
    const { data } = await api.put("/users/me", payload)
    return data
  },

  // DELETE /api/users/me — account withdrawal
  async deleteMe() {
    if (USE_MOCK) {
      await delay()
      return { ok: true }
    }
    const { data } = await api.delete("/users/me")
    return data
  },
}

export default userApi
