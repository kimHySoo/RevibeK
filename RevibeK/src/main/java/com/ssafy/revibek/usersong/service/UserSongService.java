package com.ssafy.revibek.usersong.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.revibek.usersong.dto.UserSongRequestDto;
import com.ssafy.revibek.usersong.dto.UserSongResponseDto;
import com.ssafy.revibek.usersong.mapper.UserSongMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserSongService {

	
	private final UserSongMapper userSongMapper;

	//노래 저장
	@Transactional
	public void saveSong(String userId, UserSongRequestDto dto) {
		int existingCount = userSongMapper.countUserSong(userId, dto.getSongId());
		if (existingCount > 0) {
			userSongMapper.restoreSavedSong(userId, dto.getSongId());
			return;
		}
		userSongMapper.insertUserSong(userId, dto.getSongId());
	}
	
	//저장 목록 조회
	@Transactional(readOnly = true)
	public List<UserSongResponseDto> getSavedSongs(String userId){
		return userSongMapper.selectSavedSongs(userId);
	}
	// 별점 등록
	@Transactional
	public void updateRating(String userId, UserSongRequestDto dto) {
		if (dto.getRating() == null) {
			throw new RuntimeException("별점은 필수입니다.");
		}
		int updatedRows = userSongMapper.updateRating(userId, dto.getSongId(), dto.getRating());
		if (updatedRows == 0) {
			throw new RuntimeException("저장된 곡이 없어 별점을 등록할 수 없습니다.");
		}
	}
	
	//재생 카운트 증가 
	@Transactional
	public void increasePlayCount(String userId, String songId) {
		int updatedRows = userSongMapper.increasePlayCount(userId, songId);
		if (updatedRows == 0) {
			throw new RuntimeException("저장된 곡이 없어 재생 카운트를 증가할 수 없습니다.");
		}
	}
	
	// 저장 취소
	@Transactional
	public void deleteSavedSong(String userId, String songId) {
		int updatedRows = userSongMapper.deleteSavedSong(userId, songId);
		if (updatedRows == 0) {
			throw new RuntimeException("저장된 곡이 없어 취소할 수 없습니다.");
		}
	}
	
}
