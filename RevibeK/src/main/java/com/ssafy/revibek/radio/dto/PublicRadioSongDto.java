package com.ssafy.revibek.radio.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PublicRadioSongDto {
    private String songId;
    private String title;
    private String artist;
    private String generation;
    private String genre;
}
