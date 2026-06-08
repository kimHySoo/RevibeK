package com.ssafy.revibek.radio.service;

import org.springframework.stereotype.Service;

import com.ssafy.revibek.radio.mapper.RadioMapper;
import com.ssafy.revibek.radio.dto.RadioRequestDto;
import com.ssafy.revibek.radio.dto.RadioResponseDto;
import java.util.List;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RadioService {

	private final RadioMapper radioMapper;
	
	//라디오 세션 생성
	public void createSession(RadioRequestDto dto) {
		// TODO: Claude API 호출 → DJ 멘트, 위로 메시지, 소설 구절 생성
        // TODO: 곡 추천 로직
        // TODO: radioMapper.insertSession()
        // TODO: radioMapper.insertRecommendation()
		
		//Claude API 연동 후 채우기
	}
	
	
	//세션 단건 조회
	public RadioResponseDto getSession(String id) {
		// TODO: radioMapper.selectSessionById()
        // TODO: radioMapper.selectRecommendationsBySessionId()
		RadioResponseDto session = radioMapper.selectRadioSessionById(id);
		if(session == null) {
			throw new RuntimeException("존재하지 않는 세션입니다.");
		}
		List<RadioResponseDto.RadioSongDto> songs = 
				radioMapper.selectRecommendationBySessionId(id);
		session.setSongs(songs);
		return session; 
	}
	
	//유저 세션 목록 조회
	public List<RadioResponseDto> getSessionByUser(String userId){
		// TODO: radioMapper.selectSessionsByUserId()
		return radioMapper.selectRadioSessionByUserId(userId);
	}
}
