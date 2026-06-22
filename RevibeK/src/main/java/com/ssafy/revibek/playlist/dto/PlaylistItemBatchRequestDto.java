package com.ssafy.revibek.playlist.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistItemBatchRequestDto {

    @NotEmpty(message = "songIds는 1개 이상이어야 합니다.")
    private List<String> songIds;
}
