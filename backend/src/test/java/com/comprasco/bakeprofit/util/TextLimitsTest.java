package com.comprasco.bakeprofit.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.comprasco.bakeprofit.core.utils.TextLimits;

class TextLimitsTest {

    private static final int MAX = 5;

    @Test
    void dejaIgualUnTextoQueCabe() {
        assertThat(TextLimits.truncate("arroz", MAX)).isEqualTo("arroz");
        assertThat(TextLimits.truncate(null, MAX)).isNull();
    }

    @Test
    void recortaPorCaracteresSinPartirUnEmoji() {
        String truncated = TextLimits.truncate("abcd😀😀", MAX);

        assertThat(truncated).isEqualTo("abcd😀");
        assertThat(truncated.codePointCount(0, truncated.length())).isEqualTo(MAX);
    }
}
