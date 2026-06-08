package com.ssafy.revibek.usersong.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserSongRequestDto {
	private String userId;
	private String songId;
	private int rating; //1- 5
	
	
	
	

}
