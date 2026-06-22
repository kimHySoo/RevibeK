package com.ssafy.revibek.usersong.dto;

import java.time.LocalDateTime;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor

public class UserSongResponseDto {

	private String id;
	private String userId;
	private String songId;
	private String title;
	private String artist;
	private int isSaved;
	private Integer rating;
	private Integer playCount;
	private LocalDateTime lastPlayedAt;
	

	
	
	
	
	
	
}
