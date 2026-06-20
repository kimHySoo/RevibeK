package com.ssafy.revibek.radio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ssafy.revibek.analysis.service.AnalysisService;
import com.ssafy.revibek.follow.mapper.FollowMapper;
import com.ssafy.revibek.playlist.service.PlaylistService;
import com.ssafy.revibek.preference.service.PreferenceService;
import com.ssafy.revibek.qdrant.QdrantService;
import com.ssafy.revibek.radio.ai.AiDjMentService;
import com.ssafy.revibek.radio.dto.RadioCreateRequestDto;
import com.ssafy.revibek.radio.dto.RadioCreateResponseDto;
import com.ssafy.revibek.radio.dto.RadioResponseDto;
import com.ssafy.revibek.radio.dto.TtsFallbackResponseDto;
import com.ssafy.revibek.radio.mapper.RadioLikeMapper;
import com.ssafy.revibek.radio.mapper.RadioMapper;
import com.ssafy.revibek.song.mapper.SongDao;
import com.ssafy.revibek.song.service.SongService;
import com.ssafy.revibek.tts.TtsResponseDto;
import com.ssafy.revibek.tts.TtsService;

@ExtendWith(MockitoExtension.class)
class RadioServiceTest {

    @Mock private RadioMapper radioMapper;
    @Mock private RadioLikeMapper radioLikeMapper;
    @Mock private FollowMapper followMapper;
    @Mock private SongDao songDao;
    @Mock private AiDjMentService aiDjMentService;
    @Mock private TtsService ttsService;
    @Mock private PreferenceService preferenceService;
    @Mock private QdrantService qdrantService;
    @Mock private PlaylistService playlistService;
    @Mock private SongService songService;
    @Mock private AnalysisService analysisService;

    @InjectMocks
    private RadioService radioService;

    private RadioCreateRequestDto request() {
        RadioCreateRequestDto request = new RadioCreateRequestDto();
        request.setMood("그리운");
        request.setEra("2세대");
        request.setGenre("댄스");
        return request;
    }

    @Test
    void 라디오생성시_DJ멘트와TTS결과가_응답에포함되고_세션에저장된다() {
        when(aiDjMentService.createDjMent(any(RadioCreateRequestDto.class), anyList()))
                .thenReturn("디제이 멘트");
        TtsResponseDto tts = TtsResponseDto.builder()
                .mode("GOOGLE_TTS")
                .text("디제이 멘트")
                .audioUrl("data:audio/mpeg;base64,xxx")
                .voice("ko-KR-Chirp3-HD-Vindemiatrix")
                .audioEncoding("MP3")
                .build();
        when(ttsService.synthesize("디제이 멘트")).thenReturn(tts);

        RadioCreateResponseDto response = radioService.createRadio("user1", request());

        assertThat(response.getDjMent()).isEqualTo("디제이 멘트");
        assertThat(response.getTts().getMode()).isEqualTo("GOOGLE_TTS");
        assertThat(response.getTts().getAudioUrl()).isEqualTo("data:audio/mpeg;base64,xxx");

        ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
        verify(radioMapper).updateRadioSessionTts(
                idCaptor.capture(), eq("user1"), eq("GOOGLE_TTS"),
                eq("data:audio/mpeg;base64,xxx"), eq("ko-KR-Chirp3-HD-Vindemiatrix"), eq("MP3"));
        assertThat(idCaptor.getValue()).isEqualTo(response.getRadioSessionId());
    }

    @Test
    void TTS결과저장이실패해도_라디오생성자체는_성공한다() {
        when(aiDjMentService.createDjMent(any(RadioCreateRequestDto.class), anyList()))
                .thenReturn("디제이 멘트");
        when(ttsService.synthesize("디제이 멘트")).thenReturn(
                TtsResponseDto.builder().mode("BROWSER_TTS").text("디제이 멘트").audioUrl(null).build());
        org.mockito.Mockito.doThrow(new RuntimeException("DB connection lost"))
                .when(radioMapper).updateRadioSessionTts(anyString(), anyString(), any(), any(), any(), any());

        RadioCreateResponseDto response = radioService.createRadio("user1", request());

        assertThat(response.getDjMent()).isEqualTo("디제이 멘트");
        assertThat(response.getTts().getMode()).isEqualTo("BROWSER_TTS");
    }

    @Test
    void 조회시_저장된구글TTS를_복원하고_다시합성하지않는다() {
        RadioResponseDto stored = new RadioResponseDto();
        stored.setId("session1");
        stored.setDjMent("디제이 멘트");
        stored.setTts(TtsFallbackResponseDto.builder().mode("GOOGLE_TTS").audioUrl("data:audio/mpeg;base64,yyy").build());
        when(radioMapper.selectRadioSessionByIdAndUserId("session1", "user1")).thenReturn(stored);
        when(radioMapper.selectRecommendationBySessionId("session1")).thenReturn(List.of());

        RadioResponseDto result = radioService.getSession("session1", "user1");

        assertThat(result.getTts().getMode()).isEqualTo("GOOGLE_TTS");
        assertThat(result.getTts().getAudioUrl()).isEqualTo("data:audio/mpeg;base64,yyy");
        assertThat(result.getTts().getText()).isEqualTo("디제이 멘트");
        verify(ttsService, never()).synthesize(anyString());
    }

    @Test
    void 조회시_TTS컬럼이없는과거세션은_djMent기반_브라우저fallback을_반환한다() {
        RadioResponseDto stored = new RadioResponseDto();
        stored.setId("session2");
        stored.setDjMent("옛날 멘트");
        stored.setTts(null);
        when(radioMapper.selectRadioSessionByIdAndUserId("session2", "user1")).thenReturn(stored);
        when(radioMapper.selectRecommendationBySessionId("session2")).thenReturn(List.of());

        RadioResponseDto result = radioService.getSession("session2", "user1");

        assertThat(result.getTts().getMode()).isEqualTo("BROWSER_TTS");
        assertThat(result.getTts().getAudioUrl()).isNull();
        assertThat(result.getTts().getText()).isEqualTo("옛날 멘트");
        verify(ttsService, never()).synthesize(anyString());
    }

    @Test
    void 조회시_DJ멘트도없는세션은_tts가null이다() {
        RadioResponseDto stored = new RadioResponseDto();
        stored.setId("session3");
        stored.setDjMent(null);
        stored.setTts(null);
        when(radioMapper.selectRadioSessionByIdAndUserId("session3", "user1")).thenReturn(stored);
        when(radioMapper.selectRecommendationBySessionId("session3")).thenReturn(List.of());

        RadioResponseDto result = radioService.getSession("session3", "user1");

        assertThat(result.getTts()).isNull();
    }
}
