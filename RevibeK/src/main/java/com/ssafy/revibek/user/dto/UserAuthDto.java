package com.ssafy.revibek.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAuthDto {

    private String id;
    private String nickname;
    private String email;
    private String provider;
    private String providerId;
    private String passwordHash;
}
