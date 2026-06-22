package com.ssafy.revibek.challenge.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.revibek.challenge.dto.ChallengeParticipantDto;
import com.ssafy.revibek.challenge.dto.ChallengeResponseDto;

@Mapper
public interface ChallengeMapper {

    void insertChallenge(@Param("id") String id,
                          @Param("creatorId") String creatorId,
                          @Param("title") String title,
                          @Param("description") String description,
                          @Param("startDate") String startDate,
                          @Param("endDate") String endDate,
                          @Param("targetCount") int targetCount,
                          @Param("challengeType") String challengeType);

    List<ChallengeResponseDto> selectAllChallenges();

    ChallengeResponseDto selectChallengeById(@Param("id") String id);

    int countParticipant(@Param("challengeId") String challengeId,
                         @Param("userId") String userId);

    void insertParticipant(@Param("id") String id,
                            @Param("challengeId") String challengeId,
                            @Param("userId") String userId,
                            @Param("targetCount") int targetCount);

    List<ChallengeParticipantDto> selectMyParticipations(@Param("userId") String userId);

    int updateProgress(@Param("challengeId") String challengeId,
                       @Param("userId") String userId,
                       @Param("increment") int increment);

    int deleteParticipant(@Param("challengeId") String challengeId,
                          @Param("userId") String userId);

    int deleteChallenge(@Param("id") String id,
                        @Param("creatorId") String creatorId);
}
