package com.ssafy.revibek.radio.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RadioResponseDto {
	
	private String id;
	private String playlistId;
	private String mood;
	private String story;
	private String generation;
	private String genre;
	private String situation;
	private String desiredMood;
	private String videoType;
	private String preferredArtist;
	private String excludedKeywords;
	private String recommendationSource;
	private String djMent;
	private Boolean isPublic;
	private java.time.LocalDateTime publishedAt;
	private String comfortText;
	// [FIX] DB 컬럼 novel_excerpt 와 camelCase 매핑이 맞도록 필드명 수정.
	private String novelExcerpt;
	private LocalDateTime createdAt;
	private List<RadioSongDto> songs;
	// MyBatis resultMap(association)이 tts_mode/tts_audio_url 컬럼을 채워준다.
	// tts_mode가 NULL이면(과거 세션 또는 TTS 미생성) null로 남고, RadioService에서 djMent 기반 브라우저 fallback으로 보강한다.
	private TtsFallbackResponseDto tts;
	
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
