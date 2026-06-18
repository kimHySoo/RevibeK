import { mockSongs } from "./songs"

export const mockPlaylists = [
  {
    playlistId: "playlist-1",
    title: "비 오는 밤의 2세대 감성",
    moodTag: "위로",
    coverGradient: "linear-gradient(135deg,#ff3ea5,#9b5cff)",
    items: [
      { itemId: "item-1", ...mockSongs[0] },
      { itemId: "item-2", ...mockSongs[1] },
      { itemId: "item-3", ...mockSongs[2] },
    ],
  },
  {
    playlistId: "playlist-2",
    title: "주말 아침 드라이브",
    moodTag: "설렘",
    coverGradient: "linear-gradient(135deg,#2ee6e6,#9b5cff)",
    items: [
      { itemId: "item-4", ...mockSongs[3] },
      { itemId: "item-5", ...mockSongs[5] },
    ],
  },
  {
    playlistId: "playlist-3",
    title: "조용한 새벽 플레이리스트",
    moodTag: "차분함",
    coverGradient: "linear-gradient(135deg,#9b5cff,#ff3ea5)",
    items: null,
  },
]
