package com.ssafy.revibek.radio.service;

import org.springframework.stereotype.Service;

import com.ssafy.revibek.radio.mapper.RadioMapper;
import com.ssafy.revibek.radio.dto.RadioRequestDto;
import com.ssafy.revibek.radio.dto.RadioResponseDto;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RadioService {

	private final RadioMapper radioMapper;
	
	//라디오 세션 생성
	public String createSession(String userId, RadioRequestDto dto) {
		// TODO: Claude API 호출 → DJ 멘트, 위로 메시지, 소설 구절 생성
        // TODO: 곡 추천 로직
		String sessionId = UUID.randomUUID().toString();
		radioMapper.insertRadioSession(sessionId, userId, dto.getMood(), dto.getStory());
        // TODO: radioMapper.insertRecommendation()
		
		//Claude API 연동 후 추천곡 저장 로직 채우기
		return sessionId;
	}
	
	
	//세션 단건 조회
	public RadioResponseDto getSession(String id, String userId) {
		// TODO: radioMapper.selectRadioSessionByIdAndUserId()
        // TODO: radioMapper.selectRecommendationBySessionId()
		RadioResponseDto session = radioMapper.selectRadioSessionByIdAndUserId(id, userId);
		if(session == null) {
			throw new RuntimeException("존재하지 않는 세션이거나 접근 권한이 없습니다.");
		}
		List<RadioResponseDto.RadioSongDto> songs = 
				radioMapper.selectRecommendationBySessionId(id);
		session.setSongs(songs);
		return session; 
	}
	
	//유저 세션 목록 조회
	public List<RadioResponseDto> getSessionByUser(String userId){
		List<RadioResponseDto> sessions = radioMapper.selectRadioSessionByUserId(userId);
		for (RadioResponseDto session : sessions) {
			List<RadioResponseDto.RadioSongDto> songs =
					radioMapper.selectRecommendationBySessionId(session.getId());
			session.setSongs(songs);
		}
		return sessions;
	}
}
