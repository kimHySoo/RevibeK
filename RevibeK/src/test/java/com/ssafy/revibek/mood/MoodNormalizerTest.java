package com.ssafy.revibek.mood;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MoodNormalizerTest {

    @Test
    void 동의어를_표준코드로_변환한다() {
        assertThat(MoodNormalizer.normalize("지침")).contains(MoodCode.TIRED);
        assertThat(MoodNormalizer.normalize("지친")).contains(MoodCode.TIRED);
        assertThat(MoodNormalizer.normalize("설렘")).contains(MoodCode.EXCITED);
        assertThat(MoodNormalizer.normalize("설레는")).contains(MoodCode.EXCITED);
        assertThat(MoodNormalizer.normalize("회상")).contains(MoodCode.NOSTALGIC);
        assertThat(MoodNormalizer.normalize("그리운")).contains(MoodCode.NOSTALGIC);
        assertThat(MoodNormalizer.normalize("자신감")).contains(MoodCode.CONFIDENT);
        assertThat(MoodNormalizer.normalize("위로")).contains(MoodCode.COMFORT);
        assertThat(MoodNormalizer.normalize("외로운")).contains(MoodCode.LONELY);
        assertThat(MoodNormalizer.normalize("신나는")).contains(MoodCode.ENERGETIC);
    }

    @Test
    void 표준코드_문자열은_대소문자무관하게_그대로_통과한다() {
        assertThat(MoodNormalizer.normalize("LONELY")).contains(MoodCode.LONELY);
        assertThat(MoodNormalizer.normalize("lonely")).contains(MoodCode.LONELY);
    }

    @Test
    void 공백이포함된입력은_trim하여_처리한다() {
        assertThat(MoodNormalizer.normalize("  외로운  ")).contains(MoodCode.LONELY);
    }

    @Test
    void null과_빈문자열은_안전하게_빈값을반환한다() {
        assertThat(MoodNormalizer.normalize(null)).isEmpty();
        assertThat(MoodNormalizer.normalize("")).isEmpty();
        assertThat(MoodNormalizer.normalize("   ")).isEmpty();
    }

    @Test
    void 미등록값은_빈값을반환하고_검토대상여부를_구분할수있다() {
        assertThat(MoodNormalizer.normalize("환상")).isEmpty();
        assertThat(MoodNormalizer.isReviewCandidate("환상")).isTrue();
        assertThat(MoodNormalizer.normalize("존재하지않는감정")).isEmpty();
        assertThat(MoodNormalizer.isReviewCandidate("존재하지않는감정")).isFalse();
    }
}
