package com.ssafy.revibek.radio.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor

public class RadioRequestDto {
	
	@NotBlank(message = "mood는 필수입니다.")
	private String mood; // 외로운, 설레는, 그리운, 지친, 행복한 , 슬픈
	@NotBlank(message = "story는 필수입니다.")
	private String story;
	
	

}
