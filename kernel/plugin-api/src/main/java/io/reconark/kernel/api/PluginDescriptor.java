package io.reconark.kernel.api;

import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * What a plugin is, what it provides and what it needs. Read by the kernel before the plugin is activated.
 *
 * @param id stable, globally unique kebab-case id, e.g. {@code bus-kafka}
 * @param version the plugin's own version
 * @param kernelApi range of {@link KernelApi#VERSION} this plugin works with
 * @param name display name
 * @param description one-line description
 * @param trustTier where the plugin may run
 * @param provides ids of extension points this plugin contributes to
 * @param requires extension point ids (e.g. {@code secret-provider}) that must be bound before this plugin registers
 * @param config configuration the plugin accepts
 */
public record PluginDescriptor(
        String id,
        SemVer version,
        VersionRange kernelApi,
        String name,
        String description,
        TrustTier trustTier,
        Set<String> provides,
        Set<String> requires,
        ConfigSpec config) {

    private static final Pattern ID = Pattern.compile("[a-z][a-z0-9]*(-[a-z0-9]+)*");

    public PluginDescriptor {
        Objects.requireNonNull(id, "id");
        if (!ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Plugin id must be kebab-case: " + id);
        }
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(kernelApi, "kernelApi");
        Objects.requireNonNull(trustTier, "trustTier");
        name = name == null ? id : name;
        description = description == null ? "" : description;
        provides = Set.copyOf(provides == null ? Set.of() : provides);
        requires = Set.copyOf(requires == null ? Set.of() : requires);
        config = config == null ? ConfigSpec.NONE : config;
    }

    /** Starts a builder for a plugin with the given id and version. */
    public static Builder builder(String id, String version) {
        return new Builder(id, SemVer.parse(version));
    }

    /** Fluent builder; defaults to kernel API {@code [1.0,2.0)}, tier {@link TrustTier#CORE}, no config. */
    public static final class Builder {
        private final String id;
        private final SemVer version;
        private VersionRange kernelApi = VersionRange.parse("[1.0,2.0)");
        private String name;
        private String description;
        private TrustTier trustTier = TrustTier.CORE;
        private Set<String> provides = Set.of();
        private Set<String> requires = Set.of();
        private ConfigSpec config = ConfigSpec.NONE;

        private Builder(String id, SemVer version) {
            this.id = id;
            this.version = version;
        }

        public Builder kernelApi(String range) {
            this.kernelApi = VersionRange.parse(range);
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder trustTier(TrustTier trustTier) {
            this.trustTier = trustTier;
            return this;
        }

        public Builder provides(ExtensionPoint<?>... points) {
            this.provides = idsOf(points);
            return this;
        }

        public Builder requires(ExtensionPoint<?>... points) {
            this.requires = idsOf(points);
            return this;
        }

        public Builder config(ConfigSpec config) {
            this.config = config;
            return this;
        }

        public PluginDescriptor build() {
            return new PluginDescriptor(id, version, kernelApi, name, description, trustTier, provides, requires, config);
        }

        private static Set<String> idsOf(ExtensionPoint<?>... points) {
            java.util.LinkedHashSet<String> ids = new java.util.LinkedHashSet<>();
            for (ExtensionPoint<?> p : points) {
                ids.add(p.id());
            }
            return ids;
        }
    }
}
