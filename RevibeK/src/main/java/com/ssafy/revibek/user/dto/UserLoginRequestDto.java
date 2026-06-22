package com.ssafy.revibek.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginRequestDto {
	
	@NotBlank(message = "email은 필수입니다.")
	@Email(message = "올바른 email 형식이 아닙니다.")
	private String email;
	@NotBlank(message = "password는 필수입니다.")
	private String password;
	

	
	

}
