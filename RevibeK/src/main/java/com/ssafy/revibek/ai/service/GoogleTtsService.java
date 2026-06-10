package com.ssafy.revibek.ai.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.ssafy.revibek.ai.dao.GoogleTtsDao;
import com.ssafy.revibek.ai.dto.TtsPreset;
import com.ssafy.revibek.ai.dto.TtsSynthesizeRequestDto;
import com.ssafy.revibek.ai.dto.TtsSynthesizeResponseDto;
import com.ssafy.revibek.ai.dto.TtsVoiceResponseDto;
import com.ssafy.revibek.ai.dto.external.GoogleTtsSynthesizeRequestDto;
import com.ssafy.revibek.ai.dto.external.GoogleTtsSynthesizeResponseDto;
import com.ssafy.revibek.ai.dto.external.GoogleTtsVoicesResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GoogleTtsService {

    private final GoogleTtsDao googleTtsDao;

    @Value("${gcp.tts.default-language-code:ko-KR}")
    private String defaultLanguageCode;

    @Value("${gcp.tts.default-voice-name:}")
    private String defaultVoiceName;

    @Value("${gcp.tts.default-audio-encoding:MP3}")
    private String defaultAudioEncoding;

    @Value("${gcp.tts.default-speaking-rate:1.0}")
    private Double defaultSpeakingRate;

    @Value("${gcp.tts.default-pitch:}")
    private String defaultPitch;

    public TtsSynthesizeResponseDto synthesize(TtsSynthesizeRequestDto request) {
        String languageCode = StringUtils.hasText(request.languageCode())
            ? request.languageCode()
            : defaultLanguageCode;
        String voiceName = StringUtils.hasText(request.voiceName())
            ? request.voiceName()
            : defaultVoiceName;
        String audioEncoding = StringUtils.hasText(request.audioEncoding())
            ? request.audioEncoding()
            : defaultAudioEncoding;
        Double speakingRate = request.speakingRate() != null
            ? request.speakingRate()
            : defaultSpeakingRate;
        Double pitch = resolvePitch(request.pitch());
        if (isPitchUnsupportedVoice(voiceName)) {
            // Chirp3-HD voices currently reject pitch parameters.
            pitch = null;
        }

        GoogleTtsSynthesizeRequestDto requestDto = new GoogleTtsSynthesizeRequestDto(
            new GoogleTtsSynthesizeRequestDto.Input(request.text()),
            new GoogleTtsSynthesizeRequestDto.Voice(languageCode, StringUtils.hasText(voiceName) ? voiceName : null),
            new GoogleTtsSynthesizeRequestDto.AudioConfig(audioEncoding, speakingRate, pitch)
        );
        GoogleTtsSynthesizeResponseDto responseDto = googleTtsDao.synthesize(requestDto);
        if (responseDto == null || !StringUtils.hasText(responseDto.getAudioContent())) {
            throw new RuntimeException("Google TTS audioContent가 비어 있습니다.");
        }

        return new TtsSynthesizeResponseDto(
            responseDto.getAudioContent(),
            toContentType(audioEncoding),
            languageCode,
            voiceName,
            audioEncoding
        );
    }

    public TtsSynthesizeResponseDto synthesizeWithPreset(TtsSynthesizeRequestDto request, String presetName) {
        TtsPreset preset = TtsPreset.from(presetName);

        TtsSynthesizeRequestDto merged = new TtsSynthesizeRequestDto(
            request.text(),
            request.languageCode() != null ? request.languageCode() : preset.languageCode,
            request.voiceName() != null ? request.voiceName() : preset.voiceName,
            request.speakingRate() != null ? request.speakingRate() : preset.speakingRate,
            request.pitch() != null ? request.pitch() : preset.pitch,
            request.audioEncoding() != null ? request.audioEncoding() : preset.audioEncoding
        );

        return synthesize(merged);
    }

    public List<TtsVoiceResponseDto> listVoices(String languageCode) {
        GoogleTtsVoicesResponseDto responseDto = googleTtsDao.listVoices(languageCode);
        if (responseDto == null || responseDto.getVoices() == null) {
            return List.of();
        }
        return responseDto.getVoices().stream()
            .map(voice -> new TtsVoiceResponseDto(
                voice.getName(),
                voice.getLanguageCodes(),
                voice.getSsmlGender(),
                voice.getNaturalSampleRateHertz()
            ))
            .toList();
    }

    private String toContentType(String audioEncoding) {
        if ("LINEAR16".equalsIgnoreCase(audioEncoding)) {
            return "audio/wav";
        }
        if ("OGG_OPUS".equalsIgnoreCase(audioEncoding)) {
            return "audio/ogg";
        }
        return "audio/mpeg";
    }

    private Double resolvePitch(Double requestPitch) {
        if (requestPitch != null) {
            return requestPitch;
        }
        if (!StringUtils.hasText(defaultPitch)) {
            return null;
        }
        try {
            return Double.valueOf(defaultPitch);
        } catch (NumberFormatException e) {
            throw new RuntimeException("gcp.tts.default-pitch 설정값이 숫자가 아닙니다: " + defaultPitch, e);
        }
    }

    private boolean isPitchUnsupportedVoice(String voiceName) {
        return StringUtils.hasText(voiceName)
            && voiceName.toLowerCase().contains("chirp3-hd");
    }
}
