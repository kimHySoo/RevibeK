import api, { USE_MOCK } from "./axios"
import { mockFollowing, mockFollowers } from "@/mocks/social"

const delay = (ms = 250) => new Promise((r) => setTimeout(r, ms))

let following = [...mockFollowing]
let followers = mockFollowers.map((f) => ({ ...f }))

export const followApi = {
  // GET /api/follows/followings — auth
  async getFollowing() {
    if (USE_MOCK) {
      await delay()
      return [...following]
    }
    const { data } = await api.get("/follows/followings")
    return data
  },

  // GET /api/follows/followers — auth
  async getFollowers() {
    if (USE_MOCK) {
      await delay()
      return [...followers]
    }
    const { data } = await api.get("/follows/followers")
    return data
  },

  // POST /api/follows/{userId} — auth
  async follow(userId) {
    if (USE_MOCK) {
      await delay()
      const target = followers.find((f) => f.userId === userId)
      if (target) target.following = true
      if (!following.some((f) => f.userId === userId) && target) {
        following.push({ ...target, following: true })
      }
      return { following: true }
    }
    const { data } = await api.post(`/follows/${userId}`)
    return data
  },

  // DELETE /api/follows/{userId} — auth
  async unfollow(userId) {
    if (USE_MOCK) {
      await delay()
      following = following.filter((f) => f.userId !== userId)
      const target = followers.find((f) => f.userId === userId)
      if (target) target.following = false
      return { following: false }
    }
    const { data } = await api.delete(`/follows/${userId}`)
    return data
  },
}

export default followApi
