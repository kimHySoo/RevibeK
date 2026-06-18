import api, { USE_MOCK } from "./axios"
import { mockSongs } from "@/mocks/songs"

const delay = (ms = 250) => new Promise((r) => setTimeout(r, ms))
const liked = new Set(["song-002"])

export const likeApi = {
  async like(songId) {
    if (USE_MOCK) {
      await delay()
      liked.add(songId)
      return { liked: true }
    }
    const { data } = await api.post("/likes", { songId })
    return data
  },

  async unlike(songId) {
    if (USE_MOCK) {
      await delay()
      liked.delete(songId)
      return { liked: false }
    }
    const { data } = await api.delete(`/likes/${songId}`)
    return data
  },

  async status(songId) {
    if (USE_MOCK) {
      await delay(120)
      return { liked: liked.has(songId) }
    }
    const { data } = await api.get(`/likes/${songId}/status`)
    return data
  },

  async likedSongs() {
    if (USE_MOCK) {
      await delay()
      return mockSongs.filter((s) => liked.has(s.songId))
    }
    const { data } = await api.get("/likes/songs")
    return data
  },
}

export default likeApi
