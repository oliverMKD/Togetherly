# Contributing to Togetherly

Thanks for your interest in Togetherly. This is an early-stage, actively-developed project — issue
and pull request response times may vary.

## Before you start

- For a small fix (typo, obvious bug), a pull request is fine on its own.
- For anything larger (a new feature, a behavior change), please open an issue first to discuss
  the approach before writing code — it can save you a rewrite.

## Local setup

See the [README](README.md#local-development) for requirements and configuration. Every
third-party integration (RevenueCat, PostHog, Sentry) is optional in local development — the app
runs fully without any of them configured.

## Conventions

Please read [docs/architecture.md](docs/architecture.md) before contributing code — it documents
the domain/data/presentation boundaries, dependency injection conventions, and testing conventions
this project follows consistently. In short:

- `domain` never imports from `content` or `data`; `feature` (presentation) never calls a
  repository or use case directly from a `Composable`.
- Every repository interface has a `Fake<Name>Repository` test double in `commonTest` — production
  code must never resolve a fake.
- New code should include tests. See [docs/architecture.md](docs/architecture.md#testing-conventions)
  for where different kinds of tests live.

## Building and testing

```
./gradlew :androidApp:assembleDebug     # Android debug build
./gradlew :shared:testAndroidHostTest   # common + Android unit tests
./gradlew :shared:iosSimulatorArm64Test # iOS unit tests
./gradlew allTests                      # every target
```

## Privacy and safety

Togetherly is a family app with no accounts and no cloud sync — please keep any test
fixtures/sample data you add clearly fictional (no real names, photos, voice recordings, or
personal identifiers). See [docs/privacy.md](docs/privacy.md) and
[docs/private-media.md](docs/private-media.md) for the app's own privacy design.

## Submitting a pull request

- Keep pull requests focused on one change.
- Make sure `./gradlew allTests` passes locally.
- Describe what changed and why in the PR description.

## Reporting a bug

Open a GitHub issue with steps to reproduce. For a security vulnerability, please follow
[SECURITY.md](SECURITY.md) instead of opening a public issue.

## Code of Conduct

This project follows the [Code of Conduct](CODE_OF_CONDUCT.md) — please read it before
participating.
