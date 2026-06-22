package com.ssafy.revibek.playlist.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistItemDto {

    private String id;
    private String playlistId;

    @NotBlank(message = "songId는 필수입니다.")
    private String songId;

    private String title;
    private String artist;
    private String genre;
    private String era;
    private String youtubeUrl;
    private String youtubeId;
    private Integer orderNum;
    private LocalDateTime addedAt;
}
