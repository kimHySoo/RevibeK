import api, { USE_MOCK } from "./axios"
import { buildMockRadio, mockRadioHistory } from "@/mocks/radio"

const delay = (ms = 1400) => new Promise((r) => setTimeout(r, ms))
const store = new Map()

// Build the request body that matches the backend RadioRequest DTO.
// Only these fields are sent — extra UI-only fields like `title`,
// `saveAsPlaylist`, `playlistTitle`, and `selectedSongs` are dropped.
function toRadioRequest(payload = {}) {
  return {
    mood: payload.mood ?? "",
    situation: payload.situation ?? "",
    desiredMood: payload.desiredMood ?? "",
    story: payload.story ?? "",
    era: payload.era ?? "",
    genre: payload.genre ?? "",
    videoType: payload.videoType ?? "",
    preferredArtist: payload.preferredArtist ?? "",
    excludedKeywords: payload.excludedKeywords ?? "",
    youtubeUrl: payload.youtubeUrl ?? "",
  }
}

export const radioApi = {
  async create(payload) {
    if (USE_MOCK) {
      await delay()
      // mock builder still uses the full story (saveAsPlaylist, etc.)
      const radio = buildMockRadio(payload)
      store.set(radio.radioSessionId, radio)
      if (radio.playlistId) store.set(radio.playlistId, radio)
      return radio
    }
    const { data } = await api.post("/radio", toRadioRequest(payload))
    return data
  },

  async getByPlaylistId(playlistId) {
    if (USE_MOCK) {
      await delay(400)
      const cached = store.get(playlistId)
      if (cached) {
        const { recommendedSongs, ...rest } = cached
        return { ...rest, songs: recommendedSongs }
      }
      const radio = buildMockRadio({ saveAsPlaylist: true })
      const { recommendedSongs, ...rest } = radio
      return { ...rest, playlistId, songs: recommendedSongs }
    }
    const { data } = await api.get(`/playlists/${playlistId}`)
    return data
  },

  async getById(id) {
    if (USE_MOCK) {
      await delay(400)
      const cached = store.get(id)
      if (cached) {
        // radio detail uses `songs` key
        const { recommendedSongs, ...rest } = cached
        return { ...rest, songs: recommendedSongs }
      }
      const radio = buildMockRadio({})
      const { recommendedSongs, ...rest } = radio
      return { ...rest, radioSessionId: id, songs: recommendedSongs }
    }
    const { data } = await api.get(`/radio/${id}`)
    return data
  },

  async myHistory() {
    if (USE_MOCK) {
      await delay(300)
      return mockRadioHistory
    }
    const { data } = await api.get("/radio/me")
    return data
  },
}

export default radioApi
