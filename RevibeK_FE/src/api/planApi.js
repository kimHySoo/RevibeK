import api, { USE_MOCK } from "./axios"
import { mockPlans } from "@/mocks/plans"

const delay = (ms = 250) => new Promise((r) => setTimeout(r, ms))

let store = mockPlans.map((p) => ({ ...p }))
let seq = 100

export const planApi = {
  // GET /api/plans — auth
  async list() {
    if (USE_MOCK) {
      await delay()
      return store.map((p) => ({ ...p }))
    }
    const { data } = await api.get("/plans")
    return data
  },

  // POST /api/plans — auth
  async create(payload) {
    if (USE_MOCK) {
      await delay()
      const plan = {
        id: `plan-${++seq}`,
        planType: payload.planType,
        title: payload.title,
        description: payload.description || "",
        targetDate: payload.targetDate || "",
        completed: false,
      }
      store.unshift(plan)
      return plan
    }
    const { data } = await api.post("/plans", payload)
    return data
  },

  // PUT /api/plans/{id} — auth
  async update(id, payload) {
    if (USE_MOCK) {
      await delay()
      const idx = store.findIndex((p) => p.id === id)
      if (idx === -1) throw new Error("계획을 찾을 수 없습니다.")
      store[idx] = { ...store[idx], ...payload }
      return store[idx]
    }
    const { data } = await api.put(`/plans/${id}`, payload)
    return data
  },

  // DELETE /api/plans/{id} — auth
  async remove(id) {
    if (USE_MOCK) {
      await delay()
      store = store.filter((p) => p.id !== id)
      return { ok: true }
    }
    const { data } = await api.delete(`/plans/${id}`)
    return data
  },
}

export default planApi
