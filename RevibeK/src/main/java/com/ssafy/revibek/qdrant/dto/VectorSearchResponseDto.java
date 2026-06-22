package com.ssafy.revibek.qdrant.dto;

import java.util.List;

import com.ssafy.revibek.song.dto.SongDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VectorSearchResponseDto {

    private String source;
    private String message;
    private List<SongDto> results;
}
