import { mockSongs } from "./songs"

// Public radio stories shown in the 리바이브닝 feed.
// Mirrors the backend GET /api/radio/public response shape.
const songsFor = (eras = [], genres = [], n = 3) => {
  const picked = mockSongs.filter(
    (s) => eras.includes(s.era) || genres.includes(s.genre)
  )
  return (picked.length ? picked : mockSongs).slice(0, n)
}

export const mockPublicRadio = [
  {
    radioSessionId: "pub-radio-1",
    userId: "u-2480",
    userNickname: "음악덕후",
    title: "퇴근길 라디오",
    story: "오늘은 지친 퇴근길에 위로받고 싶어요. 조용한 노래가 듣고 싶네요.",
    mood: "위로",
    situation: "퇴근길",
    generation: "2세대",
    djComment: "오늘 하루도 고생 많았어요. 천천히 마음을 내려놓아요.",
    publishedAt: "2026-06-15T19:20:00Z",
    likeCount: 12,
    liked: false,
    followed: false,
    recommendedSongs: songsFor(["2세대"], ["발라드"], 3),
  },
  {
    radioSessionId: "pub-radio-2",
    userId: "u-7781",
    userNickname: "새벽감성러",
    title: "새벽 두 시의 플레이리스트",
    story: "잠이 안 오는 새벽, 옛날 노래로 마음을 달래고 싶어요.",
    mood: "회상",
    situation: "새벽 감성",
    generation: "3세대",
    djComment: "잠들지 못하는 밤, 이 노래들이 곁에 있을게요.",
    publishedAt: "2026-06-14T02:10:00Z",
    likeCount: 34,
    liked: true,
    followed: false,
    recommendedSongs: songsFor(["3세대"], ["R&B"], 3),
  },
  {
    radioSessionId: "pub-radio-3",
    userId: "u-3312",
    userNickname: "운동메이트",
    title: "아침 러닝 부스터",
    story: "아침 러닝할 때 신나게 들을 곡이 필요해요!",
    mood: "활력",
    situation: "운동",
    generation: "4세대",
    djComment: "오늘도 한 걸음 더! 페이스를 끌어올려 봐요.",
    publishedAt: "2026-06-13T07:05:00Z",
    likeCount: 8,
    liked: false,
    followed: true,
    recommendedSongs: songsFor(["4세대"], ["댄스"], 3),
  },
]

export default mockPublicRadio
