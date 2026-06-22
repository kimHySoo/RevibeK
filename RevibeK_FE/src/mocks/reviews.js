// Mock reviews keyed loosely by songId. The current mock user is "user-uuid".
export const mockReviews = [
  {
    id: "r1",
    userId: "u1",
    userNickname: "음악덕후",
    songId: "song-001",
    content: "2세대 특유의 감성이 살아있어요.",
    rating: 5,
    createdAt: "2026-06-15T20:00:00",
    updatedAt: "2026-06-15T20:00:00",
  },
  {
    id: "r2",
    userId: "u2",
    userNickname: "케이팝러버",
    songId: "song-001",
    content: "가사가 정말 좋아요.",
    rating: 4,
    createdAt: "2026-06-14T18:30:00",
    updatedAt: "2026-06-14T18:30:00",
  },
  {
    id: "r3",
    userId: "user-uuid",
    userNickname: "리바이브",
    songId: "song-002",
    content: "비 오는 날 듣기 좋은 트랙이에요.",
    rating: 5,
    createdAt: "2026-06-13T09:10:00",
    updatedAt: "2026-06-13T09:10:00",
  },
]
