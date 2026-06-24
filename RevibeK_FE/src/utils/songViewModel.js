// Normalizes both radio-create (recommendedSongs) and radio-detail (songs)
// and playlist items into a common SongCard view model.
export function toSongVM(raw) {
  if (!raw) return null
  return {
    songId: raw.songId || raw.id,
    itemId: raw.itemId || null,
    title: raw.title || "제목 없음",
    artist: raw.artist || "Unknown",
    generation: raw.generation || "",
    genre: raw.genre || "",
    youtubeUrl: raw.youtubeUrl || "",
    youtubeId: raw.youtubeId || "",
    score: raw.score ?? null,
    reason: raw.reason || "",
  }
}

export function extractSongs(radio) {
  if (!radio) return []
  const list = radio.recommendedSongs || radio.songs || []
  return list.map(toSongVM)
}
