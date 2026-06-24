package com.ssafy.revibek.radio.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ssafy.revibek.radio.dto.RadioCreateRequestDto;
import com.ssafy.revibek.radio.dto.RecommendedSongResponseDto;

@ExtendWith(MockitoExtension.class)
class AiDjMentServiceTest {

    @Mock
    private AiDjPromptBuilder promptBuilder;

    @Mock
    private GmsClient gmsClient;

    @InjectMocks
    private AiDjMentService aiDjMentService;

    private RadioCreateRequestDto request() {
        RadioCreateRequestDto request = new RadioCreateRequestDto();
        request.setMood("그리운");
        request.setGeneration("2세대");
        request.setGenre("댄스");
        return request;
    }

    @Test
    void GMS가_Claude멘트를_반환하면_그대로_사용한다() {
        when(promptBuilder.build(any(RadioCreateRequestDto.class), anyList())).thenReturn("prompt");
        when(gmsClient.generate("prompt")).thenReturn(Optional.of("Claude가 만든 DJ 멘트"));

        String ment = aiDjMentService.createDjMent(request(), List.of());

        assertThat(ment).isEqualTo("Claude가 만든 DJ 멘트");
    }

    @Test
    void GMS가_비어있는결과를_반환하면_fallback_멘트를_사용한다() {
        when(promptBuilder.build(any(RadioCreateRequestDto.class), anyList())).thenReturn("prompt");
        when(gmsClient.generate("prompt")).thenReturn(Optional.empty());

        String ment = aiDjMentService.createDjMent(request(), List.of());

        assertThat(ment).isNotBlank();
        assertThat(ment).contains("그리운");
    }

    @Test
    void GMS가_공백문자열을_반환하면_fallback_멘트를_사용한다() {
        when(promptBuilder.build(any(RadioCreateRequestDto.class), anyList())).thenReturn("prompt");
        when(gmsClient.generate("prompt")).thenReturn(Optional.of("   "));

        String ment = aiDjMentService.createDjMent(request(), List.of());

        assertThat(ment).isNotBlank();
        assertThat(ment).doesNotContain("   ");
    }

    @Test
    void 추천곡이있으면_fallback_멘트에_첫곡정보가_포함된다() {
        when(promptBuilder.build(any(RadioCreateRequestDto.class), anyList())).thenReturn("prompt");
        when(gmsClient.generate("prompt")).thenReturn(Optional.empty());
        RecommendedSongResponseDto song = RecommendedSongResponseDto.builder()
                .artist("소녀시대")
                .title("Gee")
                .build();

        String ment = aiDjMentService.createDjMent(request(), List.of(song));

        assertThat(ment).contains("소녀시대").contains("Gee");
    }
}
