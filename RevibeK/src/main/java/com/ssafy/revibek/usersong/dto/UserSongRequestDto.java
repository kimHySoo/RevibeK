package com.ssafy.revibek.usersong.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserSongRequestDto {
	@NotBlank(message = "songId는 필수입니다.")
	private String songId;
	@Min(value = 1, message = "rating은 1 이상이어야 합니다.")
	@Max(value = 5, message = "rating은 5 이하여야 합니다.")
	private Integer rating; // 1-5, 저장 시 null 가능
	
	
	
	

}
