package io.reconark.kernel.api;

import java.util.Objects;

/**
 * A version range in interval notation: {@code [1.0,2.0)} means {@code >= 1.0.0} and {@code < 2.0.0}.
 * A bare version such as {@code 1.2} means {@code [1.2,2.0)} — compatible within the same major version.
 *
 * @param min lower bound
 * @param minInclusive whether {@code min} is included
 * @param max upper bound, or {@code null} for unbounded
 * @param maxInclusive whether {@code max} is included
 */
public record VersionRange(SemVer min, boolean minInclusive, SemVer max, boolean maxInclusive) {

    public VersionRange {
        Objects.requireNonNull(min, "min");
    }

    /** Parses interval notation or a bare version (caret semantics). */
    public static VersionRange parse(String text) {
        String t = Objects.requireNonNull(text, "range").strip();
        if (t.isEmpty()) {
            throw new IllegalArgumentException("Empty version range");
        }
        char first = t.charAt(0);
        if (first != '[' && first != '(') {
            SemVer v = SemVer.parse(t);
            return new VersionRange(v, true, new SemVer(v.major() + 1, 0, 0), false);
        }
        char last = t.charAt(t.length() - 1);
        if (last != ']' && last != ')') {
            throw new IllegalArgumentException("Unterminated version range: " + text);
        }
        String[] bounds = t.substring(1, t.length() - 1).split(",", -1);
        if (bounds.length != 2) {
            throw new IllegalArgumentException("Version range needs two bounds: " + text);
        }
        SemVer lo = SemVer.parse(bounds[0]);
        SemVer hi = bounds[1].isBlank() ? null : SemVer.parse(bounds[1]);
        return new VersionRange(lo, first == '[', hi, last == ']');
    }

    /** Returns whether {@code v} lies inside this range. */
    public boolean contains(SemVer v) {
        int lo = v.compareTo(min);
        if (lo < 0 || (lo == 0 && !minInclusive)) {
            return false;
        }
        if (max == null) {
            return true;
        }
        int hi = v.compareTo(max);
        return hi < 0 || (hi == 0 && maxInclusive);
    }

    @Override
    public String toString() {
        return (minInclusive ? "[" : "(") + min + "," + (max == null ? "" : max) + (maxInclusive ? "]" : ")");
    }
}
