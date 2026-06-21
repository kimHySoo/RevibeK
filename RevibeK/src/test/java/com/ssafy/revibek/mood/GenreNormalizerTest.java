package com.ssafy.revibek.mood;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GenreNormalizerTest {

    @Test
    void 한글동의어를_표준코드로_변환한다() {
        assertThat(GenreNormalizer.normalize("댄스")).contains(GenreCode.DANCE);
        assertThat(GenreNormalizer.normalize("발라드")).contains(GenreCode.BALLAD);
        assertThat(GenreNormalizer.normalize("아이돌")).contains(GenreCode.IDOL);
        assertThat(GenreNormalizer.normalize("OST")).contains(GenreCode.OST);
    }

    @Test
    void 영문표기변형도_표준코드로_변환한다() {
        assertThat(GenreNormalizer.normalize("R&B")).contains(GenreCode.RNB);
        assertThat(GenreNormalizer.normalize("RnB")).contains(GenreCode.RNB);
        assertThat(GenreNormalizer.normalize("Hip-Hop")).contains(GenreCode.HIPHOP);
    }

    @Test
    void null과_빈문자열은_안전하게_빈값을반환한다() {
        assertThat(GenreNormalizer.normalize(null)).isEmpty();
        assertThat(GenreNormalizer.normalize("")).isEmpty();
    }

    @Test
    void 미등록값은_임의로_확정하지않는다() {
        assertThat(GenreNormalizer.normalize("환상적인장르")).isEmpty();
    }

    @Test
    void 복수장르문자열을_분리해서_모두인식한다() {
        assertThat(GenreNormalizer.normalizeAll("댄스, 힙합"))
                .containsExactlyInAnyOrder(GenreCode.DANCE, GenreCode.HIPHOP);
    }

    @Test
    void 복수장르중_우선순위에따라_대표장르하나를고른다() {
        assertThat(GenreNormalizer.normalizePrimary("댄스, 힙합")).contains(GenreCode.HIPHOP);
        assertThat(GenreNormalizer.normalizePrimary("OST, 힙합")).contains(GenreCode.OST);
    }
}
