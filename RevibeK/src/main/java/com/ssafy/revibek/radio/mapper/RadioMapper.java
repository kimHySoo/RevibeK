package com.ssafy.revibek.radio.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.revibek.radio.dto.PublicRadioFeedDto;
import com.ssafy.revibek.radio.dto.PublicRadioSongDto;
import com.ssafy.revibek.radio.dto.RadioPublicToggleResponseDto;
import com.ssafy.revibek.radio.dto.RadioResponseDto;

@Mapper
public interface RadioMapper {

	int insertRadioSession(@Param("id") String id,
	                       @Param("userId") String userId,
	                       @Param("mood") String mood,
	                       @Param("story") String story);
	int insertRadioSessionWithMent(@Param("id") String id,
	                               @Param("userId") String userId,
	                               @Param("mood") String mood,
	                               @Param("story") String story,
	                               @Param("generation") String generation,
	                               @Param("genre") String genre,
	                               @Param("situation") String situation,
	                               @Param("desiredMood") String desiredMood,
	                               @Param("videoType") String videoType,
	                               @Param("preferredArtist") String preferredArtist,
	                               @Param("excludedKeywords") String excludedKeywords,
	                               @Param("recommendationSource") String recommendationSource,
	                               @Param("djMent") String djMent);
	int updateRadioSessionDjMent(@Param("id") String id,
	                             @Param("userId") String userId,
	                             @Param("djMent") String djMent);
	void updateRadioSessionPlaylistId(@Param("id") String id,
	                                  @Param("userId") String userId,
	                                  @Param("playlistId") String playlistId);
	void updateRadioSessionTts(@Param("id") String id,
	                           @Param("userId") String userId,
	                           @Param("ttsMode") String ttsMode,
	                           @Param("ttsAudioUrl") String ttsAudioUrl,
	                           @Param("ttsVoice") String ttsVoice,
	                           @Param("ttsAudioEncoding") String ttsAudioEncoding);
	RadioResponseDto selectRadioSessionByIdAndUserId(@Param("id") String id,
	                                                 @Param("userId") String userId);
	List<RadioResponseDto> selectRadioSessionByUserId(@Param("userId") String userId);
	void insertRecommendation(@Param("sessionId") String sessionId,
	                          @Param("songId")	String songId,
	                          @Param("orderNum") int orderNum,
	                          @Param("reason") String reason);
	List<RadioResponseDto.RadioSongDto> selectRecommendationBySessionId(@Param("sessionId") String sessionId);

	int updateRadioSessionPublic(@Param("id") String id,
	                             @Param("userId") String userId,
	                             @Param("isPublic") boolean isPublic);

	RadioPublicToggleResponseDto selectRadioPublicStatus(@Param("id") String id,
	                                                     @Param("userId") String userId);

	List<PublicRadioFeedDto> selectPublicRadioSessions(@Param("sort") String sort);

	PublicRadioFeedDto selectPublicRadioSessionById(@Param("id") String id);

	List<PublicRadioSongDto> selectPublicRecommendedSongs(@Param("sessionId") String sessionId);





}
