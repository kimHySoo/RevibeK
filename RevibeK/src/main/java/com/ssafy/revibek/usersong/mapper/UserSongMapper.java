package com.ssafy.revibek.usersong.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.revibek.usersong.dto.UserSongRequestDto;
import com.ssafy.revibek.usersong.dto.UserSongResponseDto;

@Mapper
public interface UserSongMapper {
	
	void insertUserSong(UserSongRequestDto dto);
	List<UserSongResponseDto> selectSavedSongs(String userId);
	void updateRating(UserSongRequestDto dto);
	
	void increasePlayCount(@Param("userId") String userId,
							@Param("songId") String songId);
	
	void deleteSavedSong(@Param("userId") String userId,
						@Param("songId") String songId);
}
