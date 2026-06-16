package com.ssafy.revibek.playlist.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PlaylistUpdateRequestDto {

    @NotBlank(message = "name은 필수입니다.")
    private String name;

    private String moodTag;
    private Boolean isPublic;
}
