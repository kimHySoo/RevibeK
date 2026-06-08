package com.ssafy.revibek.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor

/*
 * @Data : getter, setter, toString, equals, hashcode 전부 포함
 * @NoArgsConstructor : 기본 생성자
 * @AllArgsConstructor : 전체 필드 생성자
 */
public class UserRegisterRequestDto {
	@NotBlank(message = "nickname은 필수입니다.")
	private String nickname;
	@NotBlank(message = "email은 필수입니다.")
	@Email(message = "올바른 email 형식이 아닙니다.")
	private String email;
	@NotBlank(message = "password는 필수입니다.")
	@Size(min = 8, message = "password는 8자 이상이어야 합니다.")
	private String password;
	
	

}
