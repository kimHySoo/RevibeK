package com.ssafy.revibek.explore.dto;

import com.ssafy.revibek.song.dto.SongDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ExploreResponseDto {
    private SongDto song;
    private List<SongDto> similar;
    private boolean isNew;
}
