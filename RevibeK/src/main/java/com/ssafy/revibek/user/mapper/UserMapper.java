package com.ssafy.revibek.user.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;

import com.ssafy.revibek.user.dto.UserAuthDto;
import com.ssafy.revibek.user.dto.UserResponseDto;

@Mapper
public interface UserMapper {
		
	void insertLocalUser(@Param("nickname") String nickname,
						 @Param("email") String email,
						 @Param("passwordHash") String passwordHash);
	void insertGoogleUser(@Param("nickname") String nickname,
						  @Param("email") String email,
						  @Param("providerId") String providerId);
	UserResponseDto selectUserById(@Param("id") String id);
	UserAuthDto selectUserAuthByEmail(@Param("email") String email);
	void updateUser(@Param("id") String id,
					@Param("nickname") String nickname,
					@Param("email") String email);
	void deleteUser(@Param("id") String id);
	

}
