package com.ssafy.revibek.playlist.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistItemBatchResponseDto {

    private List<PlaylistItemDto> added;
    private List<SkippedItem> skipped;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkippedItem {
        private String songId;
        private String reason;
    }
}
