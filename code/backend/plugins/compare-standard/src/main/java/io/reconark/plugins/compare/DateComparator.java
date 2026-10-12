package io.reconark.plugins.compare;

import io.reconark.common.plugin.FieldComparator;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * Compares dates, optionally within a number of days. Each side can use its own date format.
 *
 * <p>Options:
 * <ul>
 *   <li>{@code format}: pattern for both sides, default {@code yyyy-MM-dd}</li>
 *   <li>{@code leftFormat} / {@code rightFormat}: override the pattern for one side, e.g. {@code yyyyMMdd}</li>
 *   <li>{@code toleranceDays}: default 0</li>
 * </ul>
 * Values that cannot be parsed are only equal when their text is identical.
 */
public class DateComparator implements FieldComparator {

    @Override
    public String id() {
        return "date";
    }

    @Override
    public String description() {
        return "Date comparison. Options: format, leftFormat, rightFormat, toleranceDays (default 0).";
    }

    @Override
    public boolean same(String left, String right, Map<String, String> options) {
        String common = options.getOrDefault("format", "yyyy-MM-dd");
        LocalDate l = parse(left, options.getOrDefault("leftFormat", common));
        LocalDate r = parse(right, options.getOrDefault("rightFormat", common));
        if (l == null || r == null) {
            return ExactComparator.clean(left).equals(ExactComparator.clean(right));
        }
        long tolerance = Long.parseLong(options.getOrDefault("toleranceDays", "0"));
        return Math.abs(ChronoUnit.DAYS.between(l, r)) <= tolerance;
    }

    private static LocalDate parse(String value, String pattern) {
        String clean = ExactComparator.clean(value);
        if (clean.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(clean, DateTimeFormatter.ofPattern(pattern));
        } catch (DateTimeParseException notADate) {
            return null;
        }
    }
}
