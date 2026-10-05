package io.reconark.kernel.api;

import java.util.Objects;

/**
 * A semantic version (MAJOR.MINOR.PATCH). Pre-release and build metadata are accepted and ignored for ordering.
 *
 * @param major incompatible API changes
 * @param minor backwards-compatible additions
 * @param patch backwards-compatible fixes
 */
public record SemVer(int major, int minor, int patch) implements Comparable<SemVer> {

    public SemVer {
        if (major < 0 || minor < 0 || patch < 0) {
            throw new IllegalArgumentException("Version parts must be >= 0");
        }
    }

    /** Parses {@code 1}, {@code 1.2}, {@code 1.2.3} or {@code 1.2.3-rc.1+build}. */
    public static SemVer parse(String text) {
        Objects.requireNonNull(text, "version");
        String core = text.strip();
        int cut = indexOfAny(core, '-', '+');
        if (cut >= 0) {
            core = core.substring(0, cut);
        }
        String[] parts = core.split("\\.", -1);
        if (parts.length == 0 || parts.length > 3) {
            throw new IllegalArgumentException("Not a semantic version: " + text);
        }
        try {
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            return new SemVer(major, minor, patch);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Not a semantic version: " + text, e);
        }
    }

    private static int indexOfAny(String s, char a, char b) {
        int ia = s.indexOf(a);
        int ib = s.indexOf(b);
        if (ia < 0) {
            return ib;
        }
        return ib < 0 ? ia : Math.min(ia, ib);
    }

    @Override
    public int compareTo(SemVer o) {
        int c = Integer.compare(major, o.major);
        if (c != 0) {
            return c;
        }
        c = Integer.compare(minor, o.minor);
        return c != 0 ? c : Integer.compare(patch, o.patch);
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
