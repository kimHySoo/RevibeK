package com.ssafy.revibek.radio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RadioLikeResponseDto {
    private String radioSessionId;
    private boolean liked;
}
