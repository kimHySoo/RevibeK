import { mockSongs } from "./songs"

let counter = 0

export function buildMockRadio(payload = {}) {
  counter += 1
  const id = `radio-${Date.now()}-${counter}`
  const generation = payload.generation || "2세대"
  const genre = payload.genre || "발라드"
  const mood = payload.mood || "외로움"
  const desiredMood = payload.desiredMood || "위로"

  const recommendedSongs = mockSongs
    .filter((s) => s.generation === generation || s.genre === genre)
    .slice(0, 5)
  const songs = recommendedSongs.length ? recommendedSongs : mockSongs.slice(0, 5)

  const djMent = `오늘 밤, ${mood}을(를) 느끼는 당신에게 조용히 ${desiredMood}을(를) 건넬 ${generation} ${genre} 트랙들을 준비했어요.`

  return {
    radioSessionId: id,
    userId: "user-uuid",
    mood,
    story: payload.story || "",
    generation,
    genre,
    situation: payload.situation || "",
    desiredMood,
    videoType: payload.videoType || "AI cover",
    preferredArtist: payload.preferredArtist || "",
    excludedKeywords: payload.excludedKeywords || "",
    djMent,
    playlistId: payload.saveAsPlaylist ? `playlist-${id}` : null,
    playlistTitle:
      payload.playlistTitle || payload.title || `${mood} ${genre} 라디오`,
    recommendationSource: "DB_MOOD_ERA_GENRE",
    tts: {
      mode: "BROWSER_TTS",
      text: djMent,
      audioUrl: null,
    },
    recommendedSongs: songs,
  }
}

export const mockRadioHistory = [
  {
    radioSessionId: "radio-history-1",
    mood: "설렘",
    generation: "3세대",
    genre: "댄스",
    situation: "주말 아침 드라이브",
    djMent: "산뜻한 하루를 여는 트랙들이에요.",
    createdAt: "2026-06-10T09:00:00Z",
    isPublic: true,
    publishedAt: "2026-06-10T10:00:00Z",
  },
  {
    radioSessionId: "radio-history-2",
    mood: "외로움",
    generation: "2세대",
    genre: "발라드",
    situation: "퇴근 후 비 오는 버스",
    djMent: "조용히 마음을 감싸줄 노래들이에요.",
    createdAt: "2026-06-08T21:30:00Z",
    isPublic: false,
    publishedAt: null,
  },
]
