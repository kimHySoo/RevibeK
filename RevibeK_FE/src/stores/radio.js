import { defineStore } from "pinia"
import radioApi from "@/api/radioApi"
import playlistApi from "@/api/playlistApi"

const PENDING_KEY = "revibek.pendingRadioStory"

export function emptyRadioStory() {
  return {
    title: "",
    mood: "",
    situation: "",
    desiredMood: "",
    story: "",
    generation: "",
    genre: "",
    videoType: "AI cover",
    preferredArtist: "",
    excludedKeywords: "",
    youtubeUrl: "",
    saveAsPlaylist: true,
    playlistTitle: "",
    selectedSongs: [],
  }
}

export const useRadioStore = defineStore("radio", {
  state: () => ({
    // current result
    current: null,
    history: [],

    // pending story (saved before login)
    pendingRadioStory: null,

    // generating state
    generating: {
      loading: false,
      step: 1,
      previewMoodText: "",
      previewFlowText: "",
      previewDjMentText: "",
      error: null,
    },
  }),
  getters: {
    hasPending: (s) => !!s.pendingRadioStory,
  },
  actions: {
    loadPending() {
      try {
        const raw = sessionStorage.getItem(PENDING_KEY)
        this.pendingRadioStory = raw ? JSON.parse(raw) : null
      } catch {
        this.pendingRadioStory = null
      }
      return this.pendingRadioStory
    },

    savePending(story) {
      this.pendingRadioStory = { ...emptyRadioStory(), ...story }
      sessionStorage.setItem(
        PENDING_KEY,
        JSON.stringify(this.pendingRadioStory)
      )
    },

    clearPending() {
      this.pendingRadioStory = null
      sessionStorage.removeItem(PENDING_KEY)
    },

    setGeneratingPreview(story) {
      const desired = story?.desiredMood || "위로"
      this.generating.previewMoodText = `${story?.situation || "조용한 밤"}, 오래된 기억을 ${desired ? desired + "와 함께 " : ""}꺼내는 감성`
      this.generating.previewFlowText =
        "잔잔한 도입 → 익숙한 후렴 → 감정 회복 → 마무리 위로"
      this.generating.previewDjMentText =
        "오늘은 마음이 조금 느리게 걷고 싶은 날 같아요..."
    },

    async generate(story) {
      this.generating.loading = true
      this.generating.error = null
      this.generating.step = 1
      this.setGeneratingPreview(story)

      // step animation
      const stepTimer = setInterval(() => {
        if (this.generating.step < 5) this.generating.step += 1
      }, 700)

      try {
        const radio = await radioApi.create(story)
        this.current = radio
        // If this radio was saved as a playlist, persist it so the
        // playlist detail page (/playlists/:id) can load it afterwards.
        if (radio?.playlistId) {
          try {
            await playlistApi.saveFromRadio(radio)
          } catch {
            // non-fatal — detail page has its own fallbacks
          }
        }
        clearInterval(stepTimer)
        this.generating.step = 5
        this.generating.loading = false
        this.clearPending()
        return radio
      } catch (err) {
        clearInterval(stepTimer)
        this.generating.loading = false
        this.generating.error =
          err?.response?.data?.message ||
          err?.message ||
          "라디오 생성에 실패했어요."
        throw err
      }
    },

    async fetchRadio(id) {
      const radio = await radioApi.getById(id)
      this.current = radio
      return radio
    },

    async fetchByPlaylist(playlistId) {
      // prefer the freshly generated result if it matches
      if (this.current && this.current.playlistId === playlistId) {
        const { recommendedSongs, songs, ...rest } = this.current
        return { ...rest, songs: songs || recommendedSongs }
      }
      const radio = await radioApi.getByPlaylistId(playlistId)
      this.current = radio
      return radio
    },

    async fetchHistory() {
      this.history = await radioApi.myHistory()
      return this.history
    },
  },
})
