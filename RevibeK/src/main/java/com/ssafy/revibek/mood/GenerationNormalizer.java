package com.ssafy.revibek.mood;

import java.util.Map;
import java.util.Optional;

/**
 * 세대 입력값(한글 라벨/영문 동의어/코드)을 {@link GenerationCode}로 변환한다.
 * 1세대·4세대·5세대 등 서비스 대상 외 값은 임의로 SECOND/THIRD에 합치지 않고
 * Optional.empty()를 반환한다(검토 대상으로 별도 처리).
 */
public final class GenerationNormalizer {

    private static final Map<String, GenerationCode> ALIASES = Map.ofEntries(
            Map.entry("2세대", GenerationCode.SECOND),
            Map.entry("2 세대", GenerationCode.SECOND),
            Map.entry("second", GenerationCode.SECOND),
            Map.entry("gen2", GenerationCode.SECOND),
            Map.entry("3세대", GenerationCode.THIRD),
            Map.entry("3 세대", GenerationCode.THIRD),
            Map.entry("third", GenerationCode.THIRD),
            Map.entry("gen3", GenerationCode.THIRD),
            Map.entry("전체", GenerationCode.ALL),
            Map.entry("all", GenerationCode.ALL)
    );

    private GenerationNormalizer() {
    }

    public static Optional<GenerationCode> normalize(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return Optional.empty();
        }
        for (GenerationCode code : GenerationCode.values()) {
            if (code.name().equalsIgnoreCase(value)) {
                return Optional.of(code);
            }
        }
        return Optional.ofNullable(ALIASES.get(value.toLowerCase()));
    }

    /** DB의 기존 generation 컬럼이 한글 라벨을 그대로 쓰므로, 코드를 한글 라벨로 역변환한다. */
    public static String toLabel(GenerationCode code) {
        return code == null ? null : code.label();
    }
}
