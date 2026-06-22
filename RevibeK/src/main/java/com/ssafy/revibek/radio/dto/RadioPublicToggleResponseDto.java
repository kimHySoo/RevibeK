package com.ssafy.revibek.radio.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RadioPublicToggleResponseDto {
    private String radioSessionId;
    private Boolean isPublic;
    private LocalDateTime publishedAt;
}
