package com.ssafy.revibek.user.dto;

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
	private String nickname;
	private String email;
	private String password;
	
	

}
