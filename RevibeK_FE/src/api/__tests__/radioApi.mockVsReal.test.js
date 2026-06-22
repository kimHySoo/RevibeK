import { describe, it, expect, beforeEach, vi } from "vitest"

const { postMock, getMock } = vi.hoisted(() => ({
  postMock: vi.fn(),
  getMock: vi.fn(),
}))

vi.mock("axios", () => ({
  default: {
    create: () => ({
      post: postMock,
      get: getMock,
      interceptors: {
        request: { use: vi.fn() },
        response: { use: vi.fn() },
      },
    }),
  },
}))

describe("radioApi.create — Mock과 실제 백엔드 호출 분기", () => {
  beforeEach(() => {
    vi.resetModules()
    vi.unstubAllEnvs()
    postMock.mockReset()
    getMock.mockReset()
  })

  it("VITE_USE_MOCK=true이면 실제 POST를 호출하지 않고 mocks/radio.js의 BROWSER_TTS 응답을 반환한다", async () => {
    vi.stubEnv("VITE_USE_MOCK", "true")
    const { default: radioApi } = await import("../radioApi.js")

    const result = await radioApi.create({ mood: "신남" })

    expect(postMock).not.toHaveBeenCalled()
    expect(result.tts).toEqual({
      mode: "BROWSER_TTS",
      text: result.djMent,
      audioUrl: null,
    })
  })

  it("VITE_USE_MOCK=false이면 실제 POST /radio를 호출하고 백엔드의 GOOGLE_TTS 응답을 그대로 반환한다", async () => {
    vi.stubEnv("VITE_USE_MOCK", "false")
    postMock.mockResolvedValueOnce({
      data: {
        radioSessionId: "s1",
        djMent: "실제 백엔드 DJ 멘트",
        tts: {
          mode: "GOOGLE_TTS",
          text: "실제 백엔드 DJ 멘트",
          audioUrl: "data:audio/mpeg;base64,abc123",
        },
        recommendedSongs: [],
      },
    })
    const { default: radioApi } = await import("../radioApi.js")

    const result = await radioApi.create({ mood: "신남" })

    expect(postMock).toHaveBeenCalledWith("/radio", expect.any(Object))
    expect(result.tts.mode).toBe("GOOGLE_TTS")
    expect(result.tts.audioUrl).toBe("data:audio/mpeg;base64,abc123")
  })

  it("VITE_USE_MOCK=false에서 백엔드 호출이 실패하면 Mock으로 조용히 대체하지 않고 에러를 그대로 던진다", async () => {
    vi.stubEnv("VITE_USE_MOCK", "false")
    postMock.mockRejectedValueOnce(new Error("Network Error"))
    const { default: radioApi } = await import("../radioApi.js")

    await expect(radioApi.create({ mood: "신남" })).rejects.toThrow("Network Error")
    expect(postMock).toHaveBeenCalled()
  })
})
