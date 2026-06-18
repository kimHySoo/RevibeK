import { defineStore } from "pinia"
import playlistApi, { radioToPlaylist } from "@/api/playlistApi"

const LAST_RADIO_KEY = "revibek.lastRadioResult"

export const usePlaylistStore = defineStore("playlist", {
  state: () => ({
    playlists: [],
    current: null,
    loading: false,
  }),
  actions: {
    async fetchAll() {
      this.loading = true
      try {
        this.playlists = await playlistApi.list()
      } finally {
        this.loading = false
      }
      return this.playlists
    },
    async fetchOne(id) {
      this.loading = true
      try {
        let pl = await playlistApi.getById(id)

        // Fallback: reconstruct from the last generated radio result.
        // The mock playlist store resets on full page reload, so a refresh
        // on /playlists/:id would otherwise lose a just-created playlist.
        if (!pl) {
          try {
            const saved = sessionStorage.getItem(LAST_RADIO_KEY)
            if (saved) {
              const radio = JSON.parse(saved)
              if (radio?.playlistId === id) {
                pl = radioToPlaylist(radio)
                await playlistApi.saveFromRadio(radio)
              }
            }
          } catch {
            // ignore malformed storage
          }
        }

        this.current = pl
      } finally {
        this.loading = false
      }
      return this.current
    },
    async create(payload) {
      const pl = await playlistApi.create(payload)
      this.playlists.unshift(pl)
      return pl
    },
    async createFromRadio({ title, moodTag, songs = [] }) {
      const pl = await playlistApi.create({ title, moodTag })
      for (const song of songs) {
        await playlistApi.addItem(pl.playlistId, song)
      }
      const full = await playlistApi.getById(pl.playlistId)
      const idx = this.playlists.findIndex(
        (p) => p.playlistId === pl.playlistId
      )
      if (idx === -1) this.playlists.unshift(full)
      else this.playlists[idx] = full
      return full
    },
    async update(playlistId, payload) {
      const updated = await playlistApi.update(playlistId, payload)
      if (this.current?.playlistId === playlistId && updated) {
        this.current = { ...this.current, ...updated }
      }
      const idx = this.playlists.findIndex((p) => p.playlistId === playlistId)
      if (idx !== -1 && updated) {
        this.playlists[idx] = { ...this.playlists[idx], ...updated }
      }
      return updated
    },
    async addItem(playlistId, song) {
      const pl = await playlistApi.addItem(playlistId, song)
      if (this.current?.playlistId === playlistId) this.current = pl
      return pl
    },
    async removeItem(playlistId, itemId) {
      await playlistApi.removeItem(playlistId, itemId)
      if (this.current?.playlistId === playlistId && this.current.items) {
        this.current.items = this.current.items.filter(
          (i) => i.itemId !== itemId
        )
      }
    },
    async remove(playlistId) {
      await playlistApi.remove(playlistId)
      this.playlists = this.playlists.filter(
        (p) => p.playlistId !== playlistId
      )
    },
  },
})
