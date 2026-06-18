import api, { USE_MOCK } from "./axios"
import { mockPlaylists } from "@/mocks/playlists"
import { mockSongs } from "@/mocks/songs"

const delay = (ms = 400) => new Promise((r) => setTimeout(r, ms))
let local = JSON.parse(JSON.stringify(mockPlaylists))

// Convert a generated radio result into a playlist-shaped object.
export function radioToPlaylist(radio) {
  if (!radio) return null
  const sourceSongs = radio.items || radio.recommendedSongs || radio.songs || []
  return {
    playlistId: radio.playlistId,
    title:
      radio.playlistTitle ||
      radio.title ||
      `${radio.mood || "오늘의"} ${radio.genre || ""} 라디오`.trim(),
    moodTag: radio.desiredMood || radio.mood || "",
    coverGradient: "linear-gradient(135deg,#9b5cff,#2ee6e6)",
    items: sourceSongs.map((s, i) => ({
      itemId: s.itemId || `item-${radio.playlistId}-${i}`,
      ...s,
    })),
  }
}

export const playlistApi = {
  async list() {
    if (USE_MOCK) {
      await delay()
      return local
    }
    const { data } = await api.get("/playlists")
    return data
  },

  async getById(playlistId) {
    if (USE_MOCK) {
      await delay()
      return local.find((p) => p.playlistId === playlistId) || null
    }
    const { data } = await api.get(`/playlists/${playlistId}`)
    return data
  },

  // Persist a generated radio result as a playlist (mock only — the real
  // backend already creates the playlist server-side during generation).
  async saveFromRadio(radio) {
    if (!radio?.playlistId) return null
    if (USE_MOCK) {
      const existing = local.find((p) => p.playlistId === radio.playlistId)
      if (existing) return existing
      const pl = radioToPlaylist(radio)
      local.unshift(pl)
      return pl
    }
    return radio
  },

  async create({ title, name, moodTag, isPublic = false }) {
    const resolvedName = name || title || ""
    if (USE_MOCK) {
      await delay()
      const pl = {
        playlistId: `playlist-${Date.now()}`,
        title: resolvedName,
        moodTag: moodTag || "",
        coverGradient: "linear-gradient(135deg,#9b5cff,#2ee6e6)",
        items: [],
      }
      local.unshift(pl)
      return pl
    }
    const { data } = await api.post("/playlists", { name: resolvedName, moodTag, isPublic })
    return data
  },

  // PUT /api/playlists/{id} — update name / moodTag / isPublic
  async update(playlistId, payload) {
    if (USE_MOCK) {
      await delay()
      const pl = local.find((p) => p.playlistId === playlistId)
      if (pl) {
        if (payload.name != null) pl.title = payload.name
        if (payload.moodTag != null) pl.moodTag = payload.moodTag
        if (payload.isPublic != null) pl.isPublic = payload.isPublic
      }
      return pl ? { ...pl } : null
    }
    const { data } = await api.put(`/playlists/${playlistId}`, payload)
    return data
  },

  async addItem(playlistId, song) {
    if (USE_MOCK) {
      await delay()
      const pl = local.find((p) => p.playlistId === playlistId)
      if (pl) {
        if (!pl.items) pl.items = []
        pl.items.push({ itemId: `item-${Date.now()}`, ...song })
      }
      return pl
    }
    const { data } = await api.post(`/playlists/${playlistId}/items`, song)
    return data
  },

  // Add many songs at once. Backend: POST /playlists/{id}/items/batch
  // Returns { added, skipped } counts.
  async addItemsBatch(playlistId, songIds = []) {
    if (USE_MOCK) {
      await delay()
      const pl = local.find((p) => p.playlistId === playlistId)
      let added = 0
      let skipped = 0
      if (pl) {
        if (!pl.items) pl.items = []
        const existing = new Set(
          pl.items.map((i) => i.songId || i.id).filter(Boolean)
        )
        songIds.forEach((sid, idx) => {
          if (!sid || existing.has(sid)) {
            skipped += 1
            return
          }
          const found = mockSongs.find((s) => (s.songId || s.id) === sid)
          pl.items.push({
            itemId: `item-${Date.now()}-${idx}`,
            ...(found || { songId: sid }),
          })
          existing.add(sid)
          added += 1
        })
      }
      return { added, skipped }
    }
    const { data } = await api.post(`/playlists/${playlistId}/items/batch`, {
      songIds,
    })
    return data
  },

  async removeItem(playlistId, itemId) {
    if (USE_MOCK) {
      await delay()
      const pl = local.find((p) => p.playlistId === playlistId)
      if (pl && pl.items) {
        pl.items = pl.items.filter((i) => i.itemId !== itemId)
      }
      return { ok: true }
    }
    const { data } = await api.delete(
      `/playlists/${playlistId}/items/${itemId}`
    )
    return data
  },

  async remove(playlistId) {
    if (USE_MOCK) {
      await delay()
      local = local.filter((p) => p.playlistId !== playlistId)
      return { ok: true }
    }
    const { data } = await api.delete(`/playlists/${playlistId}`)
    return data
  },
}

export default playlistApi
