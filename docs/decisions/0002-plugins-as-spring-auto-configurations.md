# 0002: Plugins are Spring Boot auto-configurations behind small interfaces

| Status | Accepted |
|---|---|
| Date | 2026-10-12 |

## Context

Formats, comparison rules, report formats and storage must be addable, replaceable and removable without changing engines or services.

## Decision

Each plugin kind is a Java interface in `backend/common` (`FileFormat`, `FieldComparator`, `ReportWriter`, `FileStore`). Each plugin is a Gradle module with one `@AutoConfiguration` class, switched off with `reconark.plugins.<name>.enabled=false`. Services collect the active plugins into a `PluginRegistry` and look them up by the id named in the definition.

## Consequences

- Uses Spring Boot's own mechanism: nothing new to learn, works in tests.
- A plugin is chosen at build time (module present) and at run time (enabled + named in a definition).
- Plugins must be Java; non-JVM plugins would need a remote-call plugin later.

## Alternatives considered

- Custom plugin kernel (v0.2): more control (versions, trust tiers) but much more code and concepts.
- Java ServiceLoader only: no on/off by configuration without extra code.
- OSGi / PF4J: hot loading, but heavy.
