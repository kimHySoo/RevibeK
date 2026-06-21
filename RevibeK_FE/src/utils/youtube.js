// Extract an 11-char YouTube video id from a raw id or any YouTube URL form.
export function getYoutubeId(value) {
  if (!value) return ""
  if (/^[a-zA-Z0-9_-]{11}$/.test(value)) return value
  const match = value.match(
    /(?:youtu\.be\/|youtube\.com\/(?:watch\?v=|embed\/|shorts\/))([a-zA-Z0-9_-]{11})/
  )
  return match ? match[1] : ""
}

// Resolve a playable YouTube id from a song-like object.
export function getSongYoutubeId(song) {
  if (!song) return ""
  return getYoutubeId(song.youtubeId || song.youtubeUrl || "")
}
