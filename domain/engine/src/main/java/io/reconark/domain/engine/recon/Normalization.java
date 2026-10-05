package io.reconark.domain.engine.recon;

import java.util.Locale;

/** Match-key normalisations (build prompt §6.6). */
public enum Normalization {
    TRIM {
        @Override
        String apply(String s) {
            return s.strip();
        }
    },
    UPPER_CASE {
        @Override
        String apply(String s) {
            return s.toUpperCase(Locale.ROOT);
        }
    },
    STRIP_LEADING_ZEROS {
        @Override
        String apply(String s) {
            String t = s.replaceFirst("^0+(?=.)", "");
            return t;
        }
    },
    REMOVE_SEPARATORS {
        @Override
        String apply(String s) {
            return s.replaceAll("[\\s\\-_/.:]", "");
        }
    };

    abstract String apply(String s);
}
