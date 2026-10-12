---
description: Create a new reconArk plugin (file format, comparator, report writer or storage) with tests, wired into the services that use it
argument-hint: <kind: format|comparator|report|storage> <name, e.g. format-jsonl>
---

Create a plugin. Arguments: $ARGUMENTS. Follow `docs/04-developer-guide.md` §4.5 and copy the closest existing plugin
in `code/backend/plugins/` (format → `format-csv`, comparator → `compare-standard`, report → `report-csv`,
storage → `storage-local`).

1. Interface by kind: format → `FileFormat`, comparator → `FieldComparator`, report → `ReportWriter`,
   storage → `FileStore` (all in `code/backend/common/.../plugin`). If none fits, stop: a new plugin kind needs a
   decision record (`/new-adr`) first.
2. Module `code/backend/plugins/<name>/build.gradle.kts`: `description`, `api(project(":backend:common"))`,
   `implementation("org.springframework.boot:spring-boot-autoconfigure")`. Third-party libraries: ask first; versions
   come from the Spring Boot BOM where possible. Add `include(":backend:plugins:<name>")` to `code/settings.gradle.kts`.
3. Package `io.reconark.plugins.<shortname>`:
   - the implementation class with a short, unique, lower-case `id()` and a one-sentence `description()` that lists
     its options;
   - `<Name>Plugin` with `@AutoConfiguration` and
     `@ConditionalOnProperty(name = "reconark.plugins.<name>.enabled", havingValue = "true", matchIfMissing = true)`;
   - `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
   Formats and writers must stream (never load a whole file into memory). Error messages name lines/fields, never values.
4. Unit tests: normal cases, every option, edge cases (empty, null, malformed input → clear message).
5. Wire it into the services' `build.gradle.kts`:
   format/comparator → `worker` and `config-service`; report → `report-service`; storage → `ingestion-service` and
   `worker` (and tell the user to switch `storage-local` off where the new storage replaces it).
6. Docs: add the plugin and its options to `docs/03-detailed-design.md` §3.2 (plugin options table).
7. Run `cd code && ./gradlew :backend:plugins:<name>:test` and the tests of the services you changed. Summarise
   what you created and how to use it in a definition.
