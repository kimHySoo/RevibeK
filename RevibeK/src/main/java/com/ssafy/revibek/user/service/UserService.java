package com.ssafy.revibek.user.service;

import org.springframework.stereotype.Service;

import com.ssafy.revibek.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import com.ssafy.revibek.user.dto.UserResponseDto;
import com.ssafy.revibek.user.dto.UserUpdateRequestDto;

@Service
@RequiredArgsConstructor
public class UserService {
	
	private final UserMapper userMapper;

	public UserResponseDto getMyInfo(String userId) {
		UserResponseDto user = userMapper.selectUserById(userId);
		if (user == null) {
			throw new RuntimeException("존재하지 않는 유저입니다.");
		}
		return user;
	}

	public void updateMyInfo(String userId, UserUpdateRequestDto dto) {
		UserResponseDto user = userMapper.selectUserById(userId);
		if (user == null) {
			throw new RuntimeException("존재하지 않는 유저입니다.");
		}
		userMapper.updateUser(userId, dto.getNickname(), dto.getEmail());
	}

	public void deleteMyAccount(String userId) {
		UserResponseDto user = userMapper.selectUserById(userId);
		if (user == null) {
			throw new RuntimeException("존재하지 않는 유저입니다.");
		}
		userMapper.deleteUser(userId);
	}
}
