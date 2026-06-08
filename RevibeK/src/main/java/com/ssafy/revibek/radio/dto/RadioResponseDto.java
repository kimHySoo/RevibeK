package com.ssafy.revibek.radio.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RadioResponseDto {
	
	private String id;
	private String mood;
	private String story;
	private String djMent;
	private String comfortText;
	private String novelExcept;
	private LocalDateTime createdAt;
	private List<RadioSongDto> songs;
	
	@Data
	@NoArgsConstructor
	public static class RadioSongDto{
		private String songId;
		private String title;
		private String artist;
		private int orderNum;
		private String reason;
	}
	
	

}
