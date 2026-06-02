package com.ssafy.revibek.radio.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.revibek.radio.dto.RadioResponseDto;

@Mapper
public interface RadioMapper {
	
	int insertRadioSession(@Param("id") String id,
						   @Param("userId") String userId,
						   @Param("mood") String mood,
						   @Param("story") String story);
	RadioResponseDto selectRadioSessionByIdAndUserId(@Param("id") String id,
													 @Param("userId") String userId);
	List<RadioResponseDto> selectRadioSessionByUserId(@Param("userId") String userId);
	void insertRecommendation(@Param("sessionId") String sessionId,
							  @Param("songId")	String songId,
							  @Param("orderNum") int orderNum,
							  @Param("reason") String reason);
	List<RadioResponseDto.RadioSongDto> selectRecommendationBySessionId(@Param("sessionId") String sessionId);	
			
			
			
	
	
	

}
