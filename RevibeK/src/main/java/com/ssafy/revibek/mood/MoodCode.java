package com.ssafy.revibek.mood;

/**
 * 서비스 전역에서 사용하는 표준 감정 코드. 정확히 7개로 고정한다.
 * DB(moods.code)·Qdrant payload·추천 쿼리 파라미터에는 이 enum의 name()만 사용하고,
 * ordinal()은 절대 저장하지 않는다(순서 변경 시 기존 데이터가 깨지기 때문).
 */
public enum MoodCode {
    TIRED("지침"),
    EXCITED("설렘"),
    NOSTALGIC("회상"),
    CONFIDENT("자신감"),
    COMFORT("위로"),
    LONELY("외로움"),
    ENERGETIC("신남");

    private final String label;

    MoodCode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
