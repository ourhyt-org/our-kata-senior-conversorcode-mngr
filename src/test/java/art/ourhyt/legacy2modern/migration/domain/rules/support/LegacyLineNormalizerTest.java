package art.ourhyt.legacy2modern.migration.domain.rules.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LegacyLineNormalizerTest {
    @Test
    void removesTrailingDotFromStopRun() {
        assertEquals("STOP RUN", LegacyLineNormalizer.normalizeCobolStatement("STOP RUN."));
    }

    @Test
    void removesTrailingDotFromEndIf() {
        assertEquals("END-IF", LegacyLineNormalizer.normalizeCobolStatement("END-IF."));
    }

    @Test
    void preservesInnerDoubleQuotedDots() {
        assertEquals("DISPLAY \"A.B\"", LegacyLineNormalizer.normalizeCobolStatement("DISPLAY \"A.B\"."));
    }

    @Test
    void preservesInnerSingleQuotedDots() {
        assertEquals("DISPLAY 'A.B'", LegacyLineNormalizer.normalizeCobolStatement("DISPLAY 'A.B'."));
    }
}
