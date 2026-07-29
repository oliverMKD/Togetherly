# Continuous Integration & Repository Automation (Step 14.5.4–14.5.6)

What runs on every pull request, why it's shaped this way, and what's intentionally out of scope.
For the full operational guide — local equivalents, required-check/branch-protection design, fork
PR security, and a troubleshooting runbook — see [docs/continuous-integration.md](continuous-integration.md).

## 1. Workflows

| Workflow | File | Trigger | Purpose |
|---|---|---|---|
| CI | `.github/workflows/ci.yml` | `pull_request`/`push` to `main`, manual | Android Lint, common+Android unit tests, common metadata compile, Android debug assemble — on `ubuntu-latest`. |
| Apple CI | `.github/workflows/apple-ci.yml` | `pull_request`/`push` to `main`, manual | A cheap `gate` job (`ubuntu-latest`) always runs first and `git diff`s the change against `shared/`, `iosApp/`, Gradle config; the expensive `verify-apple` job (compiles the shared module's Apple targets, links the iOS simulator framework, compiles and runs `iosSimulatorArm64Test`, compiles the iOS host app with no code signing, on a pinned `macos-15-arm64` runner) only runs when that gate says something Apple-relevant changed — see `docs/continuous-integration.md` for why this is a gate job rather than a workflow-level path filter. |
| Dependency Review | `.github/workflows/dependency-review.yml` | `pull_request` to `main` | Diffs the dependency graph and fails the PR on a new high-or-critical severity vulnerability; moderate/low findings surface in the job summary but don't block. |

CodeQL is **not** a workflow file in this repository — it runs via GitHub's managed
["default setup"](https://docs.github.com/en/code-security/code-scanning/automatically-scanning-your-code-for-vulnerabilities-and-errors/configuring-default-setup-for-code-scanning),
enabled in repository settings. See §4.

## 2. Known platform limitation: `iosSimulatorArm64Test`

`apple-ci.yml` runs `:shared:iosSimulatorArm64Test` as its own step with `continue-on-error: true`
and an explicit `::warning::` annotation on failure — it is never silently skipped or removed.

Kotlin/Native's `iosSimulatorArm64Test` runs the compiled test binary as a bare process, not a real
`.app` bundle. `IosReminderSchedulerTest` calls `UNUserNotificationCenter.currentNotificationCenter()`,
which requires a genuine app-bundle identity (`NSBundle.mainBundle`) that a bare `.kexe` test binary
never establishes, so the whole shared test process aborts with `NSInternalInconsistencyException`.
This is a pre-existing platform/test-harness limitation, not something introduced by CI — see
`docs/architecture.md` § Known environment limitations for the full history (including the earlier,
now-resolved Skiko/SDK-version issue this was originally confused with).

## 3. Action version pinning

Every `uses:` reference in every workflow is pinned to a full 40-character commit SHA, with a
`# vX.Y.Z` comment for human readability, e.g.:

```yaml
uses: actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1 # v7.0.1
```

Rationale: a mutable tag (`@v4`, `@main`) can be repointed by the upstream repository (compromised
or not) to different code after review; a commit SHA cannot. Only actions published by `actions`
(GitHub's own organization) or `gradle` (Gradle Inc.'s official organization for `gradle/actions`)
are used — no other third-party actions are referenced anywhere in this repository.

Dependabot (`.github/dependabot.yml`, `github-actions` ecosystem) keeps these SHAs current: it
updates both the pinned SHA and the version comment together when a new release ships, grouping
minor/patch bumps into a single PR. A major version bump is never grouped — it always gets its own
PR so a breaking change is reviewed in isolation.

## 4. CodeQL scope and limitations

GitHub's default-setup analysis for this repository covers `java-kotlin`, `swift`, and `actions`
(confirmed via the repository's own `code-scanning/default-setup` configuration). In practice that
means:

- **Covered**: all JVM-compilable Kotlin (`commonMain`/`androidMain`/`androidApp` — anything that
  participates in an Android/JVM compilation), the `iosApp` Swift shell, and every workflow YAML
  file in `.github/workflows/`.
- **Not covered**: Kotlin/Native-compiled code paths that only exist for `iosArm64`/
  `iosSimulatorArm64` and have no JVM-side equivalent. CodeQL's Kotlin extractor works via the JVM
  compiler front end, so Kotlin/Native-only source has no equivalent database to analyze. This is a
  known upstream CodeQL limitation, not a misconfiguration — most of this project's shared logic is
  `commonMain` and does get analyzed via the Android compilation; only genuinely Apple-target-only
  code (e.g. `iosMain` actuals) falls outside CodeQL's coverage today.

## 5. Secret scanning

Push protection and secret scanning are both enabled for this repository (GitHub's free tier for
public repositories). As a local pre-push check, [gitleaks](https://github.com/gitleaks/gitleaks)
can be run against the working tree or full history:

```bash
gitleaks detect --source . --verbose
```

This is a documented recommendation for contributors who want an extra local check before pushing —
it is not wired into CI, since GitHub's server-side secret scanning and push protection already
cover the repository itself.

## 6. Labels

| Label | Use |
|---|---|
| `bug` | Something isn't working correctly |
| `enhancement` | New feature or improvement request |
| `documentation` | Docs-only changes |
| `android` | Android-specific |
| `ios` | iOS-specific |
| `kmp` | Shared/`commonMain` Kotlin Multiplatform code |
| `design` | UI/UX/design-system |
| `privacy` | Privacy behavior or data handling |
| `revenuecat` | Family Plus / purchase flow |
| `analytics` | PostHog product analytics |
| `good first issue` | Approachable for a new contributor |
| `shipaton` | Related to the Shipaton submission/timeline |
