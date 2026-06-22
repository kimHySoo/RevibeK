package com.ssafy.revibek.tts;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.ssafy.revibek.ai.dto.TtsSynthesizeRequestDto;
import com.ssafy.revibek.ai.dto.TtsSynthesizeResponseDto;
import com.ssafy.revibek.ai.service.GoogleTtsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TtsService implements TtsClient {

    private final GoogleTtsService googleTtsService;

    @Override
    public TtsResponseDto synthesize(String text) {
        if (!StringUtils.hasText(text)) {
            return browserFallback("");
        }

        try {
            TtsSynthesizeResponseDto response = googleTtsService.synthesize(
                new TtsSynthesizeRequestDto(text, null, null, null, null, null)
            );

            if (StringUtils.hasText(response.audioContentBase64())) {
                return TtsResponseDto.builder()
                    .mode(response.mode())
                    .text(text)
                    .audioUrl("data:%s;base64,%s".formatted(response.contentType(), response.audioContentBase64()))
                    .voice(response.voiceName())
                    .audioEncoding(response.audioEncoding())
                    .build();
            }

            return TtsResponseDto.builder()
                .mode(StringUtils.hasText(response.mode()) ? response.mode() : "BROWSER_TTS")
                .text(StringUtils.hasText(response.text()) ? response.text() : text)
                .audioUrl(response.audioUrl())
                .voice(response.voiceName())
                .audioEncoding(response.audioEncoding())
                .build();
        } catch (Exception e) {
            // GoogleTtsService는 disabled/api-key-missing은 예외 없이 직접 browserTts를 반환하므로,
            // 여기로 들어오는 예외는 실제 외부 호출 실패(http-error) 또는 응답에 오디오가 없는 경우(empty-audio)뿐이다.
            String reason = isEmptyAudioFailure(e) ? "empty-audio" : "http-error";
            log.warn("[TTS] fallback reason={} message={}", reason, e.getMessage());
            return browserFallback(text);
        }
    }

    private boolean isEmptyAudioFailure(Exception e) {
        String message = e.getMessage();
        return message != null && message.contains("audioContent가 비어");
    }

    private TtsResponseDto browserFallback(String text) {
        return TtsResponseDto.builder()
            .mode("BROWSER_TTS")
            .text(text)
            .audioUrl(null)
            .build();
    }
}
