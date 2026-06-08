package com.ssafy.revibek.usersong.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.revibek.usersong.dto.UserSongRequestDto;
import com.ssafy.revibek.usersong.dto.UserSongResponseDto;
import com.ssafy.revibek.usersong.service.UserSongService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/usersongs")
@RequiredArgsConstructor
public class UserSongController {
	
	private final UserSongService userSongService;

	//노래저장
	@PostMapping
	public ResponseEntity<String> saveSong(@RequestBody UserSongRequestDto dto){
		userSongService.saveSong(dto);
		return ResponseEntity.ok("저장 완료");
	}
	
	// 저장 목록 조회
	@GetMapping("/{userId}")
	public ResponseEntity<List<UserSongResponseDto>> getSavedSongs(@PathVariable String userId){
		return ResponseEntity.ok(userSongService.getSavedSongs(userId));
	}
	
	//별점 등록
	@PutMapping("/rating")
	public ResponseEntity<String> updateRating(@RequestBody UserSongRequestDto dto){
		userSongService.updateRating(dto);
		return ResponseEntity.ok("별점 등록 완료");
	
	}
	
	//재생 카운트 증가
	@PutMapping("/play/{userId}/{songId}")
	public ResponseEntity<String> increasePlayCount(@PathVariable String userId,
													@PathVariable String songId){
		userSongService.increasePlayCount(userId, songId);
		return ResponseEntity.ok("재상 카운트 증가");
	}
	
	 // 저장 취소
    @DeleteMapping("/{userId}/{songId}")
    public ResponseEntity<String> deleteSavedSong(@PathVariable String userId,
                                                   @PathVariable String songId) {
        userSongService.deleteSavedSong(userId, songId);
        return ResponseEntity.ok("저장 취소 완료");
	}
	
	
}
