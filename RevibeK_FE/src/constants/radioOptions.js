// 라디오 추천에서 사용하는 감정/세대/장르 선택지의 단일 소스.
// label은 화면 표시 및 기존 API 요청 payload(한글 문자열) 그대로 유지하고,
// value는 백엔드 표준 코드(MoodCode/GenerationCode/GenreCode)와 1:1로 맞춘다.
// 감정은 정확히 7개, 세대는 2개+전체(ALL), 장르는 정확히 6개로 고정한다.

export const MOOD_OPTIONS = [
  { label: "지침", value: "TIRED" },
  { label: "설렘", value: "EXCITED" },
  { label: "회상", value: "NOSTALGIC" },
  { label: "자신감", value: "CONFIDENT" },
  { label: "위로", value: "COMFORT" },
  { label: "외로움", value: "LONELY" },
  { label: "신남", value: "ENERGETIC" },
]

export const GENERATION_OPTIONS = [
  { label: "2세대", value: "SECOND" },
  { label: "3세대", value: "THIRD" },
  { label: "전체", value: "ALL" },
]

export const GENRE_OPTIONS = [
  { label: "댄스", value: "DANCE" },
  { label: "발라드", value: "BALLAD" },
  { label: "R&B", value: "RNB" },
  { label: "힙합", value: "HIPHOP" },
  { label: "아이돌", value: "IDOL" },
  { label: "OST", value: "OST" },
  { label: "전체", value: "ALL" },
]
