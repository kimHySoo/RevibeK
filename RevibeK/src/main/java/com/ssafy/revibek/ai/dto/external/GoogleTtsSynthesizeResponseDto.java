package com.ssafy.revibek.ai.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleTtsSynthesizeResponseDto {
    private String audioContent;
}
