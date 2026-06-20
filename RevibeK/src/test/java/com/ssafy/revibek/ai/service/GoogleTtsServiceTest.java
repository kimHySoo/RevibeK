package com.ssafy.revibek.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.ssafy.revibek.ai.dao.GoogleTtsDao;
import com.ssafy.revibek.ai.dto.TtsSynthesizeRequestDto;
import com.ssafy.revibek.ai.dto.TtsSynthesizeResponseDto;
import com.ssafy.revibek.ai.dto.external.GoogleTtsSynthesizeRequestDto;
import com.ssafy.revibek.ai.dto.external.GoogleTtsSynthesizeResponseDto;

class GoogleTtsServiceTest {

    private GoogleTtsDao googleTtsDao;
    private GoogleTtsService googleTtsService;

    @BeforeEach
    void setUp() {
        googleTtsDao = mock(GoogleTtsDao.class);
        googleTtsService = new GoogleTtsService(googleTtsDao);
        ReflectionTestUtils.setField(googleTtsService, "apiKey", "dummy-key");
        ReflectionTestUtils.setField(googleTtsService, "defaultLanguageCode", "ko-KR");
        ReflectionTestUtils.setField(googleTtsService, "defaultVoiceName", "ko-KR-Chirp3-HD-Vindemiatrix");
        ReflectionTestUtils.setField(googleTtsService, "defaultAudioEncoding", "MP3");
        ReflectionTestUtils.setField(googleTtsService, "defaultSpeakingRate", 1.0);
        ReflectionTestUtils.setField(googleTtsService, "defaultPitch", "");
    }

    private TtsSynthesizeRequestDto request() {
        return new TtsSynthesizeRequestDto("디제이 멘트", null, null, null, null, null);
    }

    @Test
    void TTS가_비활성화면_외부API를_호출하지않고_브라우저모드를_반환한다() {
        ReflectionTestUtils.setField(googleTtsService, "enabled", false);

        TtsSynthesizeResponseDto result = googleTtsService.synthesize(request());

        assertThat(result.mode()).isEqualTo("BROWSER_TTS");
        verify(googleTtsDao, never()).synthesize(any());
    }

    @Test
    void apiKey가없으면_외부API를_호출하지않고_브라우저모드를_반환한다() {
        ReflectionTestUtils.setField(googleTtsService, "enabled", true);
        ReflectionTestUtils.setField(googleTtsService, "apiKey", "");

        TtsSynthesizeResponseDto result = googleTtsService.synthesize(request());

        assertThat(result.mode()).isEqualTo("BROWSER_TTS");
        verify(googleTtsDao, never()).synthesize(any());
    }

    @Test
    void 정상호출이면_구글TTS_오디오결과를_반환한다() {
        ReflectionTestUtils.setField(googleTtsService, "enabled", true);
        GoogleTtsSynthesizeResponseDto daoResponse = new GoogleTtsSynthesizeResponseDto();
        daoResponse.setAudioContent("base64audio==");
        when(googleTtsDao.synthesize(any(GoogleTtsSynthesizeRequestDto.class))).thenReturn(daoResponse);

        TtsSynthesizeResponseDto result = googleTtsService.synthesize(request());

        assertThat(result.mode()).isEqualTo("GOOGLE_TTS");
        assertThat(result.audioContentBase64()).isEqualTo("base64audio==");
        assertThat(result.contentType()).isEqualTo("audio/mpeg");
        assertThat(result.voiceName()).isEqualTo("ko-KR-Chirp3-HD-Vindemiatrix");
        assertThat(result.audioEncoding()).isEqualTo("MP3");
    }

    @Test
    void 구글응답에_오디오가없으면_예외를_던진다() {
        ReflectionTestUtils.setField(googleTtsService, "enabled", true);
        when(googleTtsDao.synthesize(any(GoogleTtsSynthesizeRequestDto.class)))
                .thenReturn(new GoogleTtsSynthesizeResponseDto());

        assertThatThrownBy(() -> googleTtsService.synthesize(request()))
                .isInstanceOf(RuntimeException.class);
    }
}
