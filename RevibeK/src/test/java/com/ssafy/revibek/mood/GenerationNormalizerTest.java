package com.ssafy.revibek.mood;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GenerationNormalizerTest {

    @Test
    void 한글라벨을_표준코드로_변환한다() {
        assertThat(GenerationNormalizer.normalize("2세대")).contains(GenerationCode.SECOND);
        assertThat(GenerationNormalizer.normalize("3세대")).contains(GenerationCode.THIRD);
        assertThat(GenerationNormalizer.normalize("전체")).contains(GenerationCode.ALL);
    }

    @Test
    void 표준코드는_그대로_통과한다() {
        assertThat(GenerationNormalizer.normalize("SECOND")).contains(GenerationCode.SECOND);
        assertThat(GenerationNormalizer.normalize("ALL")).contains(GenerationCode.ALL);
    }

    @Test
    void 서비스대상이아닌세대는_임의로변환하지않는다() {
        assertThat(GenerationNormalizer.normalize("1세대")).isEmpty();
        assertThat(GenerationNormalizer.normalize("4세대")).isEmpty();
        assertThat(GenerationNormalizer.normalize("5세대")).isEmpty();
    }

    @Test
    void null과_빈문자열은_안전하게_빈값을반환한다() {
        assertThat(GenerationNormalizer.normalize(null)).isEmpty();
        assertThat(GenerationNormalizer.normalize("")).isEmpty();
    }

    @Test
    void 코드를_한글라벨로_역변환한다() {
        assertThat(GenerationNormalizer.toLabel(GenerationCode.SECOND)).isEqualTo("2세대");
        assertThat(GenerationNormalizer.toLabel(GenerationCode.THIRD)).isEqualTo("3세대");
    }
}
