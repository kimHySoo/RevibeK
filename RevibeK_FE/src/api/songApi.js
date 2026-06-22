import api, { USE_MOCK } from "./axios"
import { mockSongs } from "@/mocks/songs"

const delay = (ms = 300) => new Promise((r) => setTimeout(r, ms))

export const songApi = {
  async list() {
    if (USE_MOCK) {
      await delay()
      return mockSongs
    }
    const { data } = await api.get("/songs")
    return data
  },

  async getById(id) {
    if (USE_MOCK) {
      await delay()
      return mockSongs.find((s) => s.songId === id) || null
    }
    const { data } = await api.get(`/songs/${id}`)
    return data
  },

  async search(title) {
    if (USE_MOCK) {
      await delay()
      return mockSongs.filter((s) => s.title.includes(title || ""))
    }
    const { data } = await api.get("/songs/search", { params: { title } })
    return data
  },

  async byGenre(genre) {
    if (USE_MOCK) {
      await delay()
      return mockSongs.filter((s) => s.genre === genre)
    }
    const { data } = await api.get("/songs/genre", { params: { genre } })
    return data
  },

  async recommend() {
    if (USE_MOCK) {
      await delay()
      return mockSongs.slice(0, 5)
    }
    const { data } = await api.get("/songs/recommend")
    return data
  },
}

export default songApi
