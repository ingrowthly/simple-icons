# Repository Guidelines

## Project Structure & Module Organization

This repository contains a Kotlin-based IntelliJ Platform plugin. Runtime code lives in `src/main/kotlin/io/github/ingrowthly/icons/`; plugin metadata, messages, and SVG assets live under `src/main/resources/`. Icon matching rules are maintained in `icons/files.json` and `icons/folders.json`. `buildSrc/` contains the custom Gradle task that compiles those rules into Kotlin and binary resources under `build/generated/`; generated output should not be edited or committed. Unit tests mirror the main package under `src/test/kotlin/`. CI and release automation are in `.github/workflows/`, while shared IDE run configurations are in `.run/`.

## Build, Test, and Development Commands

Use the checked-in Gradle wrapper and JDK 21:

- `./gradlew check` compiles the project, runs JUnit tests, and produces the Kover XML coverage report.
- `./gradlew test` runs the unit test suite only.
- `./gradlew runIde` starts a sandboxed IntelliJ IDEA instance with the plugin installed.
- `./gradlew buildPlugin` creates the distributable plugin ZIP in `build/distributions/`.
- `./gradlew verifyPlugin` performs plugin structure and compatibility verification.
- `./gradlew generateIconKotlin` regenerates icon lookup sources and data from the JSON rule files.

## Coding Style & Naming Conventions

Follow standard Kotlin style: four-space indentation, trailing commas in multiline declarations, and concise expression bodies where they remain readable. Use `PascalCase` for classes and objects, `camelCase` for functions and properties, and descriptive test names such as `exactNameBeatsExtensionAtSamePriority`. Keep packages under `io.github.ingrowthly.icons`. Qodana performs CI code inspection; use IntelliJ's Kotlin formatter before committing. Preserve existing icon filename casing, and ensure JSON rules exactly reference an SVG in the corresponding `src/main/resources/icons/files/` or `folders/` directory.

## Testing Guidelines

Tests use JUnit 4 (`org.junit.Test`) and belong in files named `*Test.kt`. Add focused resolver tests whenever changing rule precedence, glob handling, path normalization, or dictionary generation. Run `./gradlew check` before opening a pull request. CI also runs Qodana and `verifyPlugin`; there is no documented minimum coverage threshold, but new behavior should be exercised directly.

## Commit & Pull Request Guidelines

Recent commits use concise Conventional Commit prefixes, primarily `feat:`, `chore:`, and `chore(deps):`; use an imperative summary scoped to one change. Pull requests should explain the behavior changed, list validation performed, and link relevant issues. Include before/after screenshots for visible icon changes, update `CHANGELOG.md` for user-facing changes, and keep generated and local IDE artifacts out of the diff.
