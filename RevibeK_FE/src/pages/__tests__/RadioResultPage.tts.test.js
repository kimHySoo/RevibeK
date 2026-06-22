import { describe, it, expect, beforeEach, afterEach, vi } from "vitest"
import { mount, flushPromises } from "@vue/test-utils"
import RadioResultPage from "../RadioResultPage.vue"

const fetchRadioMock = vi.fn()

vi.mock("@/stores/radio", () => ({
  useRadioStore: () => ({ fetchRadio: fetchRadioMock }),
}))
vi.mock("@/stores/playlist", () => ({
  usePlaylistStore: () => ({ createFromRadio: vi.fn() }),
}))
vi.mock("@/stores/ui", () => ({
  useUiStore: () => ({ success: vi.fn(), error: vi.fn(), notify: vi.fn() }),
}))
vi.mock("vue-router", () => ({
  useRoute: () => ({ params: { id: "session-1" } }),
  useRouter: () => ({ push: vi.fn() }),
}))
vi.mock("@/api/playlistApi", () => ({ default: { addItemsBatch: vi.fn() } }))

const stubs = {
  AppShell: { template: "<div><slot /></div>" },
  BaseButton: { template: "<button><slot /></button>" },
  NeonWaveform: true,
  TrackRow: true,
  LoadingOverlay: true,
}

async function mountAndWaitForAutoplay() {
  const wrapper = mount(RadioResultPage, { global: { stubs } })
  await flushPromises()
  await vi.advanceTimersByTimeAsync(600)
  await flushPromises()
  return wrapper
}

describe("RadioResultPage — TTS 응답에 따른 실제 음성 재생 분기", () => {
  beforeEach(() => {
    vi.useFakeTimers()
    fetchRadioMock.mockReset()
    window.speechSynthesis = {
      cancel: vi.fn(),
      speak: vi.fn(),
      speaking: false,
      pending: false,
    }
    window.SpeechSynthesisUtterance = vi.fn().mockImplementation(function (text) {
      this.text = text
    })
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it("tts.mode=GOOGLE_TTS이고 audioUrl이 있으면 Audio로 해당 audioUrl을 재생한다", async () => {
    window.Audio = vi.fn().mockImplementation(function () {
      return {
        play: vi.fn().mockResolvedValue(undefined),
        pause: vi.fn(),
        onended: null,
        onerror: null,
      }
    })
    fetchRadioMock.mockResolvedValue({
      djMent: "안녕하세요, DJ입니다",
      tts: {
        mode: "GOOGLE_TTS",
        text: "안녕하세요, DJ입니다",
        audioUrl: "data:audio/mpeg;base64,abc123",
      },
      songs: [],
    })

    await mountAndWaitForAutoplay()

    expect(window.Audio).toHaveBeenCalledWith("data:audio/mpeg;base64,abc123")
    expect(window.speechSynthesis.speak).not.toHaveBeenCalled()
  })

  it("tts.mode=BROWSER_TTS이면 Web Speech API(SpeechSynthesisUtterance)로 재생한다", async () => {
    fetchRadioMock.mockResolvedValue({
      djMent: "안녕하세요, DJ입니다",
      tts: { mode: "BROWSER_TTS", text: "안녕하세요, DJ입니다", audioUrl: null },
      songs: [],
    })

    await mountAndWaitForAutoplay()

    expect(window.speechSynthesis.speak).toHaveBeenCalled()
  })

  it("Google 오디오 재생 중 오류(onerror)가 발생하면 브라우저 TTS로 자동 fallback한다", async () => {
    let createdAudio
    window.Audio = vi.fn().mockImplementation(function () {
      createdAudio = {
        play: vi.fn().mockResolvedValue(undefined),
        pause: vi.fn(),
        onended: null,
        onerror: null,
      }
      return createdAudio
    })
    fetchRadioMock.mockResolvedValue({
      djMent: "안녕하세요, DJ입니다",
      tts: {
        mode: "GOOGLE_TTS",
        text: "안녕하세요, DJ입니다",
        audioUrl: "data:audio/mpeg;base64,broken",
      },
      songs: [],
    })

    await mountAndWaitForAutoplay()
    createdAudio.onerror()

    expect(window.speechSynthesis.speak).toHaveBeenCalled()
  })

  it("djMent와 tts가 모두 없으면 재생을 시도하지 않고 안전하게 종료한다", async () => {
    window.Audio = vi.fn()
    fetchRadioMock.mockResolvedValue({ djMent: null, tts: null, songs: [] })

    await mountAndWaitForAutoplay()

    expect(window.Audio).not.toHaveBeenCalled()
    expect(window.speechSynthesis.speak).not.toHaveBeenCalled()
  })
})
