import api, { USE_MOCK } from "./axios"
import { mockReviews } from "@/mocks/reviews"
import { mockUser } from "@/mocks/auth"

const delay = (ms = 250) => new Promise((r) => setTimeout(r, ms))

// session-local copy so mock CRUD persists while the app is open
let store = [...mockReviews]
let seq = 100

export const reviewApi = {
  // GET /api/reviews/songs/{songId} — public
  async getBySong(songId) {
    if (USE_MOCK) {
      await delay()
      return store
        .filter((r) => r.songId === songId)
        .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
    }
    const { data } = await api.get(`/reviews/songs/${songId}`)
    return data
  },

  // GET /api/reviews/me — auth
  async mine() {
    if (USE_MOCK) {
      await delay()
      return store
        .filter((r) => r.userId === mockUser.id)
        .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
    }
    const { data } = await api.get("/reviews/me")
    return data
  },

  // POST /api/reviews — auth { songId, content, rating }
  async create(payload) {
    if (USE_MOCK) {
      await delay()
      const now = new Date().toISOString()
      const review = {
        id: `r${++seq}`,
        userId: mockUser.id,
        userNickname: mockUser.nickname,
        songId: payload.songId,
        content: payload.content,
        rating: payload.rating,
        createdAt: now,
        updatedAt: now,
      }
      store.unshift(review)
      return review
    }
    const { data } = await api.post("/reviews", payload)
    return data
  },

  // PUT /api/reviews/{reviewId} — auth (owner only) { content, rating }
  async update(reviewId, payload) {
    if (USE_MOCK) {
      await delay()
      const idx = store.findIndex((r) => r.id === reviewId)
      if (idx === -1) throw new Error("리뷰를 찾을 수 없습니다.")
      store[idx] = {
        ...store[idx],
        content: payload.content,
        rating: payload.rating,
        updatedAt: new Date().toISOString(),
      }
      return store[idx]
    }
    const { data } = await api.put(`/reviews/${reviewId}`, payload)
    return data
  },

  // DELETE /api/reviews/{reviewId} — auth (owner only)
  async remove(reviewId) {
    if (USE_MOCK) {
      await delay()
      store = store.filter((r) => r.id !== reviewId)
      return { ok: true }
    }
    const { data } = await api.delete(`/reviews/${reviewId}`)
    return data
  },
}

export default reviewApi
