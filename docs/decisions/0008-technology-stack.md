# 0008: Java 25, Spring Boot 4, React 19, Gradle

| Status | Accepted |
|---|---|
| Date | 2026-10-12 |

## Context

The stack must be mainstream, supported for years, and learnable by developers with 2–3 years of experience.

## Decision

Java 25 (LTS) with Spring Boot 4 for all services; React 19 + TypeScript + Vite + TanStack Query for the UI; Gradle (Kotlin DSL) as the single build; JUnit 5, AssertJ, Testcontainers and Vitest for tests; Docker compose locally and one Helm chart for Kubernetes.

## Consequences

- Long support windows; the largest talent pool.
- Spring Boot 4 / Jackson 3 are recent: examples on the internet are often for Boot 3 (package and starter names differ slightly; see the developer guide).

## Alternatives considered

- Java 21 + Spring Boot 3: more online examples; shorter remaining support.
- Maven instead of Gradle: more familiar to many, more verbose.
- Angular or Vue instead of React; Quarkus or Micronaut instead of Spring Boot (see architecture §2.10).
