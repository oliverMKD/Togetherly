# Continuous Integration — full guide (Step 14.5.7)

The complete operational reference for Togetherly's CI: what runs, why it's shaped this way, how
to reproduce any check locally, and how to unblock a stuck pull request. For the short reference
table and repository label list, see [docs/ci.md](ci.md).

## 1. Workflow triggers

| Workflow | Trigger | Notes |
|---|---|---|
| `ci.yml` | `pull_request`/`push` to `main`, `workflow_dispatch` | No path filter — always runs. |
| `apple-ci.yml` | `pull_request`/`push` to `main`, `workflow_dispatch` | No path filter either, but see §2 — the expensive job is gated internally. |
| `dependency-review.yml` | `pull_request` to `main` | No path filter. |
| CodeQL (default setup) | GitHub-managed, not a workflow file | Runs on `push` to `main` and on `pull_request`, per GitHub's own default-setup schedule. |

Every workflow also has `concurrency` set to `<workflow>-${{ github.ref }}` with
`cancel-in-progress: true` — pushing again to the same branch (including a PR's head branch)
cancels that branch's in-flight run rather than queuing a second one.

## 2. Linux verification (`ci.yml`)

Runs unconditionally on every PR and push to `main`, on `ubuntu-latest`:

1. Check out, set up JDK (Zulu 21 + 17 — see §5), validate the Gradle wrapper.
2. `./gradlew :androidApp:lintDebug` — the only static-analysis tool configured (no ktlint/detekt/Spotless in this project).
3. `./gradlew :shared:testAndroidHostTest` — common + Android unit tests, including catalogue validation (`QuestCatalogueValidator` against the real bundled catalogue).
4. `./gradlew :shared:compileCommonMainKotlinMetadata` — a fast correctness check for `commonMain` independent of any single platform target.
5. `./gradlew :androidApp:assembleDebug` — the actual Android debug build.
6. On failure, uploads the unit test report and (always) the Lint HTML report as build artifacts.

## 3. Apple verification (`apple-ci.yml`)

Two jobs:

- **`gate`** (`ubuntu-latest`, ~5–10s): checks out with full history, diffs the PR's base...head
  range (or the push's before...after range) against `shared/`, `iosApp/`, `gradle/`,
  `gradle.properties`, `settings.gradle.kts`, `build.gradle.kts`, and `apple-ci.yml` itself. Sets
  an `apple` output (`true`/`false`).
- **`verify-apple`** (`macos-15-arm64`, pinned Xcode 16.4): `needs: gate`, `if: needs.gate.outputs.apple == 'true'`.
  Compiles `iosArm64`/`iosSimulatorArm64`, links the simulator framework, compiles (and runs, with
  `continue-on-error: true` — see §10) the iOS simulator test binary, and compiles the iOS host app
  with `CODE_SIGNING_ALLOWED=NO`.

**Why a gate job instead of a workflow-level `paths:` filter**: a path-filtered workflow trigger
means the workflow never runs at all for a change outside those paths — no check run is ever
created, so a required status check pointed at that workflow's job name gets stuck "Expected —
waiting for status to be reported" forever on, say, a documentation-only PR. Moving the path check
inside an always-running job instead means the workflow always triggers and `verify-apple` always
reports a real conclusion: either it ran, or GitHub records it as **skipped** — and a skipped
required check satisfies GitHub's required-status-check requirement, so the PR is never stuck. This
was verified live: PR #4 (a `docs/`-only change) showed `Verify (macOS)` as `skipping`, and the
PR's `mergeStateStatus` still went `BLOCKED` → `CLEAN` once it reported.

## 4. Required checks

The `main` branch ruleset requires two status checks:

- `Verify (Linux)` (`ci.yml`) — always runs.
- `Verify (macOS)` (`apple-ci.yml`) — runs or is skipped, per §3, but always reports.

Both must be **up to date with `main`** (`strict_required_status_checks_policy: true`) before
merging — GitHub's "Update branch" button (or `gh pr merge --rebase`/a manual merge-from-main) is
enough to satisfy this after `main` moves.

The ruleset also: requires a pull request (0 required approvals — solo project), blocks force
pushes and branch deletion on `main`, requires conversation resolution, restricts merges to squash
only, and lets the repository owner bypass *pull-request-time* requirements only (not force-push or
deletion protection) for a genuine emergency.

## 5. Local equivalents

Every CI check has a direct local command:

```bash
./gradlew :androidApp:lintDebug                          # Android Lint
./gradlew :shared:testAndroidHostTest                     # common + Android unit tests
./gradlew :shared:compileCommonMainKotlinMetadata          # commonMain compile check
./gradlew :androidApp:assembleDebug                        # Android debug build
./gradlew :shared:compileKotlinIosSimulatorArm64 :shared:compileKotlinIosArm64   # Apple compile
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64       # iOS simulator framework link
./gradlew :shared:iosSimulatorArm64Test                     # iOS unit tests (see §10)
./gradlew allTests                                          # everything, every target
```

The Apple commands require Xcode and a macOS host; everything else runs on any platform Gradle
supports. `gradle-daemon-jvm.properties` pins the Gradle *daemon* to Zulu 21 — install that
alongside whatever JDK 17 your IDE uses for the actual `jvmToolchain(17)` compilation, exactly as
CI does (see `ci.yml`'s "Set up JDK" step).

## 6. Configuration strategy

No CI job references a repository secret of any kind. `local.properties` never exists in a CI
checkout, so the RevenueCat/PostHog/Sentry keys all resolve to their documented blank/no-op
defaults (free mode, analytics disabled, diagnostics disabled — see
[docs/configuration.md](configuration.md)) and every build/test/lint step succeeds without any
production credential. This is deliberate: it means a fork can run this entire pipeline unmodified,
and no `secrets.*` context ever needs to exist in these workflow files at all.

## 7. Fork pull-request security

`pull_request` (never `pull_request_target`) is used everywhere. GitHub's own built-in behavior for
`pull_request` already runs fork PRs with a read-only `GITHUB_TOKEN` and no access to repository
secrets — a meaningful protection on its own, but redundant here regardless, since (per §6) nothing
in this repository's workflows references a secret to begin with. There is nothing a malicious fork
PR could exfiltrate even if that built-in protection didn't exist. No workflow interpolates
`github.event.*` free-text fields (PR titles, bodies, branch names) directly into a `run:` shell
block — the only `github.event.*` values used (in `apple-ci.yml`'s `gate` job) are commit SHAs and
the event name, which are not attacker-controlled.

## 8. Artifact retention

`ci.yml` uploads two artifacts, both 5-day retention:

- `unit-test-report` (on failure only) — `shared/build/reports/tests`, `shared/build/test-results`.
- `android-lint-report` (always) — `androidApp/build/reports/lint-results-debug.html`.

Both are build/test output only — HTML reports and XML/JSON test results generated from this
repository's own source. Neither can contain a credential, since no credential exists anywhere in
the build environment to begin with (§6).

## 9. Dependabot behavior

Weekly, two ecosystems (`gradle`, `github-actions`), 5 open-PR limit each. Minor/patch bumps are
grouped (`kotlin-and-compose`, `androidx`, `actions-minor-patch`); a major version bump always gets
its own ungrouped PR, so a breaking change is never silently bundled with something unrelated. Every
Dependabot PR goes through the same required checks as a human-authored PR — there is no
auto-merge configured, so every dependency bump is a normal reviewed, CI-gated pull request.

## 10. Troubleshooting

**A PR is stuck "Expected — waiting for status to be reported" on `Verify (macOS)`.**
Should not happen after the §3 gate restructure — check that `apple-ci.yml`'s `gate` job actually
ran (if the whole workflow failed to trigger, that's a different, deeper problem: check the
Actions tab for the workflow run list, not just the PR's checks tab).

**`Verify (macOS)` is stuck `queued` for a long time.**
A known, pre-existing limitation of this repository's free-tier GitHub account: macOS runners are
billed at a much higher Actions-minute multiplier than Linux and are sometimes unavailable/rate
limited without a payment method on file. This is a runner-availability constraint, not a workflow
defect — see the Step 14.5.5/14.5.7 history. It resolves on its own; there is nothing to fix in the
workflow itself.

**iOS simulator tests fail inside `apple-ci.yml`.**
Check whether it's the known `IosReminderSchedulerTest` / `UNUserNotificationCenter` crash (see
`docs/architecture.md`'s "Known environment limitations") — that step runs with
`continue-on-error: true` specifically because of this pre-existing platform limitation, and does
not block the PR. Any *other* iOS test failure is a real regression and should be treated as one.

**A required check never appears at all.**
Confirm the job's `name:` in the workflow YAML still matches the name registered in the ruleset's
`required_status_checks` (`gh api repos/oliverMKD/Togetherly/rulesets/19988539`) — renaming a job
without updating the ruleset breaks the match silently.

## 11. Updating action versions

Every `uses:` reference is pinned to a full 40-character commit SHA with a `# vX.Y.Z` comment (see
[docs/ci.md](ci.md) §3 for the full policy and rationale). Dependabot's `github-actions` ecosystem
config keeps these current automatically, updating both the SHA and the comment together in a
grouped minor/patch PR (major bumps get their own PR). To update one manually instead:

```bash
gh api repos/<org>/<action>/releases/latest --jq '.tag_name'
gh api repos/<org>/<action>/git/refs/tags/<tag> --jq '.object.sha'
```

Then replace both the SHA and the version comment together — never update one without the other.
