package com.ssafy.revibek.coaching.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.ssafy.revibek.coaching.dto.CoachingResponseDto;
import com.ssafy.revibek.preference.dto.UserPreferenceDto;
import com.ssafy.revibek.preference.service.PreferenceService;
import com.ssafy.revibek.radio.dto.RadioResponseDto;
import com.ssafy.revibek.radio.mapper.RadioMapper;
import com.ssafy.revibek.usersong.dto.UserSongResponseDto;
import com.ssafy.revibek.usersong.mapper.UserSongMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CoachingService {

    private final UserSongMapper userSongMapper;
    private final RadioMapper radioMapper;
    private final PreferenceService preferenceService;

    public CoachingResponseDto analyzeUser(String userId) {
        List<UserSongResponseDto> savedSongs = userSongMapper.selectSavedSongs(userId);
        List<RadioResponseDto> radioSessions = radioMapper.selectRadioSessionByUserId(userId);
        UserPreferenceDto preference = preferenceService.getPreference(userId);

        String emotionTendency = analyzeEmotionTendency(radioSessions);
        String generationPreference = analyzeGenerationPreference(preference);
        String situationAdvice = buildSituationAdvice(preference, savedSongs);
        String recommendationDirection = buildRecommendationDirection(preference, savedSongs);
        List<String> insights = buildInsights(savedSongs, radioSessions, preference);
        String summary = buildSummary(emotionTendency, generationPreference, savedSongs.size());

        return CoachingResponseDto.builder()
                .emotionTendency(emotionTendency)
                .generationPreference(generationPreference)
                .situationAdvice(situationAdvice)
                .recommendationDirection(recommendationDirection)
                .insightMessages(insights)
                .overallSummary(summary)
                .build();
    }

    private String analyzeEmotionTendency(List<RadioResponseDto> sessions) {
        if (sessions.isEmpty()) return "아직 라디오 세션 기록이 없습니다.";

        Map<String, Long> moodCount = sessions.stream()
                .collect(Collectors.groupingBy(RadioResponseDto::getMood, Collectors.counting()));

        String topMood = moodCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("알 수 없음");

        long topCount = moodCount.getOrDefault(topMood, 0L);
        return String.format("'%s' 감정을 %d번 선택하여 가장 자주 찾는 음악 분위기입니다.", topMood, topCount);
    }

    private String analyzeGenerationPreference(UserPreferenceDto preference) {
        if (preference == null || preference.getPreferredGenerations().isEmpty()) {
            return "세대 취향이 아직 설정되지 않았습니다.";
        }
        String topGeneration = preference.getPreferredGenerations().get(0);
        return String.format("%s K-POP 선호도가 높습니다.", topGeneration);
    }

    private String buildSituationAdvice(UserPreferenceDto preference, List<UserSongResponseDto> savedSongs) {
        if (preference == null || preference.getPreferredMoods().isEmpty()) {
            return "취향 데이터가 부족합니다. 더 많은 음악을 감상해보세요.";
        }
        String mood = preference.getPreferredMoods().get(0);
        return switch (mood) {
            case "신나는" -> "운동이나 활동적인 상황에서 빠른 BPM 곡이 잘 어울립니다.";
            case "위로" -> "힘든 시간에 발라드와 감성적인 음악이 마음을 어루만져 줍니다.";
            case "감성" -> "조용한 밤이나 카페에서 감성 음악을 즐겨보세요.";
            case "몽환" -> "집중이 필요할 때 몽환적인 인디 음악이 효과적입니다.";
            case "청량" -> "아침이나 상쾌한 시작을 원할 때 청량한 팝 음악을 추천합니다.";
            case "강렬함" -> "에너지가 필요한 순간 강렬한 댄스 음악을 활용해보세요.";
            default -> "다양한 분위기의 음악을 탐색하며 나만의 취향을 찾아보세요.";
        };
    }

    private String buildRecommendationDirection(UserPreferenceDto preference, List<UserSongResponseDto> savedSongs) {
        if (savedSongs.isEmpty()) {
            return "아직 저장한 곡이 없습니다. 마음에 드는 곡을 저장해보세요.";
        }

        double avgRating = savedSongs.stream()
                .filter(s -> s.getRating() != null && s.getRating() > 0)
                .mapToInt(UserSongResponseDto::getRating)
                .average()
                .orElse(0.0);

        String ratingMsg = avgRating > 0
                ? String.format("저장곡 평균 별점 %.1f점을 기반으로", avgRating)
                : "저장 목록을 기반으로";

        String genMsg = (preference != null && !preference.getPreferredGenerations().isEmpty())
                ? preference.getPreferredGenerations().get(0) + " "
                : "";

        return String.format("%s %sK-POP 중심의 추천 방향을 제안합니다.", ratingMsg, genMsg);
    }

    private List<String> buildInsights(List<UserSongResponseDto> savedSongs,
                                        List<RadioResponseDto> sessions,
                                        UserPreferenceDto preference) {
        List<String> insights = new ArrayList<>();

        if (!savedSongs.isEmpty()) {
            insights.add(String.format("총 %d곡을 저장했습니다.", savedSongs.size()));

            long highRated = savedSongs.stream()
                    .filter(s -> s.getRating() != null && s.getRating() >= 4)
                    .count();
            if (highRated > 0) {
                insights.add(String.format("별점 4점 이상의 곡이 %d개 있어 음악 취향이 뚜렷합니다.", highRated));
            }

            UserSongResponseDto mostPlayed = savedSongs.stream()
                    .max((a, b) -> Integer.compare(a.getPlayCount(), b.getPlayCount()))
                    .orElse(null);
            if (mostPlayed != null && mostPlayed.getPlayCount() > 1) {
                insights.add(String.format("'%s - %s'을 %d번 재생하여 가장 많이 들은 곡입니다.",
                        mostPlayed.getTitle(), mostPlayed.getArtist(), mostPlayed.getPlayCount()));
            }
        }

        if (!sessions.isEmpty()) {
            insights.add(String.format("총 %d번 AI 라디오를 이용했습니다.", sessions.size()));
        }

        if (preference != null && !preference.getPreferredArtists().isEmpty()) {
            insights.add(String.format("선호 아티스트 '%s' 관련 곡을 적극 추천해드립니다.",
                    preference.getPreferredArtists().get(0)));
        }

        if (insights.isEmpty()) {
            insights.add("더 많은 음악을 감상하고 저장하면 맞춤형 분석을 제공할 수 있습니다.");
        }

        return insights;
    }

    private String buildSummary(String emotionTendency, String generationPreference, int savedCount) {
        return String.format(
                "당신은 %s %s 저장곡 %d개를 통해 음악과 깊게 교감하고 있습니다. " +
                "AI 라디오를 통해 더 다양한 K-POP 세계를 탐험해보세요!",
                emotionTendency, generationPreference, savedCount
        );
    }
}
