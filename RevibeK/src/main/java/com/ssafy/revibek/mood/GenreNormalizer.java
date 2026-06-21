package com.ssafy.revibek.mood;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 장르 입력값(한글/영문 동의어/코드, 복수 표기 포함)을 {@link GenreCode}로 변환한다.
 * 자동 분류 근거가 부족하면(예: 알 수 없는 값) 비워두고, 임의로 6개 중 하나로 확정하지 않는다.
 */
public final class GenreNormalizer {

    private static final Map<String, GenreCode> ALIASES = Map.ofEntries(
            Map.entry("댄스", GenreCode.DANCE),
            Map.entry("dance", GenreCode.DANCE),
            Map.entry("댄스곡", GenreCode.DANCE),
            Map.entry("edm", GenreCode.DANCE),
            Map.entry("일렉트로닉 댄스", GenreCode.DANCE),
            Map.entry("발라드", GenreCode.BALLAD),
            Map.entry("ballad", GenreCode.BALLAD),
            Map.entry("발라드곡", GenreCode.BALLAD),
            Map.entry("팝 발라드", GenreCode.BALLAD),
            Map.entry("r&b", GenreCode.RNB),
            Map.entry("rnb", GenreCode.RNB),
            Map.entry("알앤비", GenreCode.RNB),
            Map.entry("소울", GenreCode.RNB),
            Map.entry("힙합", GenreCode.HIPHOP),
            Map.entry("hip-hop", GenreCode.HIPHOP),
            Map.entry("hiphop", GenreCode.HIPHOP),
            Map.entry("랩", GenreCode.HIPHOP),
            Map.entry("rap", GenreCode.HIPHOP),
            Map.entry("아이돌", GenreCode.IDOL),
            Map.entry("idol", GenreCode.IDOL),
            Map.entry("아이돌 음악", GenreCode.IDOL),
            Map.entry("아이돌곡", GenreCode.IDOL),
            Map.entry("ost", GenreCode.OST),
            Map.entry("o.s.t", GenreCode.OST),
            Map.entry("드라마 ost", GenreCode.OST),
            Map.entry("영화 ost", GenreCode.OST),
            Map.entry("애니메이션 ost", GenreCode.OST),
            Map.entry("사운드트랙", GenreCode.OST)
    );

    /** 여러 감정 중 하나의 대표 장르를 골라야 할 때 사용하는 우선순위. */
    private static final Comparator<GenreCode> PRIORITY = Comparator.comparingInt(code -> switch (code) {
        case OST -> 0;
        case HIPHOP -> 1;
        case RNB -> 2;
        case BALLAD -> 3;
        case DANCE -> 4;
        case IDOL -> 5;
    });

    private GenreNormalizer() {
    }

    public static Optional<GenreCode> normalize(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return Optional.empty();
        }
        for (GenreCode code : GenreCode.values()) {
            if (code.name().equalsIgnoreCase(value)) {
                return Optional.of(code);
            }
        }
        return Optional.ofNullable(ALIASES.get(value.toLowerCase()));
    }

    /**
     * "댄스/발라드", "Hip-Hop,R&B" 처럼 한 칸에 여러 장르가 적힌 입력을 분리해
     * 매핑되는 코드들을 모두 반환한다. 매핑되지 않는 토큰은 조용히 무시한다.
     */
    public static Set<GenreCode> normalizeAll(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return Set.of();
        }
        Set<GenreCode> result = new LinkedHashSet<>();
        for (String token : raw.split("[,/&|]")) {
            normalize(token).ifPresent(result::add);
        }
        return result;
    }

    /** 단일값 컬럼에 대표 장르 하나만 저장해야 할 때, 우선순위에 따라 하나를 고른다. */
    public static Optional<GenreCode> normalizePrimary(String raw) {
        return normalizeAll(raw).stream().min(PRIORITY);
    }

    public static String toLabel(GenreCode code) {
        return code == null ? null : code.label();
    }
}
