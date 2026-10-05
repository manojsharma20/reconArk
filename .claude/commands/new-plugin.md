---
description: Scaffold a new reconArk brick (plugin) for an extension point, with descriptor, ServiceLoader registration and TCK tests
argument-hint: <plugin-id> <extension-point-id> [key]
---

Create a new reconArk plugin. Arguments: $ARGUMENTS (plugin id in kebab-case, extension point id from
`domain/spi/src/main/java/io/reconark/spi/ExtensionPoints.java`, optional extension key, which defaults to the
last segment of the plugin id).

Follow LLD §15 "Add a brick" exactly:

1. Read `ExtensionPoints.java` and the contract interface for the point. If the point doesn't exist, stop and
   propose `/new-adr` instead. New sockets need an ADR.
2. Create `plugins/<plugin-id>/build.gradle.kts` with `plugins { id("reconark.plugin") }` and a one-line
   `description`. Add third-party dependencies only through `gradle/libs.versions.toml`, and ask first.
3. Package `io.reconark.plugins.<area>.<name>`. Create:
   - `package-info.java`;
   - the implementation class (package-private, `final`);
   - `public final class <Name>Plugin implements ReconArkPlugin` with a `PluginDescriptor`. The descriptor sets:
     - id;
     - version `0.1.0`;
     - name and description;
     - `provides(...)`;
     - `requires(...)`, if needed;
     - `ConfigSpec`, with secret values as `PropertySpec.secretRef` only;
     - trust tier: `CORE`, or `DEV_ONLY` for local conveniences.
4. Register the plugin class in `src/main/resources/META-INF/services/io.reconark.kernel.api.ReconArkPlugin`.
5. Tests:
   - `<Name>PluginTest extends PluginContractTck`;
   - a `@Nested` class extending the point's TCK from `testing/plugin-tck`, if one exists. If none exists, write
     focused tests and note the missing TCK in the PR;
   - add focused tests for edge cases: nulls, limits, masking.
6. Wiring:
   - add `include(":plugins:<plugin-id>")` to `settings.gradle.kts`;
   - add `runtimeOnly(project(":plugins:<plugin-id>"))` to the services that should carry it (ask which);
   - add it to the relevant `config/composition/*.yaml` examples.
7. Documentation: update HLD §7 (shipped plugins column) and, if the plugin has notable options, LLD §3/§4/§5.
8. Run `./gradlew :plugins:<plugin-id>:test :testing:architecture-tests:test` and fix every failure. Kernel,
   domain and plugin code compiles with `-Werror`.
9. Summarise:
   - the files created;
   - the composition entry needed to activate the brick;
   - any follow-up.
