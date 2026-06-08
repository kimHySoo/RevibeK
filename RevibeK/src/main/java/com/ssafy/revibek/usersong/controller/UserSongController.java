package com.ssafy.revibek.usersong.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/usersongs")
@RequiredArgsConstructor
public class UserSongController {
	
	private final UserSongService userSongService;

	//노래저장
	@PostMapping
	public ResponseEntity<String> saveSong(Authentication authentication,
										   @Valid @RequestBody UserSongRequestDto dto){
		userSongService.saveSong(authentication.getName(), dto);
		return ResponseEntity.ok("저장 완료");
	}
	
	// 저장 목록 조회
	@GetMapping("/me")
	public ResponseEntity<List<UserSongResponseDto>> getSavedSongs(Authentication authentication){
		return ResponseEntity.ok(userSongService.getSavedSongs(authentication.getName()));
	}
	
	//별점 등록
	@PutMapping("/rating")
	public ResponseEntity<String> updateRating(Authentication authentication,
											   @Valid @RequestBody UserSongRequestDto dto){
		userSongService.updateRating(authentication.getName(), dto);
		return ResponseEntity.ok("별점 등록 완료");
	
	}
	
	//재생 카운트 증가
	@PutMapping("/play/{songId}")
	public ResponseEntity<String> increasePlayCount(Authentication authentication,
													@PathVariable String songId){
		userSongService.increasePlayCount(authentication.getName(), songId);
		return ResponseEntity.ok("재생 카운트 증가");
	}
	
	 // 저장 취소
    @DeleteMapping("/{songId}")
    public ResponseEntity<String> deleteSavedSong(Authentication authentication,
                                                   @PathVariable String songId) {
        userSongService.deleteSavedSong(authentication.getName(), songId);
        return ResponseEntity.ok("저장 취소 완료");
	}
	
	
}
