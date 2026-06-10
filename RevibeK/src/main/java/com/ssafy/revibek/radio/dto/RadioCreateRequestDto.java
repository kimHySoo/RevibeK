package com.ssafy.revibek.radio.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RadioCreateRequestDto {

    @NotBlank(message = "mood는 필수입니다.")
    private String mood;

    @NotBlank(message = "story는 필수입니다.")
    private String story;

    @NotBlank(message = "era는 필수입니다.")
    private String era;

    @NotBlank(message = "genre는 필수입니다.")
    private String genre;
}
