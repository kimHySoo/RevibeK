package com.ssafy.revibek.tts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ssafy.revibek.ai.dto.TtsSynthesizeRequestDto;
import com.ssafy.revibek.ai.dto.TtsSynthesizeResponseDto;
import com.ssafy.revibek.ai.service.GoogleTtsService;

@ExtendWith(MockitoExtension.class)
class TtsServiceTest {

    @Mock
    private GoogleTtsService googleTtsService;

    @InjectMocks
    private TtsService ttsService;

    @Test
    void 빈텍스트면_구글TTS를_호출하지않고_브라우저모드를_반환한다() {
        TtsResponseDto result = ttsService.synthesize("");

        assertThat(result.getMode()).isEqualTo("BROWSER_TTS");
        assertThat(result.getAudioUrl()).isNull();
    }

    @Test
    void 구글TTS가_비활성화면_브라우저모드DTO를_그대로_전달한다() {
        when(googleTtsService.synthesize(any(TtsSynthesizeRequestDto.class)))
                .thenReturn(TtsSynthesizeResponseDto.browserTts("디제이 멘트", "ko-KR", "ko-KR-Chirp3-HD-Vindemiatrix"));

        TtsResponseDto result = ttsService.synthesize("디제이 멘트");

        assertThat(result.getMode()).isEqualTo("BROWSER_TTS");
        assertThat(result.getAudioUrl()).isNull();
        assertThat(result.getText()).isEqualTo("디제이 멘트");
    }

    @Test
    void 구글TTS성공시_data_uri와_voice_정보를_포함한다() {
        when(googleTtsService.synthesize(any(TtsSynthesizeRequestDto.class)))
                .thenReturn(new TtsSynthesizeResponseDto(
                        "base64audio==", "audio/mpeg", "ko-KR", "ko-KR-Chirp3-HD-Vindemiatrix", "MP3"));

        TtsResponseDto result = ttsService.synthesize("디제이 멘트");

        assertThat(result.getMode()).isEqualTo("GOOGLE_TTS");
        assertThat(result.getAudioUrl()).isEqualTo("data:audio/mpeg;base64,base64audio==");
        assertThat(result.getVoice()).isEqualTo("ko-KR-Chirp3-HD-Vindemiatrix");
        assertThat(result.getAudioEncoding()).isEqualTo("MP3");
    }

    @Test
    void 구글TTS호출중예외가발생하면_브라우저fallback을_반환한다() {
        when(googleTtsService.synthesize(any(TtsSynthesizeRequestDto.class)))
                .thenThrow(new RuntimeException("Google TTS audioContent가 비어 있습니다."));

        TtsResponseDto result = ttsService.synthesize("디제이 멘트");

        assertThat(result.getMode()).isEqualTo("BROWSER_TTS");
        assertThat(result.getAudioUrl()).isNull();
        assertThat(result.getText()).isEqualTo("디제이 멘트");
    }
}
