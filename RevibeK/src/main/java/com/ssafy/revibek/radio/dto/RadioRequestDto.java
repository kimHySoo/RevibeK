package com.ssafy.revibek.radio.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor

public class RadioRequestDto {
	
	private String userId;
	private String mood; // 외로운, 설레는, 그리운, 지친, 행복한 , 슬픈
	private String story;
	
	

}
