package com.ssafy.revibek.usersong.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.revibek.usersong.dto.UserSongResponseDto;

@Mapper
public interface UserSongMapper {
	
	int countUserSong(@Param("userId") String userId,
					  @Param("songId") String songId);
	int insertUserSong(@Param("userId") String userId,
					   @Param("songId") String songId);
	int restoreSavedSong(@Param("userId") String userId,
						 @Param("songId") String songId);
	List<UserSongResponseDto> selectSavedSongs(@Param("userId") String userId);
	int updateRating(@Param("userId") String userId,
					 @Param("songId") String songId,
					 @Param("rating") int rating);
	
	int increasePlayCount(@Param("userId") String userId,
							@Param("songId") String songId);
	
	int deleteSavedSong(@Param("userId") String userId,
						@Param("songId") String songId);
}
