package com.ssafy.revibek.challenge.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.revibek.challenge.dto.ChallengeParticipantDto;
import com.ssafy.revibek.challenge.dto.ChallengeRequestDto;
import com.ssafy.revibek.challenge.dto.ChallengeResponseDto;
import com.ssafy.revibek.challenge.dto.ChallengeTemplateDto;
import com.ssafy.revibek.challenge.mapper.ChallengeMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChallengeService {

    private final ChallengeMapper challengeMapper;

    @Transactional
    public void createChallenge(String userId, ChallengeRequestDto dto) {
        challengeMapper.insertChallenge(
                UUID.randomUUID().toString(),
                userId,
                dto.getTitle(),
                dto.getDescription(),
                dto.getStartDate(),
                dto.getEndDate(),
                dto.getTargetCount(),
                dto.getChallengeType()
        );
    }

    public List<ChallengeTemplateDto> getTemplates() {
        return List.of(
            new ChallengeTemplateDto("daily-2nd-gen",   "GENERATION_LISTENING",
                "7일 동안 2세대 K-POP 듣기",
                "일주일 동안 매일 2세대 K-POP 라디오를 들어보세요.",
                7, 7, "2세대 감성", "회상", "2세대"),
            new ChallengeTemplateDto("workout-radio-5", "WORKOUT_MUSIC",
                "운동 라디오 5회 듣기",
                "운동할 때 RevibeK 라디오를 5회 들어보세요.",
                5, 14, "운동", "자신감", "3세대"),
            new ChallengeTemplateDto("study-music-3",   "STUDY_MUSIC",
                "공부 음악 3회 듣기",
                "공부할 때 집중용 K-POP 라디오를 3회 들어보세요.",
                3, 7, "학습", "집중", "2세대"),
            new ChallengeTemplateDto("midnight-mood",   "DAILY_MOOD",
                "새벽 감성 라디오 5회 듣기",
                "새벽 감성에 맞는 라디오를 5회 감상해보세요.",
                5, 10, "새벽감성", "감성", "2세대")
        );
    }

    public List<ChallengeResponseDto> getAllChallenges() {
        return challengeMapper.selectAllChallenges();
    }

    public ChallengeResponseDto getChallenge(String challengeId) {
        ChallengeResponseDto challenge = challengeMapper.selectChallengeById(challengeId);
        if (challenge == null) {
            throw new RuntimeException("챌린지를 찾을 수 없습니다.");
        }
        return challenge;
    }

    @Transactional
    public void joinChallenge(String challengeId, String userId) {
        ChallengeResponseDto challenge = getChallenge(challengeId);
        if (challengeMapper.countParticipant(challengeId, userId) > 0) {
            throw new IllegalStateException("이미 참여 중인 챌린지입니다.");
        }
        challengeMapper.insertParticipant(
                UUID.randomUUID().toString(),
                challengeId,
                userId,
                challenge.getTargetCount()
        );
    }

    public List<ChallengeParticipantDto> getMyChallenges(String userId) {
        return challengeMapper.selectMyParticipations(userId);
    }

    @Transactional
    public void updateProgress(String challengeId, String userId, int increment) {
        int updated = challengeMapper.updateProgress(challengeId, userId, increment);
        if (updated == 0) {
            throw new RuntimeException("챌린지 참여 정보를 찾을 수 없습니다.");
        }
    }

    @Transactional
    public void leaveChallenge(String challengeId, String userId) {
        int deleted = challengeMapper.deleteParticipant(challengeId, userId);
        if (deleted == 0) {
            throw new RuntimeException("참여 중인 챌린지가 아닙니다.");
        }
    }

    @Transactional
    public void deleteChallenge(String challengeId, String userId) {
        int deleted = challengeMapper.deleteChallenge(challengeId, userId);
        if (deleted == 0) {
            throw new RuntimeException("챌린지를 찾을 수 없거나 삭제 권한이 없습니다.");
        }
    }
}
