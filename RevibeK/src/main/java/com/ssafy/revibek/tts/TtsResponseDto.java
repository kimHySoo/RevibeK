package com.ssafy.revibek.tts;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TtsResponseDto {

    private String mode;
    private String text;
    private String audioUrl;
}
