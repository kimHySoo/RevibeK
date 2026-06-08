package com.ssafy.revibek.ai.dto.external;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GoogleTtsSynthesizeRequestDto {

    private Input input;
    private Voice voice;
    private AudioConfig audioConfig;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Input {
        private String text;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Voice {
        private String languageCode;
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AudioConfig {
        private String audioEncoding;
        private Double speakingRate;
        private Double pitch;
    }
}
