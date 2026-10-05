# ADR-0026: Hybrid plugin kernel: in-process SPI bricks plus out-of-process gRPC bricks

| | |
|---|---|
| Status | Proposed — v0.2 composable architecture, pending architecture review |
| Date | 2026-10-05 |
| Deciders | Architecture review board (to confirm) |
| Details | [HLD](../hld/reconArk-HLD.md) · [LLD](../lld/reconArk-LLD.md) · [Solution document](../solution/reconArk-solution-document.md) |
| Relates to | ADR-0001 (extended, not superseded) |

## Context
C1–C3, C7 (HLD §2): every capability must be addable, removable and replaceable by configuration; third-party and non-JVM extensions must be possible without weakening security. v0.1 ADR-0001 gave ports and adapters, but selection happened at build time and there was no lifecycle, validation, versioning or isolation model.

## Decision
A framework-free plugin kernel (`kernel/plugin-api`, `kernel/plugin-runtime`). Plugins implement `ReconArkPlugin`, are discovered by `ServiceLoader`, declare a `PluginDescriptor` (id, version, kernel API range, trust tier, provides, requires, config spec) and contribute implementations to typed `ExtensionPoint`s. The composition (`reconark.composition.*`) decides which installed plugins are active. Untrusted or non-JVM plugins run out-of-process behind `contracts/proto/extension_service.proto`; the kernel wraps them in a proxy implementing the same SPI.

## Consequences
Adding, removing or swapping a brick is a config change plus redeploy; engines never see implementations. Boot-time validation (RK-KRN-*) makes bad compositions fail before traffic. One extra indirection per call (interceptor proxy) — negligible in-process; remote calls are batched.

## Alternatives considered and rejected
Spring-only `@ConditionalOnProperty` wiring (ties plugins to Spring, no lifecycle or validation, no non-JVM story); PF4J runtime JAR loading (class-loader isolation complexity, hot-load not needed with immutable images); OSGi (high learning curve, poor fit with Spring Boot 4).

## Revisit when
The assumptions above change, or measurements from the performance, isolation or security suites contradict
them. A change is a new ADR that supersedes this one; accepted records are never edited in place.
