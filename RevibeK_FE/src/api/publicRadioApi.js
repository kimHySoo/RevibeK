import api, { USE_MOCK } from "./axios"
import { mockPublicRadio } from "@/mocks/publicRadio"

const delay = (ms = 300) => new Promise((r) => setTimeout(r, ms))

// in-memory copy so mock like/follow toggles persist within a session
let store = mockPublicRadio.map((r) => ({ ...r }))

export const publicRadioApi = {
  // GET /api/radio/public?sort=latest|popular — public (no token required)
  async getFeed(sort = "latest") {
    if (USE_MOCK) {
      await delay()
      const list = store.map((r) => ({ ...r }))
      if (sort === "popular") {
        return list.sort((a, b) => b.likeCount - a.likeCount)
      }
      return list.sort(
        (a, b) => new Date(b.publishedAt) - new Date(a.publishedAt)
      )
    }
    const { data } = await api.get("/radio/public", {
      params: { sort },
      _skipAuthRefresh: true,
    })
    return data
  },

  // GET /api/radio/public/{id} — public
  async getDetail(radioSessionId) {
    if (USE_MOCK) {
      await delay()
      const found = store.find((r) => r.radioSessionId === radioSessionId)
      return found ? { ...found } : null
    }
    const { data } = await api.get(`/radio/public/${radioSessionId}`, {
      _skipAuthRefresh: true,
    })
    return data
  },

  // PUT /api/radio/{id}/public — auth (toggle own radio visibility)
  async togglePublic(radioSessionId, isPublic) {
    if (USE_MOCK) {
      await delay()
      return {
        radioSessionId,
        isPublic,
        publishedAt: isPublic ? new Date().toISOString() : null,
      }
    }
    const { data } = await api.put(`/radio/${radioSessionId}/public`, {
      isPublic,
    })
    return data
  },

  // local helper so the feed reflects like/follow changes without a refetch
  _applyLocal(radioSessionId, patch) {
    const item = store.find((r) => r.radioSessionId === radioSessionId)
    if (item) Object.assign(item, patch)
  },
}

export default publicRadioApi
