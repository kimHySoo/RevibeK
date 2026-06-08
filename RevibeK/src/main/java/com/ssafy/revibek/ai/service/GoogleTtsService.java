package com.ssafy.revibek.ai.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.ssafy.revibek.ai.dao.GoogleTtsDao;
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

    @Value("${gcp.tts.default-pitch:0.0}")
    private Double defaultPitch;

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
        Double pitch = request.pitch() != null
            ? request.pitch()
            : defaultPitch;

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
}
