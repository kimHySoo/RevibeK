package com.ssafy.revibek.user.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.ssafy.revibek.user.dto.UserRegisterRequestDto;
import com.ssafy.revibek.user.dto.UserResponseDto;

@Mapper
public interface UserMapper {
		
	void insertUser(UserRegisterRequestDto dto);
	UserResponseDto selectUserById(String id);
	UserResponseDto selectUserByEmail(String email);
	String selectPasswordHashByEmail(String email);
	void updateUser(UserResponseDto dto);
	void deleteUser(String id);
	

}
