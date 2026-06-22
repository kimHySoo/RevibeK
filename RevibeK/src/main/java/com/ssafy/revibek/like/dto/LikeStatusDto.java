package com.ssafy.revibek.like.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikeStatusDto {

    private String songId;
    private boolean liked;
    private int likeCount;
}
