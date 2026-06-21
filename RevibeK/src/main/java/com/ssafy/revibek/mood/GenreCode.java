package com.ssafy.revibek.mood;

/** 서비스에서 허용하는 장르 코드. 정확히 6개로 고정한다. */
public enum GenreCode {
    DANCE("댄스"),
    BALLAD("발라드"),
    RNB("R&B"),
    HIPHOP("힙합"),
    IDOL("아이돌"),
    OST("OST");

    private final String label;

    GenreCode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
