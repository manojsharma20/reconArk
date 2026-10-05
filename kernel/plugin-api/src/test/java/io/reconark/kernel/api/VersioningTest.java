package io.reconark.kernel.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class VersioningTest {

    @Test
    void parsesAndOrdersSemanticVersions() {
        assertThat(SemVer.parse("1.2.3-rc.1+b5")).isEqualTo(new SemVer(1, 2, 3));
        assertThat(SemVer.parse("2")).isEqualTo(new SemVer(2, 0, 0));
        assertThat(SemVer.parse("1.10.0")).isGreaterThan(SemVer.parse("1.9.9"));
        assertThatThrownBy(() -> SemVer.parse("one")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rangesUseIntervalNotationOrCaretSemantics() {
        VersionRange r = VersionRange.parse("[1.0,2.0)");
        assertThat(r.contains(SemVer.parse("1.0.0"))).isTrue();
        assertThat(r.contains(SemVer.parse("1.99.0"))).isTrue();
        assertThat(r.contains(SemVer.parse("2.0.0"))).isFalse();
        assertThat(VersionRange.parse("1.4").contains(SemVer.parse("1.9"))).isTrue();
        assertThat(VersionRange.parse("1.4").contains(SemVer.parse("1.3"))).isFalse();
        assertThat(VersionRange.parse("(1.0,]").contains(SemVer.parse("9.0"))).isTrue();
    }

    @Test
    void configSpecRendersJsonSchema() {
        ConfigSpec spec = ConfigSpec.of(
                PropertySpec.required("host", PropertyType.STRING, "SFTP \"host\""),
                PropertySpec.secretRef("key-ref", "private key reference"),
                PropertySpec.optional("timeout", PropertyType.DURATION, "PT30S", "connect timeout"));
        String schema = spec.toJsonSchema("SFTP connector");
        assertThat(schema)
                .contains("\"title\":\"SFTP connector\"")
                .contains("\"host\":{\"type\":\"string\",\"description\":\"SFTP \\\"host\\\"\"}")
                .contains("\"x-reconark-secret-ref\":true")
                .contains("\"format\":\"duration\"")
                .contains("\"required\":[\"host\",\"key-ref\"]");
    }

    @Test
    void extensionPointIdsAreKebabCaseInterfaces() {
        assertThatThrownBy(() -> ExtensionPoint.of("FormatReader", Runnable.class, Cardinality.KEYED))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ExtensionPoint.of("x", String.class, Cardinality.KEYED))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
