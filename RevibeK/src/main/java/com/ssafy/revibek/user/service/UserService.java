package com.ssafy.revibek.user.service;

import org.springframework.stereotype.Service;

import com.ssafy.revibek.user.mapper.UserMapper;

import lombok.RequiredArgsConstructor;
import com.ssafy.revibek.user.dto.UserLoginRequestDto;
import com.ssafy.revibek.user.dto.UserRegisterRequestDto;
import com.ssafy.revibek.user.dto.UserResponseDto;
@Service
@RequiredArgsConstructor
public class UserService {
	
	private final UserMapper userMapper;
	
	//회원가입
	public void signUp(UserRegisterRequestDto dto) {
		// TODO: 이메일 중복 체크
		UserResponseDto existing = userMapper.selectUserByEmail(dto.getEmail());
		if(existing != null) {
			throw new RuntimeException("이미 사용중인 이메일입니다.");
		}
        // TODO: 비밀번호 BCrypt 암호화
        // TODO: userMapper.insertUser()
		userMapper.insertUser(dto);
	}

	
	// 로그인 
	public UserResponseDto login(UserLoginRequestDto dto) {
		// TODO: 이메일로 유저 조회
		
        // TODO: 비밀번호 검증
        // TODO: JWT 토큰 생성 후 반환
		
		
		return null;
	}
	
	//유저 조회
	public UserResponseDto getUserById(String id) {
		// TODO :  userMapper.selectUserById()
		UserResponseDto user = userMapper.selectUserByEmail(id);
		if(user == null) {
			throw new RuntimeException("존재하지 않는 유저입니다");
		}
		
		return user;
		
		
	}
	
	// 회원 정보 수정
	public void updateUser(UserResponseDto dto) {
		// TODO : userMapper.updateUser()
		UserResponseDto user = userMapper.selectUserByEmail(dto.getId());
		if(user == null) {
			throw new RuntimeException("존재하지 않는 유저입니다");
			
		}
		userMapper.updateUser(dto);
		
	}
	
	// 회원 탈퇴
	public void deleteUser(String id) {
		// TODO: userMapper.deleteUser()
		UserResponseDto user = userMapper.selectUserById(id);
		if(user == null) {
			throw new RuntimeException("존재하지 않는 유저입니다");
		}
		userMapper.deleteUser(id);
		
		
	}
}
