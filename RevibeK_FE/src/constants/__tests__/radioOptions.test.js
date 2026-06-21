import { describe, expect, it } from "vitest"
import { GENERATION_OPTIONS, GENRE_OPTIONS, MOOD_OPTIONS } from "@/constants/radioOptions"

describe("radioOptions", () => {
  it("감정 옵션은 정확히 7개이고 표준 코드와 1:1로 매칭된다", () => {
    expect(MOOD_OPTIONS).toHaveLength(7)
    expect(MOOD_OPTIONS.map((o) => o.value)).toEqual([
      "TIRED",
      "EXCITED",
      "NOSTALGIC",
      "CONFIDENT",
      "COMFORT",
      "LONELY",
      "ENERGETIC",
    ])
    expect(MOOD_OPTIONS.map((o) => o.label)).toEqual([
      "지침",
      "설렘",
      "회상",
      "자신감",
      "위로",
      "외로움",
      "신남",
    ])
  })

  it("세대 옵션은 2세대/3세대/전체(ALL) 3개다", () => {
    expect(GENERATION_OPTIONS).toHaveLength(3)
    expect(GENERATION_OPTIONS.map((o) => o.value)).toEqual(["SECOND", "THIRD", "ALL"])
  })

  it("장르 옵션은 정확히 6개다", () => {
    expect(GENRE_OPTIONS).toHaveLength(6)
    expect(GENRE_OPTIONS.map((o) => o.value)).toEqual([
      "DANCE",
      "BALLAD",
      "RNB",
      "HIPHOP",
      "IDOL",
      "OST",
    ])
  })
})
