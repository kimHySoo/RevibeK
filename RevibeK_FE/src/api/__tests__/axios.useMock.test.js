import { describe, it, expect, beforeEach, vi } from "vitest"

vi.mock("axios", () => ({
  default: {
    create: () => ({
      post: vi.fn(),
      get: vi.fn(),
      interceptors: {
        request: { use: vi.fn() },
        response: { use: vi.fn() },
      },
    }),
  },
}))

// axios.js의 USE_MOCK은 모듈 로드 시 한 번만 계산되므로, 환경변수를 바꿔가며
// 테스트하려면 매번 모듈 캐시를 비우고(resetModules) 동적 import로 다시 읽어야 한다.
describe("USE_MOCK 기본값 (axios.js)", () => {
  beforeEach(() => {
    vi.resetModules()
    vi.unstubAllEnvs()
  })

  it('VITE_USE_MOCK="true"이면 USE_MOCK은 true다', async () => {
    vi.stubEnv("VITE_USE_MOCK", "true")
    const { USE_MOCK } = await import("../axios.js")
    expect(USE_MOCK).toBe(true)
  })

  it('VITE_USE_MOCK="false"이면 USE_MOCK은 false다', async () => {
    vi.stubEnv("VITE_USE_MOCK", "false")
    const { USE_MOCK } = await import("../axios.js")
    expect(USE_MOCK).toBe(false)
  })

  it("VITE_USE_MOCK이 설정되지 않으면 기본값은 false다 (Mock이 저절로 켜지면 안 된다)", async () => {
    const { USE_MOCK } = await import("../axios.js")
    expect(USE_MOCK).toBe(false)
  })

  it('VITE_USE_MOCK="TRUE"처럼 대소문자가 다르면 Mock으로 인정하지 않는다(명시적 "true"만 허용)', async () => {
    vi.stubEnv("VITE_USE_MOCK", "TRUE")
    const { USE_MOCK } = await import("../axios.js")
    expect(USE_MOCK).toBe(false)
  })
})
