# Repository Guidelines

## Project Structure & Module Organization

This is a Java 21 Spring Boot wallet service. Production code lives in `src/main/java/ir/rahgozin/wallet`, with business logic and domain types under `application` and HTTP, Kafka, and configuration adapters under `infrastructure`. Runtime settings are in `src/main/resources/application.yml`. Tests mirror the production package structure under `src/test/java`; currently they cover the wallet service and REST controller. `target/` contains generated build output and should not be edited by hand.

## Build, Test, and Development Commands

Use the Maven wrapper from the repository root:

- `./mvnw test` (Windows: `.\mvnw.cmd test`) compiles and runs the test suite.
- `./mvnw clean package` (Windows: `.\mvnw.cmd clean package`) creates a clean packaged application.
- `./mvnw spring-boot:run` (Windows: `.\mvnw.cmd spring-boot:run`) starts the service locally on port 8080 with the `/wallet/` context path.

Use a Java 21 JDK. Local configuration currently uses an in-memory H2 database and expects Redis and Kafka settings as declared in `application.yml`.

## Coding Style & Naming Conventions

Follow the existing Java style: four spaces for indentation, lowercase package names, `UpperCamelCase` classes, and `lowerCamelCase` methods and fields. Keep responsibilities aligned with the existing `application` and `infrastructure` packages. There is no formatter or linter configured in `pom.xml`; keep imports explicit and match nearby formatting.

## Testing Guidelines

Write JUnit 5 tests with the `*Test.java` suffix in the matching package under `src/test/java`. Existing tests use Spring test support and Mockito. Name test methods descriptively, often using `given..._when..._then...`. Run `./mvnw test` before submitting changes; no coverage threshold is configured.

## Commit & Pull Request Guidelines

Git history is not present in this checkout, so an established commit message convention could not be verified. Use a short, imperative commit subject (for example, `Validate wallet debit requests`). Pull requests should explain the behavior change, link a related issue when available, and report relevant test results. For API changes, include example requests or responses when they clarify the impact.

