import api, { USE_MOCK } from "./axios"

// Radio STORY likes — distinct from song likes (/api/likes/**).
// Endpoints: /api/radio/{radioSessionId}/likes
const delay = (ms = 200) => new Promise((r) => setTimeout(r, ms))

// mock like state keyed by radioSessionId
const likeState = new Map()

export const radioLikeApi = {
  // POST /api/radio/{id}/likes — auth
  async like(radioSessionId) {
    if (USE_MOCK) {
      await delay()
      likeState.set(radioSessionId, true)
      return { liked: true }
    }
    const { data } = await api.post(`/radio/${radioSessionId}/likes`)
    return data
  },

  // DELETE /api/radio/{id}/likes — auth
  async unlike(radioSessionId) {
    if (USE_MOCK) {
      await delay()
      likeState.set(radioSessionId, false)
      return { liked: false }
    }
    const { data } = await api.delete(`/radio/${radioSessionId}/likes`)
    return data
  },

  // GET /api/radio/{id}/likes/status — auth
  async status(radioSessionId) {
    if (USE_MOCK) {
      await delay()
      return { liked: !!likeState.get(radioSessionId) }
    }
    const { data } = await api.get(`/radio/${radioSessionId}/likes/status`)
    return data
  },

  // GET /api/radio/{id}/likes/count — public (no token required)
  async count(radioSessionId) {
    if (USE_MOCK) {
      await delay()
      return { count: 0 }
    }
    const { data } = await api.get(`/radio/${radioSessionId}/likes/count`, {
      _skipAuthRefresh: true,
    })
    return data
  },
}

export default radioLikeApi
