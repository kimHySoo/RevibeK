package com.ssafy.revibek.usersong.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ssafy.revibek.usersong.dto.UserSongRequestDto;
import com.ssafy.revibek.usersong.dto.UserSongResponseDto;
import com.ssafy.revibek.usersong.mapper.UserSongMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserSongService {

	
	private final UserSongMapper userSongMapper;

	//노래 저장
	public void saveSong(UserSongRequestDto dto) {
		// TODO: userSongMapper.insertUserSong()
		userSongMapper.insertUserSong(dto);
	}
	
	//저장 목록 조회
	public List<UserSongResponseDto> getSavedSongs(String userId){
		// TODO: userSongMapper.selectSavedSongs()
		return userSongMapper.selectSavedSongs(userId);
	}
	// 별점 등록
	public void updateRating(UserSongRequestDto dto) {
		// TODO: userSongMapper.updateRating()
		if(dto.getRating()<1 || dto.getRating() >5) {
			throw new RuntimeException("별점은 1 ~5사이여야 합니다");
		}
		userSongMapper.updateRating(dto);
	}
	
	//재생 카운트 증가 
	public void increasePlayCount(String userId, String songId) {
		// TODO: userSongMapper.increasePlayCount()
		userSongMapper.increasePlayCount(userId, songId);
	}
	
	// 저장 취소
	public void deleteSavedSong(String userId, String songId) {
		// TODO: userSongMapper.deleteSavedSong()
		userSongMapper.deleteSavedSong(userId, songId);
		
	}
	
}
