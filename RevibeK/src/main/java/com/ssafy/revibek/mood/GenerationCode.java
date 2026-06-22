package com.ssafy.revibek.mood;

/**
 * 서비스 추천 대상 세대 코드. ALL은 검색 필터 해제 조건일 뿐, 곡의 실제
 * 세대값으로 저장하지 않는다(songs.generation에 ALL을 넣지 말 것).
 */
public enum GenerationCode {
    SECOND("2세대"),
    THIRD("3세대"),
    ALL("전체");

    private final String label;

    GenerationCode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
