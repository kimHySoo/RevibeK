package com.ssafy.revibek.playlist.dto;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistDto {

    private String id;
    private String userId;

    @NotBlank(message = "name은 필수입니다.")
    private String name;

    private String moodTag;
    private Boolean isPublic;
    private LocalDateTime createdAt;
    private List<PlaylistItemDto> items;
}
