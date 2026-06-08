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
	// [FIX] DB 컬럼 novel_excerpt 와 camelCase 매핑이 맞도록 필드명 수정.
	private String novelExcerpt;
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
