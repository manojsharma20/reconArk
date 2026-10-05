package io.reconark.plugins.recon;

import io.reconark.kernel.api.ConfigSpec;
import io.reconark.kernel.api.PluginConfig;
import io.reconark.kernel.api.PropertySpec;
import io.reconark.kernel.api.PropertyType;
import io.reconark.spi.Comparison;
import io.reconark.spi.FieldComparator;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

/**
 * {@code date-window}: same business day in a timezone ({@code window: P0D}) or within a duration window. Accepts
 * ISO dates, ISO offset date-times and instants.
 */
final class DateWindowComparator implements FieldComparator {

    @Override
    public ConfigSpec parameters() {
        return ConfigSpec.of(
                PropertySpec.optional("timezone", PropertyType.STRING, "Asia/Dubai", "business timezone"),
                PropertySpec.optional("window", PropertyType.DURATION, "PT0S", "PT0S = same calendar day; otherwise max distance"));
    }

    @Override
    public Comparison compare(Object left, Object right, PluginConfig p) {
        if (left == null || right == null) {
            return left == right ? Comparison.EQUAL : Comparison.different("one side is missing");
        }
        ZoneId zone = ZoneId.of(p.string("timezone"));
        ZonedDateTime l;
        ZonedDateTime r;
        try {
            l = parse(left.toString(), zone);
            r = parse(right.toString(), zone);
        } catch (DateTimeParseException e) {
            return Comparison.different("not a date/time");
        }
        Duration window = p.duration("window");
        if (window.isZero()) {
            return l.toLocalDate().equals(r.toLocalDate())
                    ? Comparison.EQUAL
                    : Comparison.different(l.toLocalDate() + " vs " + r.toLocalDate() + " in " + zone);
        }
        Duration between = Duration.between(l, r).abs();
        return between.compareTo(window) <= 0 ? Comparison.EQUAL : Comparison.different("apart by " + between + ", window " + window);
    }

    private static ZonedDateTime parse(String s, ZoneId zone) {
        String t = s.strip();
        if (t.length() == 10) {
            return LocalDate.parse(t).atStartOfDay(zone);
        }
        if (t.endsWith("Z")) {
            return Instant.parse(t).atZone(zone);
        }
        return OffsetDateTime.parse(t).atZoneSameInstant(zone);
    }
}
