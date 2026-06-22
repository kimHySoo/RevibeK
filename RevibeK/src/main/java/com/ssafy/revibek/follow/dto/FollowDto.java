package com.ssafy.revibek.follow.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class FollowDto {
    private String userId;
    private String nickname;
    private LocalDateTime followedAt;
}
