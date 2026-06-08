package com.ssafy.revibek.radio.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.revibek.radio.dto.RadioRequestDto;
import com.ssafy.revibek.radio.dto.RadioResponseDto;

@Mapper
public interface RadioMapper {
	
	void insertRadioSession(RadioRequestDto dto);
	RadioResponseDto selectRadioSessionById(String id);
	List<RadioResponseDto> selectRadioSessionByUserId(String userId);
	void insertRecommendation(@Param("sessionId") String sessionId,
							  @Param("songId")	String songId,
							  @Param("orderNum") int orderNum,
							  @Param("reason") String reason);
	List<RadioResponseDto.RadioSongDto> selectRecommendationBySessionId(String sessionId);	
			
			
			
	
	
	

}
