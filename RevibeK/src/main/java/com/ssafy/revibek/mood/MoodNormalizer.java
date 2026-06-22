package com.ssafy.revibek.mood;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 사용자 입력(한글 라벨/동의어) 또는 표준 코드 문자열을 {@link MoodCode}로 변환한다.
 * songs.mood / radio_sessions.mood / user_preferences.preferred_moods 어디서 와도
 * 동일하게 동작하며, 알 수 없는 값은 예외 없이 Optional.empty()를 반환한다.
 */
public final class MoodNormalizer {

    private static final Map<String, MoodCode> ALIASES = Map.ofEntries(
            Map.entry("지침", MoodCode.TIRED),
            Map.entry("지친", MoodCode.TIRED),
            Map.entry("피곤함", MoodCode.TIRED),
            Map.entry("피곤한", MoodCode.TIRED),
            Map.entry("무기력", MoodCode.TIRED),
            Map.entry("힘듦", MoodCode.TIRED),
            Map.entry("설렘", MoodCode.EXCITED),
            Map.entry("설레는", MoodCode.EXCITED),
            Map.entry("설레임", MoodCode.EXCITED),
            Map.entry("회상", MoodCode.NOSTALGIC),
            Map.entry("그리움", MoodCode.NOSTALGIC),
            Map.entry("그리운", MoodCode.NOSTALGIC),
            Map.entry("추억", MoodCode.NOSTALGIC),
            Map.entry("추억회상", MoodCode.NOSTALGIC),
            Map.entry("과거회상", MoodCode.NOSTALGIC),
            Map.entry("향수", MoodCode.NOSTALGIC),
            Map.entry("자신감", MoodCode.CONFIDENT),
            Map.entry("자신있는", MoodCode.CONFIDENT),
            Map.entry("당당함", MoodCode.CONFIDENT),
            Map.entry("당당한", MoodCode.CONFIDENT),
            Map.entry("파이팅", MoodCode.CONFIDENT),
            Map.entry("위로", MoodCode.COMFORT),
            Map.entry("힐링", MoodCode.COMFORT),
            Map.entry("편안함", MoodCode.COMFORT),
            Map.entry("안정", MoodCode.COMFORT),
            Map.entry("따뜻함", MoodCode.COMFORT),
            Map.entry("외로움", MoodCode.LONELY),
            Map.entry("외로운", MoodCode.LONELY),
            Map.entry("쓸쓸함", MoodCode.LONELY),
            Map.entry("쓸쓸한", MoodCode.LONELY),
            Map.entry("고독", MoodCode.LONELY),
            Map.entry("신남", MoodCode.ENERGETIC),
            Map.entry("신나는", MoodCode.ENERGETIC),
            Map.entry("흥겨움", MoodCode.ENERGETIC),
            Map.entry("즐거움", MoodCode.ENERGETIC),
            Map.entry("행복한", MoodCode.ENERGETIC),
            Map.entry("활기", MoodCode.ENERGETIC)
    );

    /**
     * 최종 7개 코드와 의미가 겹치지 않거나 모호해서 자동으로 합치지 않는 값.
     * normalize()는 이 값들에 대해 Optional.empty()를 반환하되, 호출 측에서
     * "알 수 없음"과 "검토가 필요한 후보"를 구분하고 싶을 때 isReviewCandidate()로 식별한다.
     */
    private static final Set<String> REVIEW_CANDIDATES = Set.of(
            "환상", "몽환", "감성", "청량", "강렬함", "슬픔", "슬픈", "행복"
    );

    private MoodNormalizer() {
    }

    public static Optional<MoodCode> normalize(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return Optional.empty();
        }
        for (MoodCode code : MoodCode.values()) {
            if (code.name().equalsIgnoreCase(value)) {
                return Optional.of(code);
            }
        }
        return Optional.ofNullable(ALIASES.get(value));
    }

    public static boolean isReviewCandidate(String raw) {
        if (raw == null) {
            return false;
        }
        return REVIEW_CANDIDATES.contains(raw.trim());
    }
}
