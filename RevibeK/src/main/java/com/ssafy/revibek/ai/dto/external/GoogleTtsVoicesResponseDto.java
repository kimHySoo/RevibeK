package com.ssafy.revibek.ai.dto.external;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleTtsVoicesResponseDto {

    private List<Voice> voices;

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Voice {
        private String name;
        private List<String> languageCodes;
        private String ssmlGender;
        private Integer naturalSampleRateHertz;
    }
}
