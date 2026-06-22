package com.ssafy.revibek.radio.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PublicRadioFeedDto {
    private String radioSessionId;
    private String userId;
    private String userNickname;
    private String title;
    private String story;
    private String mood;
    private String situation;
    private String generation;
    private String djComment;
    private Boolean isPublic;
    private LocalDateTime publishedAt;
    private int likeCount;
    private boolean liked;
    private boolean followed;
    private List<PublicRadioSongDto> recommendedSongs;
}
