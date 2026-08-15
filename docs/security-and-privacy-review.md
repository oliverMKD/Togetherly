# Togetherly security and privacy review

Scope for this pass:

- local configuration
- git history
- GitHub Actions
- RevenueCat
- PostHog
- Sentry
- private media
- local database
- notifications
- external links
- logs
- debug tooling
- release builds

## Summary

I updated the backup and file-protection policy in this step. The current repository now has the
major security/privacy gates in place:

- Android and iOS keys come from local, untracked configuration.
- PostHog autocapture and session replay are disabled.
- Diagnostics and analytics are consent-gated.
- Debug telemetry is hidden behind debug-only UI state and does not have a release entry point.
- Android release builds disable backup entirely and still carry explicit exclusion rules as
  defense in depth.
- iOS applies file protection to the database/settings tree and private-media tree, and excludes
  the app support directory from backup.
- Reminder notifications use generic copy and stable identifiers.
- Release builds compiled successfully on both Android and iOS.
- The first-release backup policy now keeps Togetherly user-generated data out of Android cloud
  backup and device-to-device transfer, and out of iOS platform backup where the app can configure
  it.

## Checks run

### Secret scan

The environment does not have a local `gitleaks` binary installed, and the repo does not include a dedicated secret-scanner task. I therefore ran a repository-wide pattern sweep for common secret formats:

```bash
rg -n "BEGIN PRIVATE KEY|BEGIN RSA PRIVATE KEY|ghp_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{20,}|sk_live_[A-Za-z0-9]{20,}|rk_live_[A-Za-z0-9]{20,}|xox[baprs]-[A-Za-z0-9-]{10,}|AIza[0-9A-Za-z_-]{20,}|test_[A-Za-z0-9]{16,}|secret[_-]?key|auth[_-]?token|client[_-]?secret" . --glob '!**/build/**' --glob '!**/.git/**'
```

Result:

- one hit in `iosApp/Configuration/RevenueCat.local.xcconfig.example:9`
- the match is an example/test value in a tracked `.example` file, not a real secret

### Dependency/security checks

GitHub dependency review is configured in `.github/workflows/dependency-review.yml`. There is no separate local dependency-security scanner in the repo.

### Build and verification commands

```bash
GRADLE_USER_HOME=/private/tmp/codex-gradle ./gradlew :shared:testAndroidHostTest :shared:compileCommonMainKotlinMetadata :shared:compileKotlinIosSimulatorArm64 :androidApp:lintDebug :androidApp:assembleDebug :androidApp:assembleRelease --stacktrace --no-daemon
```

Result:

- passed
- notable output was limited to existing Kotlin warnings:
  - expect/actual beta warning
  - non-public data-class copy visibility warning
  - deprecated `BackHandler` warning

```bash
xcodebuild build -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' -configuration Release CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO CODE_SIGN_IDENTITY=""
```

Result:

- passed
- notable output was limited to existing toolchain warnings:
  - bundle ID inference warning for the Kotlin framework
  - prebuilt RevenueCat/RevenueCatUI module-cache warnings

### Git history review

I reviewed recent repository history for the areas in scope:

```bash
git log --oneline --decorate -n 20 -- .github androidApp shared iosApp docs
```

No suspicious secret-bearing commits were evident in that reviewed range.

## Categorized findings

### Fixed

- Android release backup posture is disabled in the manifest.
- Android backup/device-transfer XML now excludes the app's storage domains explicitly and names
  the Room database files individually.
- iOS storage now applies explicit file protection to the database/settings tree and private-media
  tree, with backup exclusion on the Application Support directory.

### Accepted risk

- The repository includes debug telemetry code and screens in shared sources, but release builds do not surface the entry point and the runtime binding is debug-gated.
- The iOS build emits prebuilt-module-cache warnings from RevenueCat and Sentry artifacts; they are toolchain noise, not evidence of runtime data exposure.
- Platform-managed backups and device snapshots can still exist outside Togetherly's control; the app's policy is to exclude its own user-generated data from its backup configuration, not to claim those platform systems are impossible.

### External configuration

- `local.properties` is ignored and untracked; it provides Android RevenueCat/PostHog/Sentry values locally.
- `iosApp/Configuration/Config.xcconfig` includes gitignored local overlays for RevenueCat/PostHog/Sentry.
- Release signing, if/when added, remains external to the repository.
- Legal/support URLs are externally configurable and should stay outside source-controlled secrets.

### Requires legal review

- Privacy-policy wording around backup, uninstall behavior, analytics consent, diagnostics consent, and deletion claims should be reviewed before any store release.
- Any public-facing statement about what device backup or uninstall does to local data should remain aligned with legal copy, not inferred from implementation.

## Ordered hardening backlog

1. Reconfirm the privacy-policy/legal copy against the current backup, telemetry, and deletion behavior before release.
2. Add an in-repo local secret scanner if the team wants an offline pre-push check in addition to GitHub secret scanning.
3. Revisit iOS file-protection policy if the project wants a stricter stance than the current directory-level protection scheme.
