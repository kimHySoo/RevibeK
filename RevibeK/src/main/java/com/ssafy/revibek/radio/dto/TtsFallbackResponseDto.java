package com.ssafy.revibek.radio.dto;

import com.ssafy.revibek.tts.TtsResponseDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TtsFallbackResponseDto {

    private String mode;
    private String text;
    private String audioUrl;

    public static TtsFallbackResponseDto from(TtsResponseDto response) {
        return TtsFallbackResponseDto.builder()
            .mode(response.getMode())
            .text(response.getText())
            .audioUrl(response.getAudioUrl())
            .build();
    }
}
