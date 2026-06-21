import api, { USE_MOCK } from "./axios"
import { mockChallenges, mockChallengeTemplates } from "@/mocks/challenges"
import { mockUser } from "@/mocks/auth"

const delay = (ms = 250) => new Promise((r) => setTimeout(r, ms))

let store = mockChallenges.map((c) => ({ ...c }))
let seq = 100

export const challengeApi = {
  // GET /api/challenges/templates — public (no token required)
  async getTemplates() {
    if (USE_MOCK) {
      await delay()
      return mockChallengeTemplates.map((t) => ({ ...t }))
    }
    const { data } = await api.get("/challenges/templates", {
      _skipAuthRefresh: true,
    })
    return data
  },

  // GET /api/challenges — public
  async list() {
    if (USE_MOCK) {
      await delay()
      return store.map((c) => ({ ...c }))
    }
    const { data } = await api.get("/challenges")
    return data
  },

  // GET /api/challenges/me — auth
  async myChallenges() {
    if (USE_MOCK) {
      await delay()
      return store.filter((c) => c.joined).map((c) => ({ ...c }))
    }
    const { data } = await api.get("/challenges/me")
    return data
  },

  // POST /api/challenges — auth
  // payload: { title, description, startDate, endDate, targetCount, challengeType }
  async create(payload) {
    if (USE_MOCK) {
      await delay()
      const challenge = {
        id: `ch-${++seq}`,
        creatorId: mockUser.id,
        title: payload.title,
        description: payload.description || "",
        challengeType: payload.challengeType || "GENERATION_LISTENING",
        targetCount: payload.targetCount || 1,
        progressCount: 0,
        startDate: payload.startDate,
        endDate: payload.endDate,
        joined: true,
        participants: 1,
      }
      store.unshift(challenge)
      return challenge
    }
    const { data } = await api.post("/challenges", payload)
    return data
  },

  // POST /api/challenges/{id}/join — auth
  async join(id) {
    if (USE_MOCK) {
      await delay()
      const c = store.find((x) => x.id === id)
      if (c && !c.joined) {
        c.joined = true
        c.participants += 1
      }
      return { joined: true }
    }
    const { data } = await api.post(`/challenges/${id}/join`)
    return data
  },

  // PUT /api/challenges/{id}/progress?increment=N — auth
  async updateProgress(id, increment) {
    if (USE_MOCK) {
      await delay()
      const c = store.find((x) => x.id === id)
      if (c) c.progressCount = Math.min(c.progressCount + (increment ?? 1), c.targetCount)
      return c ? { ...c } : null
    }
    const { data } = await api.put(`/challenges/${id}/progress`, null, {
      params: { increment },
    })
    return data
  },

  // DELETE /api/challenges/{id}/leave — auth
  async leave(id) {
    if (USE_MOCK) {
      await delay()
      const c = store.find((x) => x.id === id)
      if (c && c.joined) {
        c.joined = false
        c.participants = Math.max(0, c.participants - 1)
      }
      return { joined: false }
    }
    const { data } = await api.delete(`/challenges/${id}/leave`)
    return data
  },

  // DELETE /api/challenges/{id} — auth (creator only)
  async remove(id) {
    if (USE_MOCK) {
      await delay()
      store = store.filter((c) => c.id !== id)
      return { ok: true }
    }
    const { data } = await api.delete(`/challenges/${id}`)
    return data
  },
}

export default challengeApi
